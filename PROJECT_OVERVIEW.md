# Nounish Launcher — Project Overview & Documentation

Nounish Launcher is a calm, distraction-free, text-only Android launcher built with **Kotlin** and **Jetpack Compose**. It strips away app icons, badges, notifications, and unnecessary animations to provide a focused, AMOLED-optimized interface that saves battery and eliminates dopamine triggers.

---

## Design Philosophy

1. **Minimalist App Icons & Pure Text Mode**: Toggleable option to display subtle, minimalist app icons beside each app with an optional monochrome/grayscale filter that preserves the calm AMOLED aesthetic, or toggle off completely for a pure text-only experience.
2. **AMOLED Monochrome & Custom Typography**: Pitch black background (`#000000`) paired with crisp white and subtle grey typography. An optional light theme, monospace mode, and fine-grained font size scaling (Micro 65%, Extra Small 75%, Small 85%, Compact 92%, Default 100%, Large 115%, Extra Large 130%) are also available, alongside compact, clean settings menu margins.
3. **Calm & Distraction-Free**: No notification dots, news feeds, unread counters, or recommendation algorithms.
4. **Privacy-Focused & Offline by Default**: Zero analytics, telemetry, or tracking. The only network feature is the optional crypto ticker using Binance's public API (disabled by default, with zero background services).
5. **Instantaneous & Lightweight**: App cold starts in under 300 ms with an optimized APK footprint of only **~1.05 MB**. Pinned shortcuts render on frame 1 from DataStore without waiting for `PackageManager`.
6. **Robust Process-Death Safety**: Full state restoration for active screen, drawer search, scroll offset, and settings across Android system process kills.
7. **Strict Background Discipline**: App lists are cached in memory and refreshed exclusively on package broadcast events. All network polling and WebSocket connections terminate immediately on `onPause()` or when leaving Home.

---

## Comprehensive Feature List

