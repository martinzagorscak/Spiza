package com.mzagorscak.spiza.data.household

import com.mzagorscak.spiza.crash.DataException
import com.mzagorscak.spiza.data.household.model.HouseholdDto
import com.mzagorscak.spiza.domain.constraints.ConstraintsManagerImpl
import com.mzagorscak.spiza.domain.model.UserProfile
import com.mzagorscak.spiza.fakes.FakeAuthService
import com.mzagorscak.spiza.fakes.FakeCrashLogger
import com.mzagorscak.spiza.fakes.FakeHouseholdDataSource
import com.mzagorscak.spiza.fakes.FakeUserRepository
import com.mzagorscak.spiza.fakes.TEST_PROFILE
import com.mzagorscak.spiza.fakes.TEST_USER
import com.mzagorscak.spiza.fakes.householdDto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val MAX_MEMBERS = 10
private const val MAX_ITEMS = 100
private const val MAX_HOUSEHOLDS = 3

// The first subscription plus the repository's three retries.
private const val SUBSCRIPTIONS_WITH_RETRIES = 4

class HouseholdRepositoryImplTest {
    private val dataSource = FakeHouseholdDataSource()
    private val authService = FakeAuthService()
    private val userRepository = FakeUserRepository()
    private val logger = FakeCrashLogger()

    private val repository =
        HouseholdRepositoryImpl(
            dataSource = dataSource,
            authService = authService,
            userRepository = userRepository,
            constraintsManager = ConstraintsManagerImpl,
            crashLogger = logger,
        )

    // The repository caches households while they are observed, which is what the constraint checks read.
    private suspend fun observeHouseholds(vararg households: HouseholdDto) {
        dataSource.households.value = households.toList()
        repository.observeHouseholds().first()
    }

    @Test
    fun addMember_returnsFalseWithoutLookup_whenJoinKeyCanNotBeAKey() =
        runTest {
            listOf("", "   ", "a/b", "../x", "key with spaces").forEach { key ->
                assertFalse(repository.addMemberToHousehold(householdId = "household-1", userJoinKey = key), "'$key' must be rejected")
            }
            assertTrue(dataSource.addedMembers.isEmpty())
        }

    @Test
    fun addMember_normalizesTheTypedKey_beforeLookingItUp() =
        runTest {
            val isAdded = repository.addMemberToHousehold(householdId = "household-1", userJoinKey = "  ABC-123 ")

            assertTrue(isAdded)
            assertEquals(listOf("household-1" to "abc-123"), dataSource.addedMembers)
        }

    @Test
    fun addMember_returnsFalseAndRecordsError_whenUserIsNotSignedIn() =
        runTest {
            authService.user.value = null

            assertFalse(repository.addMemberToHousehold(householdId = "household-1", userJoinKey = "abc-123"))
            assertTrue(dataSource.addedMembers.isEmpty())
            assertEquals(1, logger.exceptions.size)
        }

    @Test
    fun addMember_returnsFalse_whenHouseholdIsFull() =
        runTest {
            observeHouseholds(householdDto(members = List(MAX_MEMBERS) { "user-$it" }))

            assertFalse(repository.addMemberToHousehold(householdId = "household-1", userJoinKey = "abc-123"))
            assertTrue(dataSource.addedMembers.isEmpty())
        }

    @Test
    fun addItem_doesNotWrite_whenHouseholdReachedTheItemLimit() =
        runTest {
            repository.addItem(householdId = "household-1", text = "milk", position = 1.0, currentItemsCount = MAX_ITEMS)

            assertTrue(dataSource.addedItems.isEmpty())
            assertEquals(1, logger.warnings.size)
        }

    @Test
    fun createHousehold_returnsNullWithoutWriting_whenUserReachedTheHouseholdLimit() =
        runTest {
            observeHouseholds(*Array(MAX_HOUSEHOLDS) { householdDto(id = "household-$it") })

            assertNull(repository.createHousehold(name = "Cottage", description = ""))
            assertTrue(dataSource.createdHouseholds.isEmpty())
        }

    @Test
    fun createHousehold_deletesOwnJoinKey_whenTheNewHouseholdReachesTheLimit() =
        runTest {
            observeHouseholds(*Array(MAX_HOUSEHOLDS - 1) { householdDto(id = "household-$it") })

            assertEquals("household-new", repository.createHousehold(name = "Cottage", description = ""))
            assertEquals(listOf(TEST_USER.uid), dataSource.deletedJoinKeysOfUsers)
        }

