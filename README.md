# ClauseVault — Android app

Native **Jetpack Compose** client for [ClauseVault](https://github.com/topics/clausevault) (contract upload, AI review, generate, negotiate, playbooks, analytics, billing, on-prem deploy package).

## Download

- **Source:** clone this repository.
- **Debug APK (no install from Play):** open **Actions** → latest workflow run → **Artifacts** → download `app-debug` (ZIP contains `app-debug.apk`). Enable on device: *Settings → Security → Install unknown apps* for your browser/files app.

## Configure

1. Copy `local.properties.example` to `local.properties`.
2. Set `clausevault.apiUrl` (your Next.js app URL, no trailing slash), `supabase.url`, and `supabase.anonKey`.

## Build locally

- **JDK 17** + [Android Studio](https://developer.android.com/studio) (or Android SDK + `./gradlew assembleDebug`).

## Backend

The API must accept `Authorization: Bearer <Supabase access_token>` (supported in the ClauseVault Next.js API routes).
