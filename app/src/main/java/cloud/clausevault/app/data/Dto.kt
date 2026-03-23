package cloud.clausevault.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ContractRow(
    val id: String,
    val title: String,
    @SerialName("file_type") val fileType: String,
    val status: String,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class ContractsResponse(val contracts: List<ContractRow> = emptyList())

@Serializable
data class UploadResponse(
    val contractId: String? = null,
    val message: String? = null,
    val pageCount: Int? = null,
    val chunkCount: Int? = null,
    val error: String? = null,
)

@Serializable
data class ClauseDto(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    @SerialName("clauseType") val clauseType: String = "",
    val riskScore: Int = 0,
    val riskReason: String = "",
    val suggestedRevision: String? = null,
    val position: Int = 0,
)

@Serializable
data class ComplianceIssueDto(
    val issue: String = "",
    val severity: String = "",
    val clause: String = "",
    val recommendation: String = "",
)

@Serializable
data class ReviewResponse(
    val overallRiskScore: Int = 0,
    val summary: String = "",
    val extractedFields: Map<String, JsonElement> = emptyMap(),
    val clauses: List<ClauseDto> = emptyList(),
    val complianceIssues: List<ComplianceIssueDto> = emptyList(),
    val error: String? = null,
)

@Serializable
data class NegotiationChangeDto(
    val clause: String = "",
    val original: String = "",
    val proposed: String = "",
    val reasoning: String = "",
)

@Serializable
data class NegotiationOptionDto(
    val id: String = "",
    val label: String = "",
    val description: String = "",
    val changes: List<NegotiationChangeDto> = emptyList(),
    val acceptanceProbability: Int = 0,
    val riskDelta: Double = 0.0,
)

@Serializable
data class NegotiateResponse(
    val options: List<NegotiationOptionDto> = emptyList(),
    val errors: List<String> = emptyList(),
    val error: String? = null,
)

@Serializable
data class GenerateRequestBody(
    val contractType: String,
    val prompt: String,
    val parties: GenerateParties? = null,
)

@Serializable
data class GenerateParties(
    val party1: String,
    val party2: String,
)

@Serializable
data class GenerateResponse(
    val draft: String? = null,
    val errors: List<String> = emptyList(),
    val error: String? = null,
    val details: String? = null,
)

@Serializable
data class MonthlyDatum(
    val month: String = "",
    val contracts: Int = 0,
    val avgRisk: Int = 0,
)

@Serializable
data class TopRiskRow(
    val clause: String = "",
    val avgScore: Int = 0,
    val count: Int = 0,
)

@Serializable
data class RiskDistribution(
    val low: Int = 0,
    val medium: Int = 0,
    val high: Int = 0,
    val critical: Int = 0,
)

@Serializable
data class AnalyticsResponse(
    val totalContracts: Int = 0,
    val reviewedContracts: Int = 0,
    val avgRisk: Int = 0,
    val timeSavedMinutes: Int = 0,
    val timeSavedPercent: Int = 0,
    val riskDistribution: RiskDistribution = RiskDistribution(),
    val monthlyData: List<MonthlyDatum> = emptyList(),
    val topRisks: List<TopRiskRow> = emptyList(),
    val error: String? = null,
)

@Serializable
data class PlaybookRow(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val description: String = "",
    val rules: List<JsonElement> = emptyList(),
    @SerialName("is_custom") val isCustom: Boolean = false,
)

@Serializable
data class PlaybooksResponse(val playbooks: List<PlaybookRow> = emptyList(), val error: String? = null)

@Serializable
data class BillingPlanDto(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val features: List<String> = emptyList(),
    val contractLimit: Int = 0,
    val generationLimit: Int = 0,
)

@Serializable
data class BillingUsageDto(
    @SerialName("reviewsUsed") val reviewsUsed: Int = 0,
    @SerialName("generationsUsed") val generationsUsed: Int = 0,
)

@Serializable
data class BillingResponse(
    val plans: List<BillingPlanDto> = emptyList(),
    val usage: BillingUsageDto = BillingUsageDto(),
    @SerialName("currentPlan") val currentPlan: String = "starter",
    @SerialName("stripeCustomerId") val stripeCustomerId: String? = null,
    @SerialName("subscriptionStatus") val subscriptionStatus: String? = null,
    @SerialName("canManageBilling") val canManageBilling: Boolean = false,
    val error: String? = null,
)

@Serializable
data class BillingUrlResponse(val url: String? = null, val error: String? = null)

@Serializable
data class SettingsResponse(
    val ai_provider: String = "gemini",
    val model: String = "gemini-2.5-flash",
    val ollama_url: String = "http://localhost:11434",
    val anthropic_key: String = "",
    val currentPlan: String = "starter",
    val error: String? = null,
)

@Serializable
data class SettingsPutBody(
    val ai_provider: String,
    val model: String,
    val ollama_url: String = "",
    val anthropic_key: String = "",
)

@Serializable
data class ApiErrorBody(val error: String? = null)
