# ClauseVault — Android

Installable **APK** for [ClauseVault](https://clausevault.cloud/) — same cloud backend as the website (upload, AI review, generate, negotiate, playbooks, analytics, billing, on‑prem package download).

## Download (no setup)

1. Open **[Actions](https://github.com/lok87/clausevault-android/actions)** → select the latest successful **“Build release APK”** run.
2. Under **Artifacts**, download **ClauseVault-apk** (ZIP) and extract **`ClauseVault.apk`**.
3. On your phone: allow install from your file manager / browser if prompted (“unknown sources”), then open the APK.

No `local.properties`, env files, or API keys — production URL and Supabase **anon** client (same as the public website) are embedded at build time.

Sign in with **email + password** like on the web. Magic links still complete in the **browser** (same as clicking the email link on desktop); use password in the app, or finish link in browser then use the app after signing in on web if needed.

Billing / Stripe opens the **browser** (same as the web app).

## Self‑host / fork

To point the app at **your** deployment, copy `local.properties.example` → `local.properties` and set `clausevault.apiUrl`, `supabase.url`, and `supabase.anonKey`, then rebuild (`./gradlew assembleRelease`).

## Build locally

JDK 17 + Android SDK (or Android Studio): `./gradlew assembleRelease` → `app/build/outputs/apk/release/app-release.apk`.

## Backend

Uses `Authorization: Bearer <Supabase access_token>` against your ClauseVault Next.js API (supported on the server routes).
