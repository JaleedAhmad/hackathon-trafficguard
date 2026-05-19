package com.traffic_guard.ai.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.widget.Toast
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Wraps the Android Credential Manager Google Sign-In flow.
 *
 * Usage:
 *   val helper = GoogleSignInHelper(context)
 *   val idToken = helper.getGoogleIdToken()  // null on failure
 *   viewModel.signInWithGoogle(idToken) { ... }
 */
class GoogleSignInHelper(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)

    private fun Context.findActivity(): Activity? {
        var ctx = this
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    suspend fun getGoogleIdToken(webClientId: String): String? {
        val activityContext = context.findActivity() ?: context
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context = activityContext, request = request)
            val credential = GoogleIdTokenCredential.createFrom(result.credential.data)
            credential.idToken
        } catch (e: GetCredentialException) {
            Log.e("GoogleSignInHelper", "GetCredentialException: ${e.message}", e)
            Toast.makeText(context, "Google Sign-In Cancelled or Unavailable: ${e.message}", Toast.LENGTH_LONG).show()
            null
        } catch (e: Exception) {
            Log.e("GoogleSignInHelper", "Exception: ${e.message}", e)
            Toast.makeText(context, "Google Sign-In Error: ${e.message}", Toast.LENGTH_LONG).show()
            null
        }
    }
}

fun Context.getGoogleWebClientId(): String {
    return try {
        val appInfo = packageManager.getApplicationInfo(packageName, android.content.pm.PackageManager.GET_META_DATA)
        appInfo.metaData.getString("GOOGLE_WEB_CLIENT_ID") ?: "777173229835-q572kphfh24h8sl5j52iu3m4vgrri6f3.apps.googleusercontent.com"
    } catch (e: Exception) {
        "777173229835-q572kphfh24h8sl5j52iu3m4vgrri6f3.apps.googleusercontent.com"
    }
}

