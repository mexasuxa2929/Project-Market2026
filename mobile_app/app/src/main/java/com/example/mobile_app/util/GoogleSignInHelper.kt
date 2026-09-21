package com.example.mobile_app.util

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialOption
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object GoogleSignInHelper {

    private const val SERVER_CLIENT_ID = "758985644197-6cg7sel08sg5mjubod3pdhsvt6q07h2s.apps.googleusercontent.com"

    suspend fun signIn(context: Context): Result<String> = try {
        val credentialManager = CredentialManager.create(context)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(SERVER_CLIENT_ID)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(context, request)
        val credential = GoogleIdTokenCredential.createFrom(result.credential.data)
        Result.success(credential.idToken)
    } catch (e: NoCredentialException) {
        Result.failure(Exception("Google hisob topilmadi. Iltimos, Google hisobini qurilmaga qo'shing."))
    } catch (e: GetCredentialCancellationException) {
        Result.failure(Exception("Google kirish bekor qilindi."))
    } catch (e: Exception) {
        Result.failure(e)
    }
}
