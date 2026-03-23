package cloud.clausevault.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import cloud.clausevault.app.ui.AppNavHost
import cloud.clausevault.app.ui.ClauseVaultTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as ClauseVaultApp
        setContent {
            ClauseVaultTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavHost(api = app.api, auth = app.auth, tokens = app.tokens)
                }
            }
        }
    }
}
