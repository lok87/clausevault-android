# ClauseVault — Android app

Native **Jetpack Compose** client for [ClauseVault](https://github.com/topics/clausevault) (contract upload, AI review, generate, negotiate, playbooks, analytics, billing, on-prem deploy package).

## Download

- **Source:** `git clone https://github.com/lok87/clausevault-android.git`
- **Debug APK:** build with `./gradlew assembleDebug` (output: `app/build/outputs/apk/debug/app-debug.apk`), or add the included `.github/workflows/android-build.yml` in a follow-up commit after granting GitHub token **workflow** scope (`gh auth refresh -s workflow`) so Actions can produce an artifact.

## Configure

1. Copy `local.properties.example` to `local.properties`.
2. Set `clausevault.apiUrl` (your Next.js app URL, no trailing slash), `supabase.url`, and `supabase.anonKey`.

## Build locally

- **JDK 17** + [Android Studio](https://developer.android.com/studio) (or Android SDK + `./gradlew assembleDebug`).

## Backend

The API must accept `Authorization: Bearer <Supabase access_token>` (supported in the ClauseVault Next.js API routes).
