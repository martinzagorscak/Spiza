package com.mzagorscak.spiza.domain.usecase

import com.mzagorscak.spiza.domain.model.DeleteAccountResult
import com.mzagorscak.spiza.domain.repository.AccountRepository

interface DeleteAccountUseCase {
    suspend operator fun invoke(): DeleteAccountResult
}

class DeleteAccountUseCaseImpl(
    private val repository: AccountRepository,
) : DeleteAccountUseCase {
    override suspend operator fun invoke(): DeleteAccountResult = repository.deleteAccount()
}
