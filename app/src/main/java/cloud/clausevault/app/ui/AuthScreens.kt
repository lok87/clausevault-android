package cloud.clausevault.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cloud.clausevault.app.data.SupabaseAuthRepository
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    auth: SupabaseAuthRepository,
    appBaseUrl: String,
    onSignedIn: () -> Unit,
    onNavigateSignUp: () -> Unit,
    onError: (String) -> Unit,
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var magicSent by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (magicSent) {
        Card(
            modifier = Modifier.padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Check your email", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(
                    "We sent a magic link to $email. Open it on this device or sign in with your password.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = { magicSent = false }) { Text("Back") }
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("ClauseVault", style = MaterialTheme.typography.headlineMedium)
        Text("Sign in", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Password", style = MaterialTheme.typography.labelLarge)
            TextButton(onClick = { openUrl(context, "$appBaseUrl/forgot-password") }) {
                Text("Forgot?", style = MaterialTheme.typography.labelSmall)
            }
        }
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                scope.launch {
                    loading = true
                    auth.signInWithPassword(email, password)
                        .onSuccess { onSignedIn() }
                        .onFailure { onError(it.message ?: "Sign in failed") }
                    loading = false
                }
            },
            enabled = !loading && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (loading) "Signing in…" else "Sign in")
        }
        OutlinedButton(
            onClick = {
                if (email.isBlank()) {
                    onError("Enter your email first")
                    return@OutlinedButton
                }
                scope.launch {
                    loading = true
                    auth.sendMagicLink(email)
                        .onSuccess { magicSent = true }
                        .onFailure { onError(it.message ?: "Could not send link") }
                    loading = false
                }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Send magic link") }
        TextButton(onClick = onNavigateSignUp, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Create account")
        }
    }
}

@Composable
fun SignUpScreen(
    auth: SupabaseAuthRepository,
    onDone: () -> Unit,
    onBack: () -> Unit,
    onError: (String) -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var verifyEmail by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (verifyEmail) {
        Card(Modifier.padding(16.dp)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Check your email", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text("Confirm your address to finish signup.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onBack) { Text("Back to sign in") }
            }
        }
        return
    }

    Column(
        Modifier
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Create account", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password (8+ chars)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = confirm,
            onValueChange = { confirm = it },
            label = { Text("Confirm password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                if (password.length < 8) {
                    onError("Password must be at least 8 characters")
                    return@Button
                }
                if (password != confirm) {
                    onError("Passwords do not match")
                    return@Button
                }
                scope.launch {
                    loading = true
                    auth.signUp(email, password)
                        .onSuccess { hasSession ->
                            if (hasSession) onDone() else verifyEmail = true
                        }
                        .onFailure { onError(it.message ?: "Sign up failed") }
                    loading = false
                }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (loading) "Creating…" else "Sign up") }
        TextButton(onClick = onBack) { Text("Back to sign in") }
    }
}
