# Crumia

A fast, lightweight, privacy-first Android web browser powered by **Android System WebView (Chromium)**, **Jetpack Compose**, **Material Design 3**, **Room**, and **DataStore**.

## Key Features

- **Smart Omnibox & Search**: Automatic URL vs. search query detection, HTTPS auto-upgrade, live autocomplete merged with local bookmarks & history, voice search, copy/paste, and support for Google, Bing, DuckDuckGo, Yahoo, and Custom search engines.
- **Crumia Ad Shield**: Built-in network request blocker and cosmetic element hider modeled on **EasyList**, **EasyPrivacy**, and **AdGuard Mobile**, with real blocked counters, per-site allowlist, custom block rules, and manual remote list updates.
- **Multi-Tab Management**: Visual tab grid with previews, background tab pausing to minimize RAM/CPU, tab duplication, recently closed tab restoration, close-all, and strict separation between normal and private tabs.
- **Private (Incognito) Mode**: Does not record browsing history, saved session tabs, or closed tabs, and wipes cookies and WebStorage when private sessions end.
- **Bookmarks & Offline Reading List**: Folder organization, search, HTML bookmark import/export, and offline WebArchive (`.mht`) saving.
- **Download Manager**: Progress tracking, pause/resume, cancel/retry, notifications, and FileProvider open/share support.
- **Media & Desktop Tools**: HTML5 fullscreen video, Picture-in-Picture (PiP), Find in Page, Print/Save PDF, Translate Page, Desktop Mode per site, and explicit permission prompts for Camera, Microphone, Location, and File Uploads.
- **Zero Monetization**: No ads, no subscriptions, no analytics SDKs, and no mandatory cloud accounts.

## Technical Notes & WebView Limitations

- **Ad Blocking**: Android System WebView intercepts subresource HTTP/HTTPS requests via `shouldInterceptRequest` and applies lightweight cosmetic CSS. First-party in-stream ads or encrypted proprietary ad payloads cannot be guaranteed 100% blocked at the WebView layer.
- **Private Mode**: Crumia isolates history and wipes cookies/WebStorage on session close, while noting that Android System WebView shares one underlying Chromium profile in-process.

## Building Locally

1. Open the project in **Android Studio** (JDK 11+, Android SDK 36), or install Gradle 9.3.1.
2. Run unit & Robolectric tests:
   ```bash
   gradle :app:testDebugUnitTest
   ```
3. Build Debug or Release APK:
   ```bash
   gradle :app:assembleDebug
   gradle :app:assembleRelease
   ```

## Signing Keys & GitHub APK Releases

On every push to `main` (or manual **Run workflow** in GitHub Actions), **Crumia Android CI & Release** builds and uploads `crumia-debug-apk` as a downloadable artifact under the **Actions** tab—no signing secrets required.

To also publish signed Release APKs, keep signing keys and passwords **out of version control**:

1. Generate an upload keystore locally:
   ```bash
   keytool -genkey -v -keystore my-upload-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
   ```
2. Base64-encode the keystore and add these repository secrets in **GitHub -> Settings -> Secrets and variables -> Actions**:
   - `ANDROID_KEYSTORE_BASE64`
   - `STORE_PASSWORD`
   - `KEY_PASSWORD`
3. Tag a release (e.g., `git tag v1.0.0 && git push origin v1.0.0`) to trigger GitHub Actions to build, test, and attach the APK(s) to **GitHub Releases**.
