import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.reader()?.use(::load)
}

// Same public client targets as https://clausevault.cloud (NEXT_PUBLIC_* + Supabase anon in web bundle).
// Forks / self-host: override via local.properties — see local.properties.example.
val productionApiUrl = "https://clausevault.cloud"
val productionSupabaseUrl = "https://sqobcvynhuzkttskjwfv.supabase.co"
val productionSupabaseAnonKey =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNxb2JjdnluaHV6a3R0c2tqd2Z2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzM5MDEyMDMsImV4cCI6MjA4OTQ3NzIwM30.kul1Mz7grdzA66kojO2Z_Lt4wuY3XpCs6CkMJ-XW9FI"

val clausevaultApiUrl: String =
    localProps.getProperty("clausevault.apiUrl") ?: productionApiUrl
val supabaseUrl: String =
    localProps.getProperty("supabase.url") ?: productionSupabaseUrl
val supabaseAnonKey: String =
    localProps.getProperty("supabase.anonKey") ?: productionSupabaseAnonKey

android {
    namespace = "cloud.clausevault.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "cloud.clausevault.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.1"
        buildConfigField("String", "CLAUSEVAULT_API_URL", "\"${clausevaultApiUrl.trimEnd('/')}\"")
        buildConfigField("String", "SUPABASE_URL", "\"${supabaseUrl.trimEnd('/')}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${supabaseAnonKey}\"")
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            // Installable release APK without a private keystore in CI (sideload / direct download).
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
