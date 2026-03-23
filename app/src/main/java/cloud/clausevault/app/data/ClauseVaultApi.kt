package cloud.clausevault.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

class ClauseVaultApi(
    private val apiBaseUrl: String,
    private val tokens: TokenStore,
    private val auth: SupabaseAuthRepository,
) {
    private val base = apiBaseUrl.trimEnd('/')
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private fun authorizedBuilder(): Request.Builder {
        val t = tokens.getAccessToken()
        val b = Request.Builder()
        if (!t.isNullOrBlank()) b.header("Authorization", "Bearer $t")
        return b
    }

    private suspend fun execute(request: Request): okhttp3.Response = withContext(Dispatchers.IO) {
        var res = client.newCall(request).execute()
        if (res.code == 401 && tokens.getRefreshToken() != null) {
            res.close()
            if (auth.refreshAccessToken()) {
                val retry = request.newBuilder()
                    .header("Authorization", "Bearer ${tokens.getAccessToken()}")
                    .build()
                res = client.newCall(retry).execute()
            }
        }
        res
    }

    suspend fun getContracts(): Result<ContractsResponse> = runCatching {
        val req = authorizedBuilder().url("$base/api/contracts").get().build()
        val res = execute(req)
        val text = res.body?.string().orEmpty()
        if (!res.isSuccessful) error(json.decodeFromString<ApiErrorBody>(text).error ?: text)
        json.decodeFromString<ContractsResponse>(text)
    }

    suspend fun uploadContract(file: File, mime: String): Result<UploadResponse> = runCatching {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                file.name,
                file.asRequestBody(mime.toMediaType()),
            )
            .build()
        val req = authorizedBuilder()
            .url("$base/api/upload")
            .post(body)
            .build()
        val res = execute(req)
        val text = res.body?.string().orEmpty()
        if (!res.isSuccessful) {
            val err = runCatching { json.decodeFromString<UploadResponse>(text) }.getOrNull()
            error(err?.error ?: text)
        }
        json.decodeFromString<UploadResponse>(text)
    }

    suspend fun postReview(contractId: String): Result<ReviewResponse> = runCatching {
        val payload = """{"contractId":"$contractId"}"""
        val req = authorizedBuilder()
            .url("$base/api/review")
            .post(payload.toRequestBody(jsonMedia))
            .header("Content-Type", "application/json")
            .build()
        val res = execute(req)
        val text = res.body?.string().orEmpty()
        if (!res.isSuccessful) error(json.decodeFromString<ApiErrorBody>(text).error ?: text)
        json.decodeFromString<ReviewResponse>(text)
    }

    suspend fun getReview(contractId: String): Result<ReviewResponse?> = runCatching {
        val req = authorizedBuilder()
            .url("$base/api/review?contractId=$contractId")
            .get()
            .build()
        val res = execute(req)
        val text = res.body?.string().orEmpty()
        if (res.code == 404) return@runCatching null
        if (!res.isSuccessful) error(json.decodeFromString<ApiErrorBody>(text).error ?: text)
        json.decodeFromString<ReviewResponse>(text)
    }

    suspend fun generateContract(
        contractType: String,
        prompt: String,
        party1: String,
        party2: String,
    ): Result<GenerateResponse> = runCatching {
        val parties =
            if (party1.isNotBlank() || party2.isNotBlank()) GenerateParties(party1, party2) else null
        val payload = json.encodeToString(
            GenerateRequestBody(contractType = contractType, prompt = prompt, parties = parties),
        )
        val req = authorizedBuilder()
            .url("$base/api/generate")
            .post(payload.toRequestBody(jsonMedia))
            .header("Content-Type", "application/json")
            .build()
        val res = execute(req)
        val text = res.body?.string().orEmpty()
        if (!res.isSuccessful) {
            val g = runCatching { json.decodeFromString<GenerateResponse>(text) }.getOrNull()
            error(g?.error ?: g?.details ?: text)
        }
        json.decodeFromString<GenerateResponse>(text)
    }

    suspend fun negotiate(contractId: String, aggressiveness: String): Result<NegotiateResponse> = runCatching {
        val payload =
            """{"contractId":"$contractId","aggressiveness":"$aggressiveness"}"""
        val req = authorizedBuilder()
            .url("$base/api/negotiate")
            .post(payload.toRequestBody(jsonMedia))
            .header("Content-Type", "application/json")
            .build()
        val res = execute(req)
        val text = res.body?.string().orEmpty()
        if (!res.isSuccessful) error(json.decodeFromString<ApiErrorBody>(text).error ?: text)
        json.decodeFromString<NegotiateResponse>(text)
    }

    suspend fun analytics(): Result<AnalyticsResponse> = runCatching {
        val req = authorizedBuilder().url("$base/api/analytics").get().build()
        val res = execute(req)
        val text = res.body?.string().orEmpty()
        if (!res.isSuccessful) error(json.decodeFromString<ApiErrorBody>(text).error ?: text)
        json.decodeFromString<AnalyticsResponse>(text)
    }

    suspend fun playbooks(): Result<PlaybooksResponse> = runCatching {
        val req = authorizedBuilder().url("$base/api/playbooks").get().build()
        val res = execute(req)
        val text = res.body?.string().orEmpty()
        if (!res.isSuccessful) error(json.decodeFromString<ApiErrorBody>(text).error ?: text)
        json.decodeFromString<PlaybooksResponse>(text)
    }

    suspend fun createPlaybook(name: String, description: String, category: String, rulesJson: String): Result<Unit> =
        runCatching {
            val rulesPart = rulesJson.ifBlank { "[]" }
            val payload =
                """{"name":${json.encodeToString(name)},"description":${json.encodeToString(description)},"category":${json.encodeToString(category)},"rules":$rulesPart}"""
            val req = authorizedBuilder()
                .url("$base/api/playbooks")
                .post(payload.toRequestBody(jsonMedia))
                .header("Content-Type", "application/json")
                .build()
            val res = execute(req)
            val text = res.body?.string().orEmpty()
            if (!res.isSuccessful) error(json.decodeFromString<ApiErrorBody>(text).error ?: text)
        }

    suspend fun deletePlaybook(id: String): Result<Unit> = runCatching {
        val req = authorizedBuilder()
            .url("$base/api/playbooks?id=$id")
            .delete()
            .build()
        val res = execute(req)
        val text = res.body?.string().orEmpty()
        if (!res.isSuccessful) error(json.decodeFromString<ApiErrorBody>(text).error ?: text)
    }

    suspend fun billing(): Result<BillingResponse> = runCatching {
        val req = authorizedBuilder().url("$base/api/billing").get().build()
        val res = execute(req)
        val text = res.body?.string().orEmpty()
        if (!res.isSuccessful) error(json.decodeFromString<ApiErrorBody>(text).error ?: text)
        json.decodeFromString<BillingResponse>(text)
    }

    suspend fun billingCheckout(planId: String): Result<String> = runCatching {
        val payload = """{"action":"checkout","planId":"$planId"}"""
        val req = authorizedBuilder()
            .url("$base/api/billing")
            .post(payload.toRequestBody(jsonMedia))
            .header("Content-Type", "application/json")
            .build()
        val res = execute(req)
        val text = res.body?.string().orEmpty()
        if (!res.isSuccessful) error(json.decodeFromString<BillingUrlResponse>(text).error ?: text)
        json.decodeFromString<BillingUrlResponse>(text).url ?: error("No checkout URL")
    }

    suspend fun billingPortal(): Result<String> = runCatching {
        val payload = """{"action":"portal"}"""
        val req = authorizedBuilder()
            .url("$base/api/billing")
            .post(payload.toRequestBody(jsonMedia))
            .header("Content-Type", "application/json")
            .build()
        val res = execute(req)
        val text = res.body?.string().orEmpty()
        if (!res.isSuccessful) error(json.decodeFromString<BillingUrlResponse>(text).error ?: text)
        json.decodeFromString<BillingUrlResponse>(text).url ?: error("No portal URL")
    }

    suspend fun settings(): Result<SettingsResponse> = runCatching {
        val req = authorizedBuilder().url("$base/api/settings").get().build()
        val res = execute(req)
        val text = res.body?.string().orEmpty()
        if (!res.isSuccessful) error(json.decodeFromString<ApiErrorBody>(text).error ?: text)
        json.decodeFromString<SettingsResponse>(text)
    }

    suspend fun saveSettings(body: SettingsPutBody): Result<Unit> = runCatching {
        val payload = json.encodeToString(body)
        val req = authorizedBuilder()
            .url("$base/api/settings")
            .put(payload.toRequestBody(jsonMedia))
            .header("Content-Type", "application/json")
            .build()
        val res = execute(req)
        val text = res.body?.string().orEmpty()
        if (!res.isSuccessful) error(json.decodeFromString<ApiErrorBody>(text).error ?: text)
    }

    suspend fun exportBytes(contractId: String, format: String): Result<ByteArray> = runCatching {
        val req = authorizedBuilder()
            .url("$base/api/export?contractId=$contractId&format=$format")
            .get()
            .build()
        val res = execute(req)
        if (!res.isSuccessful) {
            val text = res.body?.string().orEmpty()
            error(json.decodeFromString<ApiErrorBody>(text).error ?: text)
        }
        res.body?.bytes() ?: error("Empty body")
    }

    suspend fun exportDraftDocx(draft: String, title: String): Result<ByteArray> = runCatching {
        val payload =
            """{"draft":${json.encodeToString(draft)},"title":${json.encodeToString(title)}}"""
        val req = authorizedBuilder()
            .url("$base/api/export")
            .post(payload.toRequestBody(jsonMedia))
            .header("Content-Type", "application/json")
            .build()
        val res = execute(req)
        if (!res.isSuccessful) {
            val text = res.body?.string().orEmpty()
            error(json.decodeFromString<ApiErrorBody>(text).error ?: text)
        }
        res.body?.bytes() ?: error("Empty body")
    }

    suspend fun deployZip(): Result<ByteArray> = runCatching {
        val req = authorizedBuilder().url("$base/api/deploy").get().build()
        val res = execute(req)
        if (!res.isSuccessful) {
            val text = res.body?.string().orEmpty()
            error(json.decodeFromString<ApiErrorBody>(text).error ?: text)
        }
        res.body?.bytes() ?: error("Empty body")
    }
}
