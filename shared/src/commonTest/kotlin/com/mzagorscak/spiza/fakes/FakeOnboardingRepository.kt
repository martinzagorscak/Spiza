package com.mzagorscak.spiza.fakes

import com.mzagorscak.spiza.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/** Every completion is recorded as an attempt before [writeFailure] applies; successful ones are reflected in [completed]. */
class FakeOnboardingRepository : OnboardingRepository {
    /** The persisted "completed" flag; seed it directly. */
    val completed = MutableStateFlow(false)

    /** When set, [onboardingCompleted] fails with it (checked each time the stream is collected). */
    var observeFailure: Throwable? = null

    /** When set, [setOnboardingCompleted] throws it. */
    var writeFailure: Throwable? = null

    var completeAttempts = 0
        private set

    override val onboardingCompleted: Flow<Boolean> =
        flow {
            observeFailure?.let { throw it }
            emitAll(completed)
        }

    override val isOnboardingCompleted: Boolean get() = completed.value

    override suspend fun setOnboardingCompleted() {
        completeAttempts++
        writeFailure?.let { throw it }
        completed.value = true
    }
}
