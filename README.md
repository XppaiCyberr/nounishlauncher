# Nounish Launcher

A calm, distraction-free, text-only Android launcher designed for pure focus and AMOLED battery efficiency. No icons, no widgets, no ads, no telemetry, and offline by default.

---

## ✨ Features

- **Text-Only Design**: Apps are displayed solely by name. No visual noise, no notification badges, and zero icon bitmaps loaded into memory.
- **AMOLED Monochrome**: Pitch black background (`#000000`) with crisp white and subtle grey typography. Includes an optional light theme.
- **Home Screen**:
  - Live clock and date (configurable, tap clock to open system clock).
  - **Live Binance Crypto Market Overview**: Fills the spacious center area with real-time tickers (`BTC`, `ETH`, `SOL`), price, and 24-hour percentage change.
  - **Expandable 24h High/Low**: Tapping any coin row reveals its 24-hour range (`H: 78,068 · L: 77,262`).
  - Pinned apps (4–8 items) in large (`28 sp`) light-weight text, left-aligned for ergonomic one-handed thumb reach.
  - Quick action dialog to unpin or open app settings on long-press.
  - Long-press background opens Settings.
- **App Drawer**:
  - Swipe up from Home to open instant search.
  - Zero icon loading: uses direct `PackageManager` labels for sub-millisecond load times and minimal RAM usage.
  - Real-time search filter with optional **Auto-launch** on single match.
  - **Alphabetical Fast-Scroll Index**: Vertical `A–Z` + `#` index scrubber with tactile haptic feedback.
  - **Customizable Position**: Option to place the index on either the **Left** or **Right** edge.
  - **Niagara-Style Fluid Wave Animation**: When scrubbing the alphabet index, apps smoothly curve and wave towards your thumb with organic spring physics at 120 FPS.
  - **Animation Toggle**: Option to disable the wave animation for a completely static, zero-motion text list.
  - Long-press menu on any app: Pin to Home, Hide, App Info, Uninstall.
- **Hidden Apps**:
  - Hide distracting apps from the drawer list.
  - Hidden apps only appear if searched for by name or in Settings &rarr; Hidden Apps.
- **Gestures**:
  - **Swipe up**: Open drawer
  - **Swipe down**: Close drawer
  - **Swipe left / right**: Launch user-assigned quick apps
  - **Double-tap**: Screen lock
  - **Long-press**: Open Settings
- **Crypto Price Integration (Binance Public API)**:
  - Small header ticker in the top-right corner of the drawer (tap to cycle coins).
  - Color-coded daily percentage change: **Green** (`#00E676`) for positive, **Red** (`#FF5252`) for negative.
  - Fetches from Binance public API (`data-api.binance.vision` with mirror fallbacks).
  - Zero background battery drain: only polls while the Home screen or Drawer is active; stops immediately when locked or in another app.
  - DataStore caching for instant offline viewing.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 1.9.24
- **UI Toolkit**: Jetpack Compose (Material3)
- **Minimum SDK**: 26 (Android 8.0 Oreo)
- **Target SDK**: 34 (Android 14)
- **Architecture**: Single Activity (`MainActivity`) + `MainViewModel` (MVI / MVVM pattern)
- **Storage**: Jetpack DataStore Preferences
- **Networking**: Native `HttpURLConnection` with fallback Binance API endpoints (no heavy third-party networking libraries)
- **Footprint**: **~1.04 MB** optimized release APK with R8 minification and resource shrinking

---

## 📂 Project Structure

```
nounishlauncher/
├── app/
│   ├── build.gradle.kts                         # Android build config, Compose BOM, R8 shrinking
│   ├── proguard-rules.pro                       # R8 optimization rules
│   └── src/main/
│       ├── AndroidManifest.xml                  # HOME launcher declaration, package queries
│       ├── java/com/nounish/launcher/
│       │   ├── MainActivity.kt                  # SingleTask launcher activity, back handling & transitions
│       │   ├── MainViewModel.kt                 # StateFlow coordination (apps, search, settings, crypto)
│       │   ├── model/
│       │   │   └── AppItem.kt                   # App label, packageName, and activityName
│       │   ├── data/
│       │   │   ├── AppRepository.kt             # PackageManager loader (labels only, no icons)
│       │   │   ├── SettingsStore.kt             # Jetpack DataStore wrapper for all preferences
│       │   │   └── PriceRepository.kt           # Binance 24hr ticker client with failover mirrors
│       │   ├── ui/
│       │   │   ├── HomeScreen.kt                # Clock, date, Binance market overview, pinned apps
│       │   │   ├── DrawerScreen.kt              # Search bar, left/right alphabet scrubber, Niagara wave list
│       │   │   ├── SettingsScreen.kt            # Settings & customization controls
│       │   │   ├── GestureModifier.kt           # Gesture detection for swipes, taps, and long-presses
│       │   │   └── theme/
│       │   │       ├── Color.kt                 # AMOLED pitch black, light theme, crypto green/red
│       │   │       ├── Type.kt                  # Typography scale (28sp home, 18sp drawer, 14sp ticker)
│       │   │       └── Theme.kt                 # Dynamic Compose theme provider
│       │   └── service/
│       │       ├── LockScreenService.kt         # Screen lock action
│       │       └── PackageChangeReceiver.kt    # BroadcastReceiver for app install/uninstall updates
│       └── res/
│           ├── drawable/                        # Vector assets for launcher icon
│           ├── mipmap-anydpi-v26/               # Adaptive launcher icons
│           ├── values/                          # Strings and window themes (#000000 background)
│           └── values-v28/                      # Display cutout support
├── build.gradle.kts                             # Root Gradle build script
├── settings.gradle.kts                          # Module inclusion & repository management
├── gradle.properties                            # JVM memory allocation & AndroidX settings
├── PROJECT_OVERVIEW.md                          # In-depth architectural & technical documentation
└── plan.md                                      # Original roadmap and design spec
```

---

## 🚀 Building & Running

### Prerequisites
- JDK 17 or JDK 21 (e.g. from Android Studio JBR: `C:\Program Files\Android\Android Studio\jbr`)
- Android SDK Platform 34 & Build-Tools 34.0.0

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
Output: `app/build/outputs/apk/release/app-release.apk` (**~1.04 MB**)

---

## 🔒 Privacy & Permissions

- **INTERNET**: Used solely for the public Binance market ticker. Completely inert when crypto features are toggled off in Settings.
- **QUERY_ALL_PACKAGES / CATEGORY_LAUNCHER**: Used strictly on-device to query installed apps for the drawer.
- **Zero tracking, zero analytics, zero external logging.**