    @Test
    fun updateHousehold_completesWithoutError_whenServerNeverAcknowledgesTheWrite() =
        runTest {
            dataSource.neverAcknowledgeWrites = true

            repository.updateHousehold(householdId = "household-1", name = "Home", description = "")

            assertTrue(logger.exceptions.isEmpty())
            assertEquals(1, logger.warnings.size)
        }

    @Test
    fun observeHouseholds_resolvesMemberProfiles() =
        runTest {
            val other = UserProfile(id = "user-2", name = "Ivo", email = null, photoUrl = null)
            userRepository.users = listOf(TEST_PROFILE, other)
            dataSource.households.value = listOf(householdDto(members = listOf(TEST_USER.uid, "user-2")))

            val household = repository.observeHouseholds().first().single()

            assertEquals(listOf(TEST_PROFILE, other), household.members)
        }

    @Test
    fun observeHouseholds_stillEmitsHouseholds_whenMemberProfilesCanNotBeLoaded() =
        runTest {
            userRepository.getUsersFailure = DataException(DataException.Reason.Offline, "offline")
            dataSource.households.value = listOf(householdDto())

            val households = repository.observeHouseholds().first()

            assertEquals(listOf("household-1"), households.map { it.id })
            assertTrue(logger.exceptions.isEmpty())
        }

    @Test
    fun getHouseholdJoinKey_emitsNoKey_whenFetchingTheKeyFails() =
        runTest {
            dataSource.joinKey = flow { throw DataException(DataException.Reason.Offline, "offline") }

            assertNull(repository.getHouseholdJoinKey().first())
            assertTrue(logger.exceptions.isEmpty())
        }

    @Test
    fun observeHouseholds_failsWithoutRetrying_whenQuotaIsExceeded() =
        runTest {
            dataSource.householdsFailure = DataException(DataException.Reason.QuotaExceeded, "quota")

            val error = assertFailsWith<DataException> { repository.observeHouseholds().first() }

            assertEquals(DataException.Reason.QuotaExceeded, error.reason)
            assertEquals(1, dataSource.householdsSubscriptions)
            assertEquals(1, logger.warnings.size)
        }

    @Test
    fun observeHouseholds_retriesBeforeFailing_whenListenerFailsForAnotherReason() =
        runTest {
            dataSource.householdsFailure = DataException(DataException.Reason.PermissionDenied, "denied")

            val error = assertFailsWith<DataException> { repository.observeHouseholds().first() }

            assertEquals(DataException.Reason.PermissionDenied, error.reason)
            assertEquals(SUBSCRIPTIONS_WITH_RETRIES, dataSource.householdsSubscriptions)
        }

    @Test
    fun observeItems_failsWithoutRetrying_whenQuotaIsExceeded() =
        runTest {
            dataSource.itemsFailure = DataException(DataException.Reason.QuotaExceeded, "quota")

            val error = assertFailsWith<DataException> { repository.observeItems(householdId = "household-1").first() }

            assertEquals(DataException.Reason.QuotaExceeded, error.reason)
            assertEquals(1, dataSource.itemsSubscriptions)
            assertEquals(1, logger.warnings.size)
        }

    @Test
    fun observeItems_failsWithoutRetrying_whenQuotaExceededIsTheCause() =
        runTest {
            dataSource.itemsFailure = RuntimeException("listener", DataException(DataException.Reason.QuotaExceeded, "quota"))

            assertFailsWith<RuntimeException> { repository.observeItems(householdId = "household-1").first() }

            assertEquals(1, dataSource.itemsSubscriptions)
        }

    @Test
    fun observeItems_retriesBeforeFailing_whenListenerFailsForAnotherReason() =
        runTest {
            dataSource.itemsFailure = DataException(DataException.Reason.PermissionDenied, "denied")

            val error = assertFailsWith<DataException> { repository.observeItems(householdId = "household-1").first() }

            assertEquals(DataException.Reason.PermissionDenied, error.reason)
            assertEquals(SUBSCRIPTIONS_WITH_RETRIES, dataSource.itemsSubscriptions)
        }
}
