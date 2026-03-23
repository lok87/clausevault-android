package cloud.clausevault.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cloud.clausevault.app.BuildConfig
import cloud.clausevault.app.data.ClauseVaultApi
import cloud.clausevault.app.data.SupabaseAuthRepository
import cloud.clausevault.app.data.TokenStore
import kotlinx.coroutines.launch

private data class TabItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val Tabs = listOf(
    TabItem(Routes.TabDashboard, "Home", Icons.Default.Dashboard),
    TabItem(Routes.TabUpload, "Upload", Icons.Default.FileUpload),
    TabItem(Routes.TabGenerate, "Generate", Icons.Default.NoteAdd),
    TabItem(Routes.TabAnalytics, "Analytics", Icons.Default.Analytics),
    TabItem(Routes.TabMore, "More", Icons.Default.MoreHoriz),
)

@Composable
fun AppNavHost(api: ClauseVaultApi, auth: SupabaseAuthRepository, tokens: TokenStore) {
    var loggedIn by remember { mutableStateOf(tokens.hasSession()) }
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val navBackStackEntry by nav.currentBackStackEntryAsState()
    val route = navBackStackEntry?.destination?.route
    val showBottomBar = loggedIn && route in Tabs.map { it.route }

    fun showError(msg: String) {
        scope.launch { snackbar.showSnackbar(msg) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (!showBottomBar) return@Scaffold
            NavigationBar {
                val current = route ?: Routes.TabDashboard
                Tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = current == tab.route,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            key(loggedIn) {
                NavHost(
                    navController = nav,
                    startDestination = if (loggedIn) Routes.TabDashboard else Routes.Login,
                ) {
                    composable(Routes.Login) {
                        LoginScreen(
                            auth = auth,
                            appBaseUrl = BuildConfig.CLAUSEVAULT_API_URL,
                            onSignedIn = { loggedIn = true },
                            onNavigateSignUp = { nav.navigate(Routes.SignUp) },
                            onError = ::showError,
                        )
                    }
                    composable(Routes.SignUp) {
                        SignUpScreen(
                            auth = auth,
                            onDone = { loggedIn = true },
                            onBack = { nav.popBackStack() },
                            onError = ::showError,
                        )
                    }
                    composable(Routes.TabDashboard) { DashboardScreen(api, nav) }
                    composable(Routes.TabUpload) { UploadScreen(api) }
                    composable(Routes.TabGenerate) { GenerateScreen(api) }
                    composable(Routes.TabAnalytics) { AnalyticsScreen(api) }
                    composable(Routes.TabMore) {
                        MoreScreen(nav, auth) { loggedIn = false }
                    }
                    composable(
                        Routes.Review,
                        arguments = listOf(navArgument("contractId") { type = NavType.StringType }),
                    ) { entry ->
                        val id = entry.arguments?.getString("contractId").orEmpty()
                        ReviewScreen(id, api, nav)
                    }
                    composable(Routes.Negotiate) { NegotiateScreen(api, nav) }
                    composable(Routes.Playbooks) { PlaybooksScreen(api, nav) }
                    composable(Routes.Settings) { SettingsScreen(api, nav) }
                    composable(Routes.Billing) { BillingScreen(api, nav) }
                    composable(Routes.Deploy) { DeployScreen(api, nav) }
                }
            }
        }
    }
}
