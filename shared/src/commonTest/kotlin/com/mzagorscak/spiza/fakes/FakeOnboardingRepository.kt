package com.mzagorscak.spiza.fakes

import com.mzagorscak.spiza.domain.repository.OnboardingRepository
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Every completion is recorded as an attempt before [writeFailure] / [neverCompleteWrites] apply; successful ones are reflected in
 * [completed].
 */
class FakeOnboardingRepository : OnboardingRepository {
    /** The persisted "completed" flag; seed it directly. */
    val completed = MutableStateFlow(false)

    /** When set, [setOnboardingCompleted] throws it. */
    var writeFailure: Throwable? = null

    /** [setOnboardingCompleted] never completes, so a test can act while the write is in flight. */
    var neverCompleteWrites = false

    var completeAttempts = 0
        private set

    override val onboardingCompleted: Flow<Boolean> = completed

    override val isOnboardingCompleted: Boolean get() = completed.value

    override suspend fun setOnboardingCompleted() {
        completeAttempts++
        writeFailure?.let { throw it }
        if (neverCompleteWrites) awaitCancellation()
        completed.value = true
    }
}
