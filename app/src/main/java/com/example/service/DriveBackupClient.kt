package com.example.service

import android.accounts.Account
import android.app.PendingIntent
import android.content.Context
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Thrown when the user still has to grant Drive access; launch [pendingIntent] from an Activity. */
class DriveConsentRequiredException(val pendingIntent: PendingIntent) :
    Exception("Google Drive access has not been granted yet")

/**
 * Stores a single JSON backup in the hidden per-app `appDataFolder` of the user's Google Drive.
 * The folder is invisible to the user and to other apps, and needs only the drive.appdata scope.
 */
object DriveBackupClient {
    private const val SCOPE_APPDATA = "https://www.googleapis.com/auth/drive.appdata"
    private const val BACKUP_FILE_NAME = "syncwell_backup.json"
    private const val FILES_URL = "https://www.googleapis.com/drive/v3/files"
    private const val UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"
    private val JSON = "application/json; charset=utf-8".toMediaType()

    private val http = OkHttpClient()

    suspend fun accessToken(context: Context, email: String?): String {
        val builder = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(SCOPE_APPDATA)))
        if (!email.isNullOrBlank() && email.contains("@")) {
            builder.setAccount(Account(email, "com.google"))
        }
        val result = Identity.getAuthorizationClient(context).authorize(builder.build()).await()
        if (result.hasResolution()) {
            throw DriveConsentRequiredException(result.pendingIntent!!)
        }
        return result.accessToken ?: throw IOException("Google did not return a Drive access token")
    }

    suspend fun upload(token: String, json: String) = withContext(Dispatchers.IO) {
        val existingId = findBackupId(token)
        val fileId = existingId ?: createEmptyFile(token)
        val request = Request.Builder()
            .url("$UPLOAD_URL/$fileId?uploadType=media")
            .header("Authorization", "Bearer $token")
            .patch(json.toRequestBody(JSON))
            .build()
        execute(request) { }
    }

    /** Returns the backup JSON, or null when this account has no backup yet. */
    suspend fun download(token: String): String? = withContext(Dispatchers.IO) {
        val fileId = findBackupId(token) ?: return@withContext null
        val request = Request.Builder()
            .url("$FILES_URL/$fileId?alt=media")
            .header("Authorization", "Bearer $token")
            .build()
        execute(request) { it }
    }

    suspend fun hasBackup(token: String): Boolean = withContext(Dispatchers.IO) {
        findBackupId(token) != null
    }

    private fun findBackupId(token: String): String? {
        val url = FILES_URL.toHttpUrl().newBuilder()
            .addQueryParameter("spaces", "appDataFolder")
            .addQueryParameter("q", "name = '$BACKUP_FILE_NAME' and trashed = false")
            .addQueryParameter("fields", "files(id)")
            .build()
        val request = Request.Builder().url(url).header("Authorization", "Bearer $token").build()
        return execute(request) { body ->
            val files = JSONObject(body).optJSONArray("files")
            if (files != null && files.length() > 0) files.getJSONObject(0).getString("id") else null
        }
    }

    private fun createEmptyFile(token: String): String {
        val metadata = JSONObject()
            .put("name", BACKUP_FILE_NAME)
            .put("parents", org.json.JSONArray().put("appDataFolder"))
        val request = Request.Builder()
            .url("$FILES_URL?fields=id")
            .header("Authorization", "Bearer $token")
            .post(metadata.toString().toRequestBody(JSON))
            .build()
        return execute(request) { JSONObject(it).getString("id") }
    }

    private fun <T> execute(request: Request, parse: (String) -> T): T {
        http.newCall(request).execute().use { response ->
            val body = response.body.string()
            if (!response.isSuccessful) {
                val reason = runCatching { JSONObject(body).getJSONObject("error").getString("message") }
                    .getOrDefault(response.message)
                throw IOException("Google Drive error ${response.code}: $reason")
            }
            return parse(body)
        }
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
        addOnCanceledListener { cont.cancel() }
    }
}
