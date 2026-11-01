# Nounish Launcher

A calm, distraction-free, text-only Android launcher designed for pure focus and AMOLED battery efficiency. No icons, no widgets, no ads, no telemetry, and offline by default.

---

## ✨ Features

- **Text-Only Design**: Apps are displayed solely by name. No visual noise, no notification badges, no app icon dopamine triggers.
- **AMOLED Monochrome**: Pure `#000000` background with crisp white and subtle grey typography. Optional light theme.
- **Home Screen**:
  - Live clock and date (configurable).
  - Pinned apps (4–8 items) in large (28 sp) light-weight text, left-aligned for ergonomic thumb reach.
  - Quick action sheet to unpin or open app settings on long-press.
- **App Drawer**:
  - Swipe up from Home to open instant search.
  - Zero icon loading: uses bare `PackageManager` labels for instant load times and negligible RAM usage.
  - Alphabetical app list with realtime filtering.
  - **Auto-launch**: Optional setting to automatically launch an app if search yields exactly 1 match.
  - Long-press options: Pin to Home, Hide, App Info, Uninstall.
- **Hidden Apps**:
  - Hide distracting apps from the drawer list.
  - Hidden apps only appear if searched for by name or in Settings -> Hidden Apps.
- **Gestures**:
  - **Swipe up**: Open drawer
  - **Swipe down**: Close drawer
  - **Swipe left**: Launch assigned quick app
  - **Swipe right**: Launch assigned quick app
  - **Double-tap**: Screen lock (via lightweight Accessibility Service, API 28+)
  - **Long-press**: Open Settings
- **Crypto Price Label (Optional & Off by Default)**:
  - Small text in the top-right corner of the app list (e.g. `BTC 67,420`).
  - Fetches from Binance public API only when the drawer is open (polls every 15s, stops immediately when drawer closes).
  - Tap the price label to cycle through configured coins (default: `BTC`, `ETH`, `SOL`).
  - Cached in DataStore for immediate display even when offline. Stale data is subtly dimmed.
  - Zero network usage when disabled.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 1.9.24
- **UI Toolkit**: Jetpack Compose (Material3)
- **Minimum SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Architecture**: Single Activity (`MainActivity`) + `MainViewModel` (MVI/MVVM pattern)
- **Storage**: Jetpack DataStore Preferences
- **Networking**: Lightweight `HttpURLConnection` with fallback Binance API endpoints (no bulky external networking dependencies)
- **APK Footprint**: ~1.0 MB optimized release APK with R8 minification and resource shrinking

---

## 📂 Project Structure

```
nounishlauncher/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/nounish/launcher/
│       │   ├── MainActivity.kt           // SingleTask launcher host & screen transitions
│       │   ├── MainViewModel.kt          // Reactive state coordination
│       │   ├── model/
│       │   │   └── AppItem.kt            // App label and component info
│       │   ├── data/
│       │   │   ├── AppRepository.kt      // PackageManager loader & intent launcher
│       │   │   ├── SettingsStore.kt      // Jetpack DataStore Preferences wrapper
│       │   │   └── PriceRepository.kt    // Public Binance price fetcher + cache
│       │   ├── ui/
│       │   │   ├── HomeScreen.kt         // Pinned apps, clock/date
│       │   │   ├── DrawerScreen.kt       // Fast text search & crypto price ticker
│       │   │   ├── SettingsScreen.kt     // Configuration & gesture mappings
│       │   │   ├── GestureModifier.kt    // Swipe & tap gesture detection
│       │   │   └── theme/
│       │   │       ├── Color.kt          // Monochrome & AMOLED palette
│       │   │       ├── Type.kt           // Typography specifications
│       │   │       └── Theme.kt          // Dynamic Compose theme provider
│       │   └── service/
│       │       ├── LockScreenService.kt  // Accessibility lock action
│       │       └── PackageChangeReceiver.kt // Dynamic install/uninstall reload
│       └── res/
│           ├── mipmap-anydpi-v26/        // Adaptive monochrome vector icon
│           ├── values/                   // Strings and window themes
│           └── xml/                      // Accessibility service configuration
├── build.gradle.kts
├── settings.gradle.kts
└── plan.md
```

---

## 🚀 Building & Running

### Prerequisites
- JDK 17 or JDK 21 (e.g. from Android Studio JBR: `C:\Program Files\Android\Android Studio\jbr`)
- Android SDK Platform 34 & Build-Tools

### Build Debug APK
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### Build Optimized Release APK
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleRelease
```
Output: `app/build/outputs/apk/release/app-release.apk` (~1 MB)

---

## 🔒 Privacy & Permissions

- **INTERNET**: Used solely for the optional crypto price label from Binance. Completely inert when the feature is toggled off in Settings.
- **QUERY_ALL_PACKAGES / CATEGORY_LAUNCHER**: Used strictly on-device to query installed launcher apps to populate the drawer.
- **Accessibility Service (Optional)**: Used only if "Double tap to lock screen" is toggled on to execute `GLOBAL_ACTION_LOCK_SCREEN`.
- **Zero tracking, zero analytics, zero external logging.**
