package cloud.clausevault.app.ui

object Routes {
    const val Login = "login"
    const val SignUp = "signup"
    const val TabDashboard = "tab_dashboard"
    const val TabUpload = "tab_upload"
    const val TabGenerate = "tab_generate"
    const val TabAnalytics = "tab_analytics"
    const val TabMore = "tab_more"
    const val Review = "review/{contractId}"
    const val Negotiate = "negotiate"
    const val Playbooks = "playbooks"
    const val Settings = "settings"
    const val Billing = "billing"
    const val Deploy = "deploy"

    fun review(contractId: String) = "review/$contractId"
}
