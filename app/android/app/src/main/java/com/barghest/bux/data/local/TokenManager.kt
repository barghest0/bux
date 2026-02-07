package com.barghest.bux.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenManager(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = createSecurePrefsWithRecovery(appContext)

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    fun clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean = getToken() != null

    companion object {
        private const val TAG = "TokenManager"
        private const val PREFS_NAME = "bux_secure_prefs"
        private const val FALLBACK_PREFS_NAME = "bux_secure_prefs_fallback"
        private const val KEY_TOKEN = "jwt_token"
    }

    private fun createSecurePrefsWithRecovery(context: Context): SharedPreferences {
        createEncryptedPrefsOrNull(context)?.let { return it }

        Log.w(TAG, "EncryptedSharedPreferences init failed, clearing secure prefs and retrying")
        context.deleteSharedPreferences(PREFS_NAME)

        createEncryptedPrefsOrNull(context)?.let { return it }

        Log.e(TAG, "EncryptedSharedPreferences unavailable, using fallback SharedPreferences")
        return context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun createEncryptedPrefsOrNull(context: Context): SharedPreferences? {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create encrypted preferences", e)
            null
        }
    }
}
