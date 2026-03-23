package cloud.clausevault.app

import android.app.Application
import cloud.clausevault.app.data.ClauseVaultApi
import cloud.clausevault.app.data.SupabaseAuthRepository
import cloud.clausevault.app.data.TokenStore

class ClauseVaultApp : Application() {
    lateinit var tokens: TokenStore
        private set
    lateinit var auth: SupabaseAuthRepository
        private set
    lateinit var api: ClauseVaultApi
        private set

    override fun onCreate() {
        super.onCreate()
        tokens = TokenStore(this)
        auth = SupabaseAuthRepository(
            BuildConfig.SUPABASE_URL,
            BuildConfig.SUPABASE_ANON_KEY,
            tokens,
        )
        api = ClauseVaultApi(BuildConfig.CLAUSEVAULT_API_URL, tokens, auth)
    }
}
