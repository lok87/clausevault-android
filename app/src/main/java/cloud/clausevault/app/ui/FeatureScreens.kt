package cloud.clausevault.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import cloud.clausevault.app.data.BillingResponse
import cloud.clausevault.app.data.AnalyticsResponse
import cloud.clausevault.app.data.ClauseVaultApi
import cloud.clausevault.app.data.ContractRow
import cloud.clausevault.app.data.NegotiationOptionDto
import cloud.clausevault.app.data.PlaybookRow
import cloud.clausevault.app.data.ReviewResponse
import cloud.clausevault.app.data.SettingsPutBody
import cloud.clausevault.app.data.SupabaseAuthRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(api: ClauseVaultApi, nav: NavHostController) {
    var contracts by remember { mutableStateOf<List<ContractRow>>(emptyList()) }
    var analytics by remember { mutableStateOf<AnalyticsResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        loading = true
        val c = api.getContracts().getOrNull()
        val a = api.analytics().getOrNull()
        contracts = c?.contracts.orEmpty()
        analytics = a
        loading = false
    }

    Column(Modifier.padding(16.dp)) {
        Text("Dashboard", style = MaterialTheme.typography.headlineMedium)
        Text(
            "AI-powered contract review at a glance",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        if (loading) {
            CircularProgressIndicator()
        } else {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatMini("Contracts", "${analytics?.totalContracts ?: 0}")
                StatMini("Reviewed", "${analytics?.reviewedContracts ?: 0}")
                StatMini("Avg risk", "${analytics?.avgRisk ?: 0}")
                StatMini("Time saved", "${analytics?.timeSavedPercent ?: 0}%")
            }
            Spacer(Modifier.height(20.dp))
            Text("Recent contracts", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (contracts.isEmpty()) {
                Text("No contracts yet. Upload from the Upload tab.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                contracts.take(12).forEach { c ->
                    Card(
                        onClick = { nav.navigate(Routes.review(c.id)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    ) {
                        ListItem(
                            headlineContent = { Text(c.title, maxLines = 2) },
                            supportingContent = { Text("${c.fileType.uppercase()} · ${c.status}") },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatMini(label: String, value: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun UploadScreen(api: ClauseVaultApi) {
    val context = LocalContext.current
    var status by remember { mutableStateOf<String?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            busy = true
            err = null
            status = null
            runCatching {
                val (file, mime) = copyUriToCacheFile(context, uri)
                api.uploadContract(file, mime).getOrThrow()
            }.onSuccess { r ->
                status = r.message ?: "Uploaded ${r.contractId}"
            }.onFailure { e ->
                err = e.message
            }
            busy = false
        }
    }

    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Upload", style = MaterialTheme.typography.headlineMedium)
        Text("PDF, DOCX, or DOC (max 50MB)", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(
            onClick = { pick.launch(arrayOf("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")) },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (busy) "Processing…" else "Choose file")
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        status?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        err?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

private val ContractTypes = listOf(
    "nda" to "NDA",
    "msa" to "MSA",
    "saas" to "SaaS",
    "vendor" to "Vendor",
    "employment" to "Employment",
    "consulting" to "Consulting",
    "licensing" to "Licensing",
    "partnership" to "Partnership",
)

@Composable
fun GenerateScreen(api: ClauseVaultApi) {
    var type by remember { mutableStateOf(ContractTypes[0].first) }
    var expanded by remember { mutableStateOf(false) }
    var party1 by remember { mutableStateOf("") }
    var party2 by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Generate", style = MaterialTheme.typography.headlineMedium)
        }
        item {
            Text("Contract type", style = MaterialTheme.typography.labelMedium)
            Box(Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(ContractTypes.find { it.first == type }?.second ?: type)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    ContractTypes.forEach { (v, l) ->
                        DropdownMenuItem(
                            text = { Text(l) },
                            onClick = { type = v; expanded = false },
                        )
                    }
                }
            }
        }
        item {
            OutlinedTextField(party1, { party1 = it }, label = { Text("Party 1") }, modifier = Modifier.fillMaxWidth())
        }
        item {
            OutlinedTextField(party2, { party2 = it }, label = { Text("Party 2") }, modifier = Modifier.fillMaxWidth())
        }
        item {
            OutlinedTextField(
                prompt,
                { prompt = it },
                label = { Text("Instructions (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )
        }
        item {
            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        err = null
                        api.generateContract(type, prompt, party1, party2)
                            .onSuccess { draft = it.draft.orEmpty().trim(); if (draft.isEmpty()) err = it.error ?: it.details ?: "No text" }
                            .onFailure { err = it.message }
                        busy = false
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (busy) "Generating…" else "Generate") }
        }
        if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        err?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        if (draft.isNotBlank()) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        scope.launch {
                            api.exportDraftDocx(draft, "${type}-draft")
                                .onSuccess { shareExportBytes(context, it, "${type}-draft.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document") }
                        }
                    }) { Text("Export DOCX") }
                }
            }
            item {
                Text(draft, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
fun AnalyticsScreen(api: ClauseVaultApi) {
    var data by remember { mutableStateOf<AnalyticsResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        loading = true
        data = api.analytics().getOrNull()
        loading = false
    }

    Column(Modifier.padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Analytics", style = MaterialTheme.typography.headlineMedium)
            OutlinedButton(
                onClick = {
                    val d = data ?: return@OutlinedButton
                    val csv = buildString {
                        appendLine("Clause,Average Risk Score,Contract Count")
                        d.topRisks.forEach { appendLine("${it.clause},${it.avgScore},${it.count}") }
                    }
                    shareExportBytes(context, csv.toByteArray(), "contract-analytics.csv", "text/csv")
                },
                enabled = !data?.topRisks.isNullOrEmpty(),
            ) { Text("Export CSV") }
        }
        if (loading) {
            CircularProgressIndicator()
        } else {
            val a = data
            if (a == null) {
                Text("Could not load analytics", color = MaterialTheme.colorScheme.error)
                return@Column
            }
            Text("Review rate: ${if (a.totalContracts > 0) (a.reviewedContracts * 100 / a.totalContracts) else 0}%")
            Spacer(Modifier.height(12.dp))
            Text("Risk distribution", style = MaterialTheme.typography.titleSmall)
            Text("Low ${a.riskDistribution.low} · Med ${a.riskDistribution.medium} · High ${a.riskDistribution.high} · Critical ${a.riskDistribution.critical}")
            Spacer(Modifier.height(16.dp))
            Text("Monthly activity", style = MaterialTheme.typography.titleSmall)
            a.monthlyData.forEach { m ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(m.month)
                    Text("${m.contracts} contracts · risk ${m.avgRisk}")
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Top risks (heatmap)", style = MaterialTheme.typography.titleSmall)
            a.topRisks.forEach { r ->
                Column(Modifier.padding(vertical = 6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(r.clause, modifier = Modifier.weight(1f), maxLines = 2, style = MaterialTheme.typography.bodySmall)
                        Text("${r.avgScore}", fontWeight = FontWeight.Bold)
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(r.avgScore / 100f)
                                .height(8.dp)
                                .background(riskHeatColor(r.avgScore)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MoreScreen(
    nav: NavHostController,
    auth: SupabaseAuthRepository,
    onSignOut: () -> Unit,
) {
    Column(Modifier.padding(16.dp)) {
        Text("More", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        MoreRow("Negotiate", Icons.Default.Gavel) { nav.navigate(Routes.Negotiate) }
        MoreRow("Playbooks", Icons.Default.Book) { nav.navigate(Routes.Playbooks) }
        MoreRow("Settings", Icons.Default.Settings) { nav.navigate(Routes.Settings) }
        MoreRow("Billing", Icons.Default.CreditCard) { nav.navigate(Routes.Billing) }
        MoreRow("Deploy on-prem", Icons.Default.CloudDownload) { nav.navigate(Routes.Deploy) }
        Spacer(Modifier.height(24.dp))
        OutlinedButton(
            onClick = {
                auth.signOut()
                onSignOut()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Logout, null)
            Spacer(Modifier.width(8.dp))
            Text("Sign out")
        }
    }
}

@Composable
private fun MoreRow(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        ListItem(
            headlineContent = { Text(label) },
            leadingContent = { Icon(icon, null) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(contractId: String, api: ClauseVaultApi, nav: NavHostController) {
    var review by remember { mutableStateOf<ReviewResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var reviewing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(contractId) {
        loading = true
        review = api.getReview(contractId).getOrNull()
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    review?.let { r ->
                        if (r.clauses.isNotEmpty()) {
                            TextButton(onClick = {
                                scope.launch {
                                    api.exportBytes(contractId, "docx")
                                        .onSuccess {
                                            shareExportBytes(context, it, "review.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                                        }
                                }
                            }) { Text("DOCX") }
                            TextButton(onClick = {
                                scope.launch {
                                    api.exportBytes(contractId, "csv")
                                        .onSuccess { shareExportBytes(context, it, "review.csv", "text/csv") }
                                }
                            }) { Text("CSV") }
                        }
                    }
                },
            )
        },
    ) { pad ->
        LazyColumn(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (loading) item { CircularProgressIndicator() }
            if (!loading && review == null) {
                item {
                    Text("No review yet.")
                    Button(
                        onClick = {
                            scope.launch {
                                reviewing = true
                                review = api.postReview(contractId).getOrNull()
                                reviewing = false
                            }
                        },
                        enabled = !reviewing,
                    ) { Text(if (reviewing) "Reviewing…" else "Start AI review") }
                }
            }
            review?.let { r ->
                item {
                    Text("Overall risk ${r.overallRiskScore}/100 — ${riskHeatLabel(r.overallRiskScore)}")
                    LinearProgressIndicator(
                        progress = { r.overallRiskScore / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                    )
                }
                item { Text(r.summary, style = MaterialTheme.typography.bodyMedium) }
                item { Text("Clauses (${r.clauses.size})", style = MaterialTheme.typography.titleMedium) }
                items(r.clauses) { c ->
                    Card {
                        Column(Modifier.padding(12.dp)) {
                            Text(c.title, fontWeight = FontWeight.Bold)
                            Text("Risk ${c.riskScore} — ${c.riskReason}", style = MaterialTheme.typography.bodySmall)
                            Text(c.content, style = MaterialTheme.typography.bodySmall, maxLines = 6)
                            c.suggestedRevision?.let { rev ->
                                Text("Suggested: $rev", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                if (r.complianceIssues.isNotEmpty()) {
                    item { Text("Compliance", style = MaterialTheme.typography.titleMedium) }
                    items(r.complianceIssues) { i ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))) {
                            Column(Modifier.padding(12.dp)) {
                                Text(i.issue, fontWeight = FontWeight.Bold)
                                Text("${i.severity} · ${i.clause}", style = MaterialTheme.typography.labelSmall)
                                Text(i.recommendation, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NegotiateScreen(api: ClauseVaultApi, nav: NavHostController) {
    var contracts by remember { mutableStateOf<List<ContractRow>>(emptyList()) }
    var selected by remember { mutableStateOf("") }
    var agg by remember { mutableStateOf("moderate") }
    var options by remember { mutableStateOf<List<NegotiationOptionDto>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var tab by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        contracts = api.getContracts().getOrNull()?.contracts?.filter { it.status == "reviewed" }.orEmpty()
        if (contracts.isNotEmpty()) selected = contracts.first().id
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Negotiate") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (contracts.isEmpty()) {
                Text("Upload and review a contract first.")
                return@Column
            }
            var exp by remember { mutableStateOf(false) }
            Text("Contract", style = MaterialTheme.typography.labelMedium)
            Box(Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { exp = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(contracts.find { it.id == selected }?.title ?: "Select", maxLines = 2)
                }
                DropdownMenu(expanded = exp, onDismissRequest = { exp = false }) {
                    contracts.forEach { c ->
                        DropdownMenuItem(
                            text = { Text(c.title, maxLines = 2) },
                            onClick = { selected = c.id; exp = false },
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("conservative", "moderate", "aggressive").forEach { a ->
                    FilterChip(a == agg, { agg = a }, { Text(a) })
                }
            }
            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        options = api.negotiate(selected, agg).getOrNull()?.options.orEmpty()
                        tab = 0
                        busy = false
                    }
                },
                enabled = !busy && selected.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (busy) "Working…" else "Generate options") }
            if (options.isNotEmpty()) {
                ScrollableTabRow(options, tab) { tab = it }
                val opt = options.getOrNull(tab) ?: return@Column
                Card {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(opt.label, style = MaterialTheme.typography.titleMedium)
                        Text(opt.description, style = MaterialTheme.typography.bodySmall)
                        Text("Acceptance ~${opt.acceptanceProbability}% · Risk Δ ${opt.riskDelta}")
                        opt.changes.forEach { ch ->
                            HorizontalDivider()
                            Text(ch.clause, fontWeight = FontWeight.Medium)
                            Text("Was: ${ch.original}", style = MaterialTheme.typography.bodySmall, maxLines = 4)
                            Text("Proposed: ${ch.proposed}", style = MaterialTheme.typography.bodySmall, maxLines = 4)
                            Text(ch.reasoning, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScrollableTabRow(
    options: List<NegotiationOptionDto>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEachIndexed { i, o ->
            FilterChip(i == selected, { onSelect(i) }, { Text(o.label, maxLines = 1) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybooksScreen(api: ClauseVaultApi, nav: NavHostController) {
    var list by remember { mutableStateOf<List<PlaybookRow>>(emptyList()) }
    var showEditor by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf("other") }
    var rules by remember { mutableStateOf("[]") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch { list = api.playbooks().getOrNull()?.playbooks.orEmpty() }
    }

    LaunchedEffect(Unit) { reload() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Playbooks") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
            )
        },
    ) { pad ->
        LazyColumn(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Button(onClick = { showEditor = true; name = ""; desc = ""; rules = "[]" }, modifier = Modifier.fillMaxWidth()) {
                    Text("New playbook")
                }
            }
            if (showEditor) {
                item {
                    Card {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(desc, { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(cat, { cat = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(rules, { rules = it }, label = { Text("Rules JSON array") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            busy = true
                                            api.createPlaybook(name, desc, cat, rules).onSuccess { showEditor = false; reload() }
                                            busy = false
                                        }
                                    },
                                    enabled = !busy && name.isNotBlank(),
                                ) { Text("Save") }
                                OutlinedButton(onClick = { showEditor = false }) { Text("Cancel") }
                            }
                        }
                    }
                }
            }
            items(list) { p ->
                Card {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(p.name, fontWeight = FontWeight.Bold)
                                Text(p.category, style = MaterialTheme.typography.labelSmall)
                                Text(p.description, style = MaterialTheme.typography.bodySmall, maxLines = 3)
                            }
                            if (p.isCustom) {
                                TextButton(onClick = {
                                    scope.launch {
                                        api.deletePlaybook(p.id).onSuccess { reload() }
                                    }
                                }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(api: ClauseVaultApi, nav: NavHostController) {
    var provider by remember { mutableStateOf("gemini") }
    var model by remember { mutableStateOf("gemini-2.5-flash") }
    var ollama by remember { mutableStateOf("http://localhost:11434") }
    var anthropic by remember { mutableStateOf("") }
    var plan by remember { mutableStateOf("starter") }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        loading = true
        api.settings().onSuccess {
            provider = it.ai_provider
            model = it.model
            ollama = it.ollama_url
            anthropic = it.anthropic_key
            plan = it.currentPlan
        }
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
            )
        },
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (loading) CircularProgressIndicator()
            Text("Plan: $plan", style = MaterialTheme.typography.labelMedium)
            var exp by remember { mutableStateOf(false) }
            Text("AI provider", style = MaterialTheme.typography.labelMedium)
            Box(Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { exp = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(provider)
                }
                DropdownMenu(expanded = exp, onDismissRequest = { exp = false }) {
                    listOf("gemini", "ollama", "anthropic").forEach { p ->
                        DropdownMenuItem(
                            text = { Text(p) },
                            onClick = { provider = p; exp = false },
                        )
                    }
                }
            }
            OutlinedTextField(model, { model = it }, label = { Text("Model id") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(ollama, { ollama = it }, label = { Text("Ollama URL") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                anthropic,
                { anthropic = it },
                label = { Text("Anthropic key") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        api.saveSettings(SettingsPutBody(provider, model, ollama, anthropic))
                        busy = false
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(api: ClauseVaultApi, nav: NavHostController) {
    var billing by remember { mutableStateOf<BillingResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun load() {
        scope.launch {
            loading = true
            billing = api.billing().getOrNull()
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Billing") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (loading) CircularProgressIndicator()
            billing?.let { b ->
                Text("Current plan: ${b.currentPlan}")
                Text("Reviews used: ${b.usage.reviewsUsed} · Generations: ${b.usage.generationsUsed}")
                b.plans.forEach { p ->
                    Card {
                        Column(Modifier.padding(12.dp)) {
                            Text(p.name, style = MaterialTheme.typography.titleMedium)
                            Text("$${p.price}/mo")
                            p.features.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                            if (p.id != "starter" && p.id != b.currentPlan) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            busy = p.id
                                            api.billingCheckout(p.id).onSuccess { openUrl(context, it) }
                                            busy = null
                                        }
                                    },
                                    enabled = busy == null,
                                ) { Text("Upgrade") }
                            }
                        }
                    }
                }
                if (b.canManageBilling) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                api.billingPortal().onSuccess { openUrl(context, it) }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Manage billing (Stripe)") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeployScreen(api: ClauseVaultApi, nav: NavHostController) {
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Deploy on-prem") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Download the Docker deployment package (same as web).")
            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        api.deployZip().onSuccess {
                            shareExportBytes(context, it, "clausevault-onprem.zip", "application/zip")
                        }
                        busy = false
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (busy) "Downloading…" else "Download ZIP") }
            Text(
                "Security: data sovereignty, zero telemetry, air-gap ready — mirrors the web Deploy page.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
