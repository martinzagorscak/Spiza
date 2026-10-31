package com.mzagorscak.spiza.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.mzagorscak.spiza.crash.CrashLogger
import com.mzagorscak.spiza.crash.recordError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AndroidAuthService(
    context: Context,
    private val activityProvider: CurrentActivityProvider,
    private val webClientId: String,
    private val crashLogger: CrashLogger,
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
) : AuthService {
    private val credentialManager = CredentialManager.create(context)

    override val authState: Flow<AuthUser?> =
        callbackFlow {
            val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.toAuthUser()) }
            firebaseAuth.addAuthStateListener(listener)
            awaitClose { firebaseAuth.removeAuthStateListener(listener) }
        }

    override val currentUser: AuthUser? get() = firebaseAuth.currentUser?.toAuthUser()

    override suspend fun signIn(provider: AuthProvider): AuthResult =
        when (provider) {
            AuthProvider.Google -> signInWithGoogle()
            AuthProvider.Apple ->
                AuthResult.Failure(UnsupportedOperationException("Apple sign-in is not supported on Android"))
        }

    override suspend fun signOut() {
        firebaseAuth.signOut()
        // The user is already signed out of Firebase; a stale Google credential only affects the next account picker.
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: ClearCredentialException) {
            crashLogger.recordError(context = "Clearing Google credential state failed", error = e)
        }
    }

    private suspend fun signInWithGoogle(): AuthResult {
        val activity =
            activityProvider.current
                ?: return AuthResult.Failure(IllegalStateException("No foreground Activity available"))
        return try {
            val option = GetSignInWithGoogleOption.Builder(webClientId).build()
            val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
            val credential = credentialManager.getCredential(activity, request).credential
            if (credential !is CustomCredential ||
                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                return AuthResult.Failure(IllegalStateException("Unexpected credential type: ${credential.type}"))
            }
            val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            val user =
                firebaseAuth
                    .signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
                    .await()
                    .user
                    ?: return AuthResult.Failure(IllegalStateException("Firebase returned no user"))
            AuthResult.Success(user.toAuthUser())
        } catch (e: GetCredentialCancellationException) {
            AuthResult.Cancelled(message = e.message)
        } catch (e: NoCredentialException) {
            AuthResult.Failure(error = e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AuthResult.Failure(e)
        }
    }

    private fun FirebaseUser.toAuthUser() = AuthUser(uid, displayName, email, photoUrl?.toString())
}