### 1. Home Screen
- **Time & Date Display**:
  - Clean, minimal time and date in the top-left corner (`14 sp`, secondary color).
  - Tapping the clock launches the system's default alarm/clock app.
  - Toggles available to show/hide time and date independently.
  - **Clock Format Options**: Select between System Default (respects Android's 24-hour setting), 24-hour (`HH:mm`), 12-hour with AM/PM (`h:mm a`), or 12-hour without AM/PM (`h:mm`).
  - **Optional Seconds**: Live second display (`:ss`) for atomic precision.
  - **Date Format Presets & Custom Pattern**: Choose from presets (`Full`, `Short`, `Standard with year`, `ISO 8601`, `US style`, `Compact`) or specify any custom `SimpleDateFormat` pattern.
- **Live Binance Crypto Market Overview (Center Space)**:
  - Fills the spacious center area between the clock and pinned shortcuts.
  - Shows real-time tickers for your configured watchlist (e.g. `BTC`, `ETH`, `SOL`, `PEPE`, `BTCUSDT`).
  - Displays coin symbol, current price, and 24-hour daily percentage change.
  - Color-coded percentage changes: **Green** (`#00E676`) for gains, **Red** (`#FF5252`) for losses.
  - **Text Sparklines**: 24-hour financial trends drawn with text block characters (` ▂▃▄▅▆▇█`) that fit seamlessly into the monochrome aesthetic.
  - **Expandable Details**: Tapping any coin row reveals its clean 24-hour high and low (`24h H: 78,068 · L: 77,262`).
  - **Stale-Data Indicator**: Dimmed text styling and age marker (`·now`, `·5m`, `·1h`) appear when offline or when viewing cached data.
  - Toggleable in Settings under **Market Prices -> Show crypto on home screen**.
- **Indonesian Stock Exchange (IDX Saham) Market Overview (Center Space)**:
  - Displays live stock prices and indices from Bursa Efek Indonesia via `https://idx.xpp.wtf/`.
  - Configurable display modes:
    - **Auto Rotate**: Automatically cycles between Market Indices, Top Gainers, Top Losers, and Watchlist with an adjustable rotation timer (default 6 seconds).
    - **Watchlist**: Custom user-defined list of tickers (e.g. `BBCA`, `BBRI`, `BMRI`, `TLKM`, `ASII`, `GOTO`). Supports static cache for 47 major blue-chip stocks and automatic query fallback for other listed equities.
    - **Indices**: Indonesian benchmark market indices (`IHSG`, `LQ45`).
    - **Top Gainers**: Real-time top daily percentage gainers.
    - **Top Losers**: Real-time top daily percentage losers.
  - **Staggered Wave Transition (1 by 1 sliding to the left)**: During category rotations, stock rows smoothly cascade and slide 1 by 1 to the left with an index-based staggered wave animation (at 120 FPS).
  - **Dynamic Category Header Labels**: The section header updates dynamically (`IDX SAHAM | Market Indices`, `IDX SAHAM | Top Gainers`, `IDX SAHAM | Top Losers`, `IDX SAHAM | Watchlist`) and can be tapped at any time to immediately advance to the next category.
  - Tap any stock row to expand clean 24-hour high, low, and traded share volume (`24h H: 10,250 · L: 10,000 · Vol: 78.4M`).
  - Full DataStore caching for instantaneous offline rendering with stale age markers (`·now`, `·5m`, `·1h`).
  - Toggleable in Settings under **Market Prices -> Show IDX stocks on home screen**.
- **Separated Market Views (Maximize Middle Screen Space)**:
  - Option to display both Crypto and IDX Saham simultaneously on the Home screen to maximize the spacious middle area between the clock and pinned shortcuts.
  - Two selectable layout arrangements:
    - **Stacked vertically**: Crypto on top, IDX Saham below with clean subtle section headers (`CRYPTO`, `IDX SAHAM`).
    - **Two columns (Side by side)**: Crypto on the left column, IDX Saham on the right column.
  - Middle market viewport automatically scales to available vertical space without crowding or colliding with the top clock or bottom pinned shortcuts.
  - Toggleable in Settings under **Market Prices -> Separate Crypto & Saham**.
- **Auto-Rotating Market Carousel (Auto-Advance Slideshow)**:
  - When separation is disabled, an auto-rotating carousel alternates smoothly between the Crypto and IDX views.
  - Animation transitions: Horizontal slide with subtle fade (`slide`) or smooth crossfade (`crossfade`).
  - Configurable slideshow rotation interval (5s to 30s, default 7s).
  - Manual interactive tabs: Tap `CRYPTO` or `IDX SAHAM` to switch immediately and pause/resume smoothly.
  - Toggleable in Settings under **Market Prices -> Auto-rotate market carousel**.
- **Pinned App Shortcuts & Limits**:
  - Up to 8 pinned apps positioned in the lower-left area for ergonomic one-handed thumb reach, with zero minimum limit (unpin down to 0 apps freely).
  - **Pinned App Reordering**: In Settings -> Pinned Apps, dedicated up (`↑`) and down (`↓`) controls allow direct reordering of shortcuts without unpinning and re-pinning.
  - **Maximum 8 Limit**: Cleanly enforces a maximum of 8 pinned apps. Attempting to pin a 9th app warns: "Maximum 8 pinned apps reached. Unpin an app first."
  - Large (`28 sp`), light-weight typography with generous vertical spacing.
  - Respects custom labels assigned by the user.
  - **Prunes Dead Entries**: If a pinned app is uninstalled, it is automatically dropped from pinned shortcuts and DataStore, leaving zero dead entries.
  - Long-press any shortcut to rename, unpin, or open system App Info.
- **Blank Area Long-Press**: Opens launcher Settings.

### 2. App Drawer & Search
- **Multi-Activity & Work Profile Support**:
  - Uses Android's official `LauncherApps` API across all user profiles (personal, work profile, secondary users).
  - Work profile apps are identified with a clean `(Work)` label suffix and launched directly into the managed profile via `launcherApps.startMainActivity`.
  - Apps with multiple launcher activities are keyed uniquely as `packageName/activityName`, eliminating duplicate collisions.
- **Accent- & Case-Insensitive Alphabetical Sorting**:
  - Uses Unicode NFD normalization to strip combining marks, sorting diacritics naturally (`Écran` under `E`, `Über` under `U`).
  - Apps starting with numbers or symbols (`1Password`, `7-Zip`, `@voice`) are cleanly sorted under `#` at the bottom of the list.
- **Instant Search**:
  - Swipe up from anywhere on the Home screen to open the app list.
  - Minimalist search input at the top filters apps in real time as you type.
  - Searches through both original app labels and user custom labels.
  - **Auto-Launch (Optional)**: Automatically launches the app when search results narrow down to exactly 1 match.
- **Live USD/IDR Exchange Rate (Top Right)**:
  - Displays the live USD to Indonesian Rupiah exchange rate at the top right of the drawer (e.g. `USD 18,142` with 24h percentage change).
  - Fetched via Binance public market feeds (`USDTIDR` / `USDTBIDR`) with automated fallback to `open.er-api.com` and offline DataStore caching.
  - Interactive: tap with haptic tick to refresh immediately.
  - Stale-data indicator (`·5m`, `·1h`) dims the display when offline or using cached data.
  - Toggleable via Settings under Market Prices -> Show USD/IDR rate in app drawer.
- **Alphabetical Fast-Scroll Index**:
  - Vertical `A–Z` + `#` scrubber for quick navigation.
  - **Configurable Position**: Move the index to either the **Left** or **Right** edge via Settings.
  - Active letters with installed apps are highlighted; empty letters are subtly dimmed.
  - **Tactile Haptic Feedback**: Light haptic tick as your thumb crosses each letter.
  - **Matching App Highlighting & Non-Matching Dimming**: When interacting with or scrubbing the index, all apps starting with the selected letter are highlighted in bold white (`textPrimary`) with full icon opacity, while non-matching apps dim down to soft muted grey (`0.28` alpha) so the list is never an undifferentiated sea of white.
  - **Smart Visibility**: Scrubber automatically hides when typing a search query to give maximum screen width to results.
- **Niagara-Style Fluid Wave Animation**:
  - When scrubbing the alphabet index, visible apps smoothly arc and wave towards your thumb.
  - The targeted app shifts up to **`+90 dp`** towards the index with a **`1.16x`** scale and bold typography, while neighboring items curve along a smooth Hermite curve.
  - Runs at 120 FPS via draw-phase `graphicsLayer` rendering without triggering recompositions.
  - **Thumb-Aligned Auto-Scroll**: Selected apps center around your thumb rather than jumping to the top of the viewport.
  - **Organic Spring Physics**: When released, the list gently springs back to baseline with a physics-based spring animation.
  - **Animation Toggle**: Option in Settings to disable the wave animation for a static, zero-motion list.
- **App Context Menu (Long-Press on any item)**:
  - **Rename App**: Assign a custom alias or label (e.g. "Chrome" -> "web"). Includes a one-tap Reset to default option.
  - **Pin to Home / Unpin from Home**
  - **Hide App / Unhide App**
  - **App Info** (System settings)
  - **Uninstall** (Prompts system uninstallation)

### 3. Hidden Apps
- Hide distracting or rarely used apps from the main drawer list.
- Hidden apps only appear when explicitly typed into the search bar or in the **Hidden Apps** manager in Settings.

### 4. Settings Backup & Restore (JSON)
- **Zero-Telemetry Local Backup**: Export all pinned apps, hidden apps, custom app labels, and settings into an offline JSON file.
- **Storage Access Framework (SAF)**: Integrates seamlessly with Android's system document picker (`CreateDocument` and `OpenDocument`) for safe, permissionless file import and export.
- **Backwards Compatible**: Parses existing and future setting backups gracefully.

### 5. Gestures
- **Swipe Up**: Open App Drawer.
- **Swipe Down**: Close App Drawer.
- **Swipe Left**: Launch user-assigned quick app.
- **Swipe Right**: Launch user-assigned quick app.
- **Double-Tap**: Screen lock trigger.
- **Long-Press**: Open Settings.
- **System Back Button**: Prevents accidentally closing the launcher; returns to Home from Drawer or Settings.

### 6. Crypto Price Ticker & Live Streaming
- **Zero API Key Required**: Queries Binance's public 24-hour ticker endpoint (`https://data-api.binance.vision/api/v3/ticker/24hr` with automatic failover to `api.binance.com` and `api.binance.me`).
- **Custom Coin Watchlist**: Add any coin symbol (e.g. `BTC`, `ETH`, `SOL`, `PEPE`) or full trading pair (e.g. `BTCUSDT`, `ETHBIDR`).
- **Quote Currency Selector**: Supports presets for `USDT`, `USDC`, `EUR`, `BIDR` (IDR), `TRY`, `BRL`, `GBP`, `AUD`, `BTC`, or any custom quote currency.
- **Text Sparklines**: 24-hour candlestick trend data rendered using 8-level text block characters (` ▂▃▄▅▆▇█`).
- **Rate-Limit Handling with Exponential Backoff**: Automatically detects HTTP 429 rate limits or connection failures and doubles delay up to 240 seconds.
- **Mobile Data & Battery Saver Awareness**: Extends polling intervals from 15s to 60s when on cellular data or power save mode (90s if both) to preserve battery and cellular data.
- **Optional Live WebSocket on Home**: Built-in zero-dependency RFC 6455 WebSocket client connecting to `data-stream.binance.vision:443`. Streams real-time price updates tick-by-tick directly to the Home screen. Closed immediately on pause, screen lock, or when navigating away.
- **Stale-Data Indicator**: When offline or if API calls fail, cached prices are clearly marked with their age (`·now`, `·5m`, `·1h`) and dimmed.

---

## Technical Stack & Architecture

| Component | Choice | Details |
|---|---|---|
| **Language** | Kotlin 1.9.24 | Idiomatic, coroutine-based asynchronous processing |
| **UI Framework** | Jetpack Compose (Material3) | Declarative UI, 120 FPS hardware-accelerated animations |
| **Target / Min SDK** | Target SDK 34 / Min SDK 26 | Android 8.0 (Oreo) through Android 14+ |
| **Architecture** | Single Activity + MVI / MVVM | `MainActivity` hosting screens via fast crossfades (`< 120 ms`) |
| **State Management** | Kotlin Coroutines & `StateFlow` | Reactive UI binding without boilerplate |
| **Persistence** | Jetpack DataStore Preferences | Asynchronous, non-blocking disk persistence |
| **Backup / Restore** | Android SAF (`CreateDocument` / `OpenDocument`) | Permissionless, offline JSON export and import |
| **Network Engine** | Java `HttpURLConnection` & RFC 6455 `SSLSocket` | Zero bulky external networking libraries; parses JSON with `org.json` |
| **Code Shrinking** | R8 Full Mode | Minified bytecode and resource shrinking |
| **Release Signing** | Custom Keystore (v2 + v3) | Avoids generic debug key flags and passes Google Play Protect |

---

## File & Directory Structure

```
nounishlauncher/
├── app/
│   ├── build.gradle.kts                         # Android build config, Compose BOM, R8 shrinking
│   ├── proguard-rules.pro                       # Keep rules for R8 optimization
│   └── src/main/
│       ├── AndroidManifest.xml                  # HOME launcher declaration, network permissions
│       ├── java/com/nounish/launcher/
│       │   ├── MainActivity.kt                  # SingleTask launcher activity, SAF backup/restore, screen transitions
│       │   ├── MainViewModel.kt                 # StateFlow coordination (apps, search, settings, crypto, labels)
│       │   ├── model/
│       │   │   ├── AppItem.kt                   # Data model for app label, customLabel, displayLabel, packageName
│       │   │   └── PassiveNote.kt               # Passive learning notes model and Nounish/crypto definitions
│       │   ├── data/
│       │   │   ├── AppRepository.kt             # PackageManager loader (labels only, no icons)
│       │   │   ├── SettingsStore.kt             # Jetpack DataStore wrapper for all preferences & cached prices
│       │   │   ├── PriceRepository.kt           # Binance public 24hr ticker client, klines sparklines, rate limit backoff
│       │   │   ├── IdxRepository.kt             # IDX stock quotes, indices, top gainers/losers, number formatting
│       │   │   ├── BinanceWebSocketClient.kt    # Native RFC 6455 WebSocket client for live Home stream
│       │   │   └── NetworkHelper.kt             # Metered network and battery saver detection
│       │   ├── ui/
│       │   │   ├── HomeScreen.kt                # Clock, date, market overview, text sparklines, pinned apps
│       │   │   ├── DrawerScreen.kt              # Search bar, left/right alphabet scrubber, Niagara wave list, rename
│       │   │   ├── SettingsScreen.kt            # Categorized launcher settings, backup/restore, coins & quotes
│       │   │   ├── GestureModifier.kt           # Pointer detection for swipes, taps, and long-presses
│       │   │   └── theme/
│       │   │       ├── Color.kt                 # AMOLED pitch black, light theme, crypto green/red colors
│       │   │       ├── Type.kt                  # Typography scale (28sp home, 18sp drawer, 14sp clock/ticker)
│       │   │       └── Theme.kt                 # CompositionLocalProvider for colors and typography
│       │   └── service/
│       │       ├── LockScreenService.kt         # Accessibility action for screen locking
│       │       └── PackageChangeReceiver.kt    # BroadcastReceiver for app install/uninstall updates
│       └── res/
│           ├── drawable/                        # Vector assets for monochrome launcher icon
│           ├── mipmap-anydpi-v26/               # Adaptive launcher icons
│           ├── values/                          # Strings, base themes (windowBackground #000000)
│           └── values-v28/                      # Display cutout support
├── build.gradle.kts                             # Root Gradle build script
├── settings.gradle.kts                          # Module inclusions & repository config
├── gradle.properties                            # JVM memory allocation & AndroidX flags
├── release.jks                                  # Dedicated release signing keystore
└── README.md                                    # Repository overview
```

---

## Configuration & Settings Guide

Accessible by long-pressing any empty area on the Home screen:

1. **Home Screen**:
   - `Show clock`: Toggle time in top-left corner.
   - `Show date`: Toggle date below the clock.
   - `Corner text`: Toggle custom text display in the bottom-right corner.
   - `Custom corner text`: Edit the corner text string (default: `xppaicyber`) or reset to default.
   - `Passive learning note`: Toggle passive learning card in top-right corner (features Nounish `⌐◨-◨` ethos, crypto slang, closeable with `x`, and tap to cycle).
2. **App Drawer**:
   - `Auto-launch on single match`: Instantly opens an app when search results narrow to 1.
   - `Alphabet index on right`: Places the alphabetical scrubber on the right edge (default: left).
   - `Alphabet index animation`: Enables/disables the Niagara-style fluid wave animation.
3. **Gestures**:
   - `Swipe left app`: Assign an app to launch on left swipe.
   - `Swipe right app`: Assign an app to launch on right swipe.
   - `Double tap to lock screen`: Toggle double-tap screen lock.
4. **Appearance**:
   - `Light theme`: Switch between AMOLED Black (`#000000`) and Light theme.
   - `Monospace font`: Switch between system sans-serif and monospace typeface.
5. **Crypto Price (Binance)**:
   - `Enable crypto price label`: Master toggle for all crypto features.
   - `Show crypto on home screen`: Displays the multi-coin market widget on the Home screen.
   - `Live WebSocket for Home screen`: Toggle live streaming of prices directly to the Home screen.
   - `Coins`: Comma-separated list of symbols (e.g. `BTC, ETH, SOL, PEPE, BTCUSDT, ETHBIDR`).
   - `Quote currency`: Pair currency presets (`USDT`, `USDC`, `EUR`, `BIDR`, `TRY`, `BRL`, `GBP`, `AUD`, `BTC`) or custom quote.
   - `Currency format`: Select display format for market prices (`Suffix 68,450 USDT / 9,850 IDR`, `Prefix`, or `None`).
6. **Backup & Restore**:
   - `Export settings (JSON)`: Save all settings, pinned apps, hidden apps, and custom labels to a JSON file.
   - `Import settings (JSON)`: Restore setup from a previous JSON backup file.
7. **Management**:
   - `Hidden Apps`: View and unhide hidden applications.
   - `Pinned Apps`: View and remove pinned home screen applications.
   - `Set as default launcher`: Direct shortcut to Android's default home app settings.

---

## Building & Releasing

### Requirements
- JDK 17 or JDK 21
- Android SDK Platform 34 & Build-Tools 34.0.0

### Build Commands

```powershell
# Set JDK 21 from Android Studio JBR
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"

# Build Debug APK
.\gradlew.bat assembleDebug

# Build R8-Shrunk & Signed Release APK
.\gradlew.bat assembleRelease
```

### Outputs
- **Release APK**: `app/build/outputs/apk/release/app-release.apk` (**~1.05 MB**)
- **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk` (**~8.7 MB**)

### Continuous Integration & Automated Releases
A GitHub Actions workflow (`.github/workflows/release.yml`) automatically builds the release APK and publishes a GitHub Release whenever a tag is pushed:
```bash
git tag v1.0.0
git push origin v1.0.0
```

---

## Security, Privacy & Google Play Protect

- **Zero Accessibility Exploits**: The release build does not declare background accessibility service permissions in the manifest, ensuring it passes Google Play Protect without security alerts.
- **Valid Release Signature**: Signed with a dedicated 2048-bit RSA key using APK Signature Schemes v2 and v3.
- **Zero Telemetry**: Backup and restore operations are 100% offline via Android Storage Access Framework.
- **Minimal Permissions**:
  - `INTERNET`: Exclusively for public Binance price tickers and WebSockets. Completely dormant when crypto features are turned off.
  - `ACCESS_NETWORK_STATE`: Used locally to stretch poll intervals on mobile cellular data or battery saver mode.
  - `QUERY_ALL_PACKAGES` / `CATEGORY_LAUNCHER`: Used strictly on-device to enumerate installed apps.
