package com.mzagorscak.spiza.data.household

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.prop
import com.mzagorscak.spiza.crash.DataException
import com.mzagorscak.spiza.data.household.model.ArticleItemDto
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
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

// The first subscription plus the repository's three retries.
private const val SUBSCRIPTIONS_WITH_RETRIES = 4

// The first count a rule rejects, so the tests follow the limits instead of copying them.
private fun limitOf(isAllowed: (Int) -> Boolean): Int = generateSequence(0) { it + 1 }.first { isAllowed(it).not() }

class HouseholdRepositoryImplTest {
    private val dataSource = FakeHouseholdDataSource()
    private val authService = FakeAuthService()
    private val userRepository = FakeUserRepository()
    private val logger = FakeCrashLogger()

    private val maxHouseholds = limitOf(ConstraintsManagerImpl::canUserCreateOrJoinHousehold)
    private val maxItems = limitOf(ConstraintsManagerImpl::canAddItemToHousehold)
    private val maxMembers = limitOf(ConstraintsManagerImpl::canAddMemberToHousehold)

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
    fun createHouseholdJoinKey_createsKeyOfCurrentUser_andEmitsItToObservers() =
        runTest {
            repository.getHouseholdJoinKey().test {
                assertThat(awaitItem()).isNull()

                val key = repository.createHouseholdJoinKey()

                assertThat(awaitItem()).isEqualTo(key)
                assertThat(dataSource.joinKeys.toList()).containsExactly(key to TEST_USER.uid)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun getHouseholdJoinKey_emitsStoredKeyOfSignedInUser_andNullAfterSignOut() =
        runTest {
            dataSource.joinKeys["other-key"] = "user-2"
            dataSource.joinKeys["stored-key"] = TEST_USER.uid

            repository.getHouseholdJoinKey().test {
                assertThat(awaitItem()).isEqualTo("stored-key")

                authService.user.value = null

                assertThat(awaitItem()).isNull()
                cancelAndIgnoreRemainingEvents()
            }
            assertThat(dataSource.joinKeyRequestsOfUsers).containsExactly(TEST_USER.uid)
        }

    @Test
    fun refreshHouseholdJoinKey_emitsTheStoredKeyAgain() =
        runTest {
            dataSource.joinKeys["old-key"] = TEST_USER.uid

            repository.getHouseholdJoinKey().test {
                assertThat(awaitItem()).isEqualTo("old-key")
                // The key was replaced elsewhere (e.g. on another device); the one-shot read only sees it when read again.
                dataSource.joinKeys.clear()
                dataSource.joinKeys["new-key"] = TEST_USER.uid

                repository.refreshHouseholdJoinKey()

                assertThat(awaitItem()).isEqualTo("new-key")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun addMember_normalizesTheTypedKey_beforeLookingItUp() =
        runTest {
            dataSource.households.value = listOf(householdDto())
            dataSource.joinKeys["abc-123"] = "user-2"

            val isAdded = repository.addMemberToHousehold(householdId = "household-1", userJoinKey = "  ABC-123 ")

            assertThat(isAdded).isTrue()
            assertThat(dataSource.addedMembers).containsExactly("household-1" to "abc-123")
        }

    @Test
    fun observeHouseholds_resolvesMemberProfiles() =
        runTest {
            val other = UserProfile(id = "user-2", name = "Ivo", email = null, photoUrl = null)
            userRepository.users = listOf(TEST_PROFILE, other)
            dataSource.households.value = listOf(householdDto(members = listOf(TEST_USER.uid, "user-2")))

            repository.observeHouseholds().test {
                assertThat(awaitItem().single().members).containsExactly(TEST_PROFILE, other)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun observeHouseholds_sortsHouseholdsByNameIgnoringCase() =
        runTest {
            dataSource.households.value =
                listOf(
                    householdDto(id = "c", name = "cottage"),
                    householdDto(id = "b", name = "Beach"),
                    householdDto(id = "a", name = "apartment"),
                )

            repository.observeHouseholds().test {
                assertThat(awaitItem().map { it.id }).containsExactly("a", "b", "c")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun observeHouseholds_fetchesOnlyMembersThatAreNotLoadedYet() =
        runTest {
            val other = UserProfile(id = "user-2", name = "Ivo", email = null, photoUrl = null)
            userRepository.users = listOf(TEST_PROFILE, other)
            dataSource.households.value = listOf(householdDto())

            repository.observeHouseholds().test {
                awaitItem()

                dataSource.households.value = listOf(householdDto(members = listOf(TEST_USER.uid, "user-2")))

                assertThat(awaitItem().single().members).containsExactly(TEST_PROFILE, other)
                cancelAndIgnoreRemainingEvents()
            }
            assertThat(userRepository.requestedUserIds).containsExactly(listOf(TEST_USER.uid), listOf("user-2"))
        }

    @Test
    fun observeHouseholds_emitsNoHouseholdsWithoutListening_whenSignedOut() =
        runTest {
            authService.user.value = null
            dataSource.households.value = listOf(householdDto())

            repository.observeHouseholds().test {
                assertThat(awaitItem()).isEmpty()
                cancelAndIgnoreRemainingEvents()
            }
            assertThat(dataSource.householdsSubscriptions).isEqualTo(0)
        }

    @Test
    fun getCachedHouseholdCount_countsTheObservedHouseholds() =
        runTest {
            observeHouseholds(householdDto(id = "household-1"), householdDto(id = "household-2"))

            repository.getCachedHouseholdCount().test {
                assertThat(awaitItem()).isEqualTo(2)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun observeItems_emitsItemsOrderedByPosition() =
        runTest {
            // Moving an item only rewrites its own position, so the stored order is not the display order.
            dataSource.items.value =
                listOf(
                    ArticleItemDto(id = "c", text = "c", isChecked = false, position = 3.0),
                    ArticleItemDto(id = "a", text = "a", isChecked = false, position = 1.0),
                    ArticleItemDto(id = "b", text = "b", isChecked = true, position = 1.5),
                )

            repository.observeItems(householdId = "household-1").test {
                assertThat(awaitItem().map { it.id }).containsExactly("a", "b", "c")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun createHousehold_createsHouseholdOwnedByCurrentUser_andKeepsJoinKeyBelowTheLimit() =
        runTest {
            dataSource.joinKeys["join-key"] = TEST_USER.uid

            val id = repository.createHousehold(name = "Cottage", description = "Weekends")

            assertThat(id).isEqualTo("household-new-1")
            assertThat(dataSource.createdHouseholds).containsExactly(
                FakeHouseholdDataSource.CreatedHousehold(ownerId = TEST_USER.uid, name = "Cottage", description = "Weekends"),
            )
            assertThat(dataSource.deletedJoinKeysOfUsers).isEmpty()
        }

    @Test
    fun createHousehold_deletesOwnJoinKey_whenTheNewHouseholdReachesTheLimit() =
        runTest {
            observeHouseholds(*Array(maxHouseholds - 1) { householdDto(id = "household-$it") })
            dataSource.joinKeys["join-key"] = TEST_USER.uid

            val id = repository.createHousehold(name = "Cottage", description = "")

            assertThat(id).isEqualTo("household-new-1")
            assertThat(dataSource.deletedJoinKeysOfUsers).containsExactly(TEST_USER.uid)
        }

    @Test
    fun updateHousehold_updatesTheHouseholdInTheDataSource() =
        runTest {
            repository.updateHousehold(householdId = "household-1", name = "Home", description = "Our flat")

            assertThat(dataSource.updatedHouseholds).containsExactly(
                FakeHouseholdDataSource.UpdatedHousehold(householdId = "household-1", name = "Home", description = "Our flat"),
            )
            assertThat(logger.warnings).isEmpty()
        }

    @Test
    fun addItem_writesItemToTheDataSource_whenHouseholdIsBelowTheItemLimit() =
        runTest {
            repository.addItem(householdId = "household-1", text = "milk", position = 1024.0, currentItemsCount = maxItems - 1)

            assertThat(dataSource.addedItems).containsExactly(
                FakeHouseholdDataSource.AddedItem(householdId = "household-1", text = "milk", position = 1024.0),
            )
            assertThat(logger.warnings).isEmpty()
        }

    // region Error handling

    @Test
    fun createHouseholdJoinKey_returnsNullWithoutCreating_whenUserReachedTheHouseholdLimit() =
        runTest {
            observeHouseholds(*Array(maxHouseholds) { householdDto(id = "household-$it") })

            val key = repository.createHouseholdJoinKey()

            assertThat(key).isNull()
            assertThat(dataSource.createdJoinKeysOfUsers).isEmpty()
            assertThat(logger.warnings).hasSize(1)
        }

    @Test
    fun getHouseholdJoinKey_emitsNoKey_whenFetchingTheKeyFails() =
        runTest {
            dataSource.joinKeyFailure = DataException(DataException.Reason.Offline, "offline")

            repository.getHouseholdJoinKey().test {
                assertThat(awaitItem()).isNull()
                cancelAndIgnoreRemainingEvents()
            }
            assertThat(logger.warnings).hasSize(1)
            assertThat(logger.exceptions).isEmpty()
        }

    @Test
    fun addMember_returnsFalseWithoutLookup_whenJoinKeyCanNotBeAKey() =
        runTest {
            val keys = listOf("", "   ", "a/b", "../x", "key with spaces")

            val results = keys.associateWith { repository.addMemberToHousehold(householdId = "household-1", userJoinKey = it) }

            assertThat(results).isEqualTo(keys.associateWith { false })
            assertThat(dataSource.addedMembers).isEmpty()
        }

    @Test
    fun addMember_returnsFalseAndRecordsError_whenUserIsNotSignedIn() =
        runTest {
            authService.user.value = null

            val isAdded = repository.addMemberToHousehold(householdId = "household-1", userJoinKey = "abc-123")

            assertThat(isAdded).isFalse()
            assertThat(dataSource.addedMembers).isEmpty()
            assertThat(logger.exceptions).hasSize(1)
        }

    @Test
    fun addMember_returnsFalse_whenHouseholdIsFull() =
        runTest {
            observeHouseholds(householdDto(members = listOf(TEST_USER.uid) + List(maxMembers - 1) { "user-$it" }))
            dataSource.joinKeys["abc-123"] = "user-new"

            val isAdded = repository.addMemberToHousehold(householdId = "household-1", userJoinKey = "abc-123")

            assertThat(isAdded).isFalse()
            assertThat(dataSource.addedMembers).isEmpty()
            assertThat(logger.warnings).hasSize(1)
        }

    @Test
    fun observeHouseholds_stillEmitsHouseholds_whenMemberProfilesCanNotBeLoaded() =
        runTest {
            userRepository.getUsersFailure = DataException(DataException.Reason.Offline, "offline")
            dataSource.households.value = listOf(householdDto())

            repository.observeHouseholds().test {
                assertThat(awaitItem().map { it.id }).containsExactly("household-1")
                cancelAndIgnoreRemainingEvents()
            }
            assertThat(logger.exceptions).isEmpty()
        }

    @Test
    fun observeHouseholds_failsWithoutRetrying_whenQuotaIsExceeded() =
        runTest {
            dataSource.householdsFailure = DataException(DataException.Reason.QuotaExceeded, "quota")

            repository.observeHouseholds().test {
                assertThat(awaitError())
                    .isInstanceOf<DataException>()
                    .prop(DataException::reason)
                    .isEqualTo(DataException.Reason.QuotaExceeded)
            }
            assertThat(dataSource.householdsSubscriptions).isEqualTo(1)
            assertThat(logger.warnings).hasSize(1)
        }

    @Test
    fun observeHouseholds_retriesBeforeFailing_whenListenerFailsForAnotherReason() =
        runTest {
            dataSource.householdsFailure = DataException(DataException.Reason.PermissionDenied, "denied")

            repository.observeHouseholds().test {
                assertThat(awaitError())
                    .isInstanceOf<DataException>()
                    .prop(DataException::reason)
                    .isEqualTo(DataException.Reason.PermissionDenied)
            }
            assertThat(dataSource.householdsSubscriptions).isEqualTo(SUBSCRIPTIONS_WITH_RETRIES)
        }

    @Test
    fun observeItems_retriesBeforeFailing_whenListenerFailsForAnotherReason() =
        runTest {
            dataSource.itemsFailure = DataException(DataException.Reason.PermissionDenied, "denied")

            repository.observeItems(householdId = "household-1").test {
                assertThat(awaitError())
                    .isInstanceOf<DataException>()
                    .prop(DataException::reason)
                    .isEqualTo(DataException.Reason.PermissionDenied)
            }
            assertThat(dataSource.itemsSubscriptions).isEqualTo(SUBSCRIPTIONS_WITH_RETRIES)
        }

    @Test
    fun createHousehold_returnsNullWithoutWriting_whenUserReachedTheHouseholdLimit() =
        runTest {
            observeHouseholds(*Array(maxHouseholds) { householdDto(id = "household-$it") })

            val id = repository.createHousehold(name = "Cottage", description = "")

            assertThat(id).isNull()
            assertThat(dataSource.createdHouseholds).isEmpty()
        }

    @Test
    fun updateHousehold_completesWithoutError_whenServerNeverAcknowledgesTheWrite() =
        runTest {
            dataSource.neverAcknowledgeWrites = true

            repository.updateHousehold(householdId = "household-1", name = "Home", description = "")

            assertThat(logger.exceptions).isEmpty()
            assertThat(logger.warnings).hasSize(1)
        }

    @Test
    fun addItem_doesNotWrite_whenHouseholdReachedTheItemLimit() =
        runTest {
            repository.addItem(householdId = "household-1", text = "milk", position = 1.0, currentItemsCount = maxItems)

            assertThat(dataSource.addedItems).isEmpty()
            assertThat(logger.warnings).hasSize(1)
        }

    @Test
    fun addItem_doesNotWriteAndRecordsError_whenUserIsNotSignedIn() =
        runTest {
            authService.user.value = null

            repository.addItem(householdId = "household-1", text = "milk", position = 1.0, currentItemsCount = 0)

            assertThat(dataSource.addedItems).isEmpty()
            assertThat(logger.exceptions).hasSize(1)
        }

    // endregion
}
