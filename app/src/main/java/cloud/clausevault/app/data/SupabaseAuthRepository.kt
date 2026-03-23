package cloud.clausevault.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.parseToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

@Serializable
private data class PasswordTokenBody(val email: String, val password: String)

@Serializable
private data class SignUpBody(val email: String, val password: String)

@Serializable
private data class RefreshBody(@SerialName("refresh_token") val refreshToken: String)

@Serializable
private data class OtpBody(val email: String)

@Serializable
private data class TokenResponse(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long = 3600,
    @SerialName("expires_at") val expiresAt: Long? = null,
)

class SupabaseAuthRepository(
    private val supabaseUrl: String,
    private val anonKey: String,
    private val tokens: TokenStore,
) {
    private val base = supabaseUrl.trimEnd('/')
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient()

    private fun expiresAtEpoch(session: TokenResponse): Long {
        session.expiresAt?.let { return it }
        return System.currentTimeMillis() / 1000 + (session.expiresIn.takeIf { it > 0 } ?: 3600L)
    }

    private fun persist(session: TokenResponse) {
        val access = session.accessToken ?: return
        tokens.save(access, session.refreshToken, expiresAtEpoch(session))
    }

    private fun parseSessionFromAuthJson(text: String): TokenResponse? {
        return runCatching {
            val o = json.parseToJsonElement(text).jsonObject
            o["session"]?.let { json.decodeFromJsonElement<TokenResponse>(it) }
                ?: json.decodeFromString<TokenResponse>(text)
        }.getOrNull()
    }

    suspend fun signInWithPassword(email: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val body = json.encodeToString(PasswordTokenBody(email.trim(), password))
            val req = Request.Builder()
                .url("$base/auth/v1/token?grant_type=password")
                .post(body.toRequestBody(jsonMedia))
                .header("apikey", anonKey)
                .header("Content-Type", "application/json")
                .build()
            val res = client.newCall(req).execute()
            val text = res.body?.string().orEmpty()
            if (!res.isSuccessful) {
                val err = runCatching { json.decodeFromString<ApiErrorBody>(text) }.getOrNull()
                error(err?.error ?: text.ifBlank { "Sign in failed (${res.code})" })
            }
            val session = json.decodeFromString<TokenResponse>(text)
            persist(session)
        }
    }

    suspend fun signUp(email: String, password: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val body = json.encodeToString(SignUpBody(email.trim(), password))
            val req = Request.Builder()
                .url("$base/auth/v1/signup")
                .post(body.toRequestBody(jsonMedia))
                .header("apikey", anonKey)
                .header("Content-Type", "application/json")
                .build()
            val res = client.newCall(req).execute()
            val text = res.body?.string().orEmpty()
            if (!res.isSuccessful) {
                val msg = runCatching {
                    json.parseToJsonElement(text).jsonObject["error_description"]?.jsonPrimitive?.content
                        ?: json.parseToJsonElement(text).jsonObject["msg"]?.jsonPrimitive?.content
                }.getOrNull()
                error(msg ?: text.ifBlank { "Sign up failed (${res.code})" })
            }
            val session = parseSessionFromAuthJson(text)
            if (session?.accessToken != null) {
                persist(session)
                true
            } else {
                false
            }
        }
    }

    suspend fun sendMagicLink(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val body = json.encodeToString(OtpBody(email.trim()))
            val req = Request.Builder()
                .url("$base/auth/v1/otp")
                .post(body.toRequestBody(jsonMedia))
                .header("apikey", anonKey)
                .header("Content-Type", "application/json")
                .build()
            val res = client.newCall(req).execute()
            val text = res.body?.string().orEmpty()
            if (!res.isSuccessful) {
                val err = runCatching { json.decodeFromString<ApiErrorBody>(text) }.getOrNull()
                error(err?.error ?: text.ifBlank { "Could not send magic link (${res.code})" })
            }
        }
    }

    suspend fun refreshAccessToken(): Boolean = withContext(Dispatchers.IO) {
        val rt = tokens.getRefreshToken() ?: return@withContext false
        runCatching {
            val body = json.encodeToString(RefreshBody(rt))
            val req = Request.Builder()
                .url("$base/auth/v1/token?grant_type=refresh_token")
                .post(body.toRequestBody(jsonMedia))
                .header("apikey", anonKey)
                .header("Content-Type", "application/json")
                .build()
            val res = client.newCall(req).execute()
            val text = res.body?.string().orEmpty()
            if (!res.isSuccessful) return@runCatching false
            val session = json.decodeFromString<TokenResponse>(text)
            if (session.accessToken != null) {
                persist(session)
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }

    fun signOut() {
        tokens.clear()
    }
}
