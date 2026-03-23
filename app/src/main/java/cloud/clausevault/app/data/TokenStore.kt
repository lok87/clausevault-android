package cloud.clausevault.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenStore(context: Context) {

    private val prefs: SharedPreferences =
        EncryptedSharedPreferences.create(
            context,
            "clausevault_auth",
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

    fun hasSession(): Boolean = !getAccessToken().isNullOrBlank()

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS, null)

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH, null)

    fun save(access: String, refresh: String?, expiresAtEpochSec: Long) {
        val ed = prefs.edit().putString(KEY_ACCESS, access).putLong(KEY_EXPIRES, expiresAtEpochSec)
        if (refresh != null) ed.putString(KEY_REFRESH, refresh)
        ed.apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
        const val KEY_EXPIRES = "expires_at"
    }
}
