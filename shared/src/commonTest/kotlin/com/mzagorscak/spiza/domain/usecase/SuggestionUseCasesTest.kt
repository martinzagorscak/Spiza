package com.mzagorscak.spiza.domain.usecase

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import com.mzagorscak.spiza.fakes.FakeSuggestionRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

private const val TIMESTAMP_MS = 1_700_000_000_000L

class SuggestionUseCasesTest {
    private val repository = FakeSuggestionRepository()

    @Test
    fun addOrUpdate_trimsName_andNormalizesItToLowercase() =
        runTest {
            AddOrUpdateSuggestionUseCaseImpl(repository)(name = "  Fresh Milk ", timestamp = TIMESTAMP_MS)

            assertThat(repository.saved).containsExactly(
                FakeSuggestionRepository.Saved(name = "Fresh Milk", normalizedName = "fresh milk", timestamp = TIMESTAMP_MS),
            )
        }

    @Test
    fun addOrUpdate_ignoresBlankNames() =
        runTest {
            val addOrUpdate = AddOrUpdateSuggestionUseCaseImpl(repository)

            addOrUpdate(name = "", timestamp = TIMESTAMP_MS)
            addOrUpdate(name = "   ", timestamp = TIMESTAMP_MS)

            assertThat(repository.saved).isEmpty()
        }
}
