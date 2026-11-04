# Nounish Launcher

A calm, distraction-free, text-only Android launcher designed for pure focus and AMOLED battery efficiency. No icons, no widgets, no ads, no telemetry, and offline by default.

---

## Features

- **Multi-Activity & Work Profile Support**: Uses Android's native `LauncherApps` API to display and launch both personal and work profile apps (`(Work)`) with independent user handles. Apps with multiple launcher activities are identified by unique composite keys (`packageName/activityName`).
- **Clean Uninstall Pruning**: Automatically drops removed or uninstalled apps from pinned shortcuts and DataStore, eliminating dead entries.
- **Accent- & Case-Insensitive Alphabetical Sorting**: Strips diacritics and normalizes accents so `Écran` and `Über` sort under `E` and `U` without distortion. Numbers and symbols (`1Password`, `7-Zip`, `@voice`) are cleanly grouped under `#` at the end.
- **Cold Start under 300 ms**: Pinned apps and market prices are drawn on frame 1 directly from local DataStore cache, while the full app list queries asynchronously in the background.
- **In-Memory App Caching**: The installed app list is loaded once into memory with disk persistence (`apps_cache.json`) and updated strictly when `PackageChangeReceiver` notifies of package changes. Opening the drawer never re-queries `PackageManager`.
- **Process-Death Resilient**: Full state restoration across OS process kills (active screen, search query, scroll position, and user configurations).
- **Zero-Jank Network Architecture**: All network operations (polling and WebSockets) run strictly on `Dispatchers.IO` and are cancelled the instant Home leaves the screen or during `onPause()`.
- **Minimalist App Icons & Pure Text Mode**: Option to display subtle, minimalist app icons beside each app (Home screen and App Drawer) with an optional monochrome/grayscale filter that preserves the AMOLED calm aesthetic, or toggle off completely for a pure text-only experience.
- **Custom App Renaming**: Long-press any app in the Home screen or Drawer to assign a custom label (e.g. "Chrome" -> "web", "Spotify" -> "music"). Easily reset back to default at any time.
- **AMOLED Monochrome & Typography**: Pitch black background (`#000000`) with crisp white and subtle grey typography. Includes an optional light theme, monospace toggle, and adjustable font size scaling (Micro 65%, Extra Small 75%, Small 85%, Compact 92%, Default 100%, Large 115%, Extra Large 130%) with live preview and compact, refined layout spacing.
- **Home Screen Essentials**:
  - **Pinned App Editing & Reordering**: Reorder pinned shortcuts with dedicated up (`↑`) and down (`↓`) controls in Settings without needing to unpin and re-pin.
  - **Pinned App Limits**: Enforces a maximum of 8 pinned apps to keep Home distraction-free, with zero minimum limit (unpin down to 0 freely). Attempting to pin a 9th app warns: "Maximum 8 pinned apps reached. Unpin an app first."
  - **Clock Options**: Choose between System Default (respects Android's 24-hour setting), 24-hour (`HH:mm`), 12-hour with AM/PM (`h:mm a`), or 12-hour without AM/PM (`h:mm`).
  - **Optional Seconds**: Live second display (`:ss`) for atomic precision.
  - **Date Format Customization**: Pick from common presets (`Full`, `Short`, `Standard with year`, `ISO 8601`, `US style`, `Compact`) or input any custom `SimpleDateFormat` pattern.
  - **Custom Corner Text (Bottom Right)**: Display custom text (such as your username, handle, or motivational quote) in the bottom-right corner of the Home screen, styled in muted secondary tone. Configurable in Settings (default: `xppaicyber`) or toggled off completely.
  - **Closeable Passive Learning Notes (Top Right)**: A calm, minimalist floating note card anchored at the top-right corner of the Home screen for passive learning. Features the definition of Nounish `⌐◨-◨` ("do good with no expectation of return, create positive externalities, embrace absurdity & difference, teach people about nouns & crypto, have fun and be honest. - PIP3"), with tap-to-cycle bites of crypto slang and web3 culture. Dismissable via a subtle `x` close button and easily restored anytime from Settings.
  - **Currency Format (Crypto USDT & Saham IDR)**: Real-time market prices clearly specify currency format across tickers, section headers, and 24h ranges—using `USDT` for Crypto (e.g. `68,450 USDT`) and `IDR` for IDX Saham (e.g. `9,850 IDR`). Configurable in Settings (Suffix, Prefix, or None).
  - Live clock and date (configurable, tap clock to open system clock).
  - **Live Binance Crypto Market Overview**: Fills the spacious center area with real-time tickers, current prices, 24-hour daily percentage change, and text sparklines.
  - **Text Sparklines**: 24-hour financial trends drawn with text block characters (` ▂▃▄▅▆▇█`) that remain strictly text-only on the main ticker row.
  - **Expandable 24h High/Low**: Tapping any coin row reveals its clean 24-hour range (`H: 78,068 · L: 77,262`).
  - **Indonesian Stock Exchange (IDX Saham) Price Overview**: Optional live IDX quotes (`https://idx.xpp.wtf/`) with modes for Auto Rotate, Market Indices (`IHSG`, `LQ45`), Top Gainers, Top Losers, or Custom Watchlist (`BBCA`, `BBRI`, `BMRI`, `TLKM`, `ASII`, `GOTO`), complete with expandable 24h high/low and volume on tap.
  - **Auto-Rotating IDX Categories with Staggered Wave Transition**: In Auto Rotate mode, the IDX view smoothly cycles through Market Indices, Top Gainers, Top Losers, and Watchlist. Each transition features a staggered wave animation where stock rows slide 1 by 1 to the left, accompanied by dynamic descriptive headers (`IDX SAHAM | Top Gainers`, `IDX SAHAM | Market Indices`, etc.) that can also be tapped to manually advance to the next category immediately.
  - **Nouns DAO Governance & Candidates Feed**: Optional live Nouns DAO governance proposals and candidate proposals powered by `https://xpp.wtf/nouns/` with Ethereum RPC fallback. Displays pure minimalist text rows (`#1002  Title  Status` for proposals, `Draft  Title  Candidate` for candidates) with color-coded status badges (`Active` in bold green, `Executed` in muted grey, `Defeated` in subtle red). Tapping any proposal reveals live voting statistics (`For`, `Against`, `Quorum`) and a direct `Vote ↗` link opening `https://nouns.wtf/vote/{id}` in your browser; tapping any candidate displays proposer address (`By: 0x...`) and a direct link opening `https://nouns.wtf/candidates/{proposer}-{slug}`.
  - **Auto-Rotating Nouns Proposals & Candidates**: When set to Auto Rotate mode, the Nouns section automatically alternates between Proposals and Candidates every 7 seconds (or manually via header tap), with a dynamic title indicator (`NOUNS DAO ⌐◨-◨ | Proposals` or `NOUNS DAO ⌐◨-◨ | Candidates`). You can also lock the view to Proposals only or Candidates only.
  - **Configurable Items to Display**: Easily configure how many proposals or candidates appear on the Home screen (2 to 6 items, default 3) to suit screen size.
  - **Separated Market & Community Views (Maximize Screen Space)**: Option to display Crypto, IDX Saham, and Nouns DAO simultaneously on the Home screen to maximize the generous middle screen area, with your choice of layout: **Stacked vertically** or **Multi-column** (Side by side).
  - **Auto-Rotating Carousel (Auto-Advance Slideshow)**: When separation is toggled off, the home center space smoothly alternates between active widgets (Crypto, IDX, Nouns DAO) with a configurable slide or crossfade transition, or manual tap switching via interactive tabs (`CRYPTO (USDT)` · `IDX SAHAM (IDR)` · `NOUNS DAO ⌐◨-◨`).
  - **Stale-Data Indicator**: When offline or displaying cached prices, values dim and display a subtle age marker (`·now`, `·5m`, `·1h`) so you always know when data was last updated.
  - Pinned apps (4–8 items) in large (`28 sp`) light-weight text, left-aligned for ergonomic one-handed thumb reach.
  - Quick action dialog on long-press to rename, unpin, or open system app settings.
  - Long-press background opens Settings.
- **App Drawer**:
  - Swipe up from Home to open instant search.
  - Zero icon loading: uses direct `PackageManager` labels for sub-millisecond load times and minimal RAM usage.
  - Real-time search filter with optional **Auto-launch** on single match.
  - Search matches both original system names and custom labels.
  - **Live USD/IDR Rate (Top Right)**: Real-time USD to Indonesian Rupiah exchange rate at the top right of the drawer with one-tap tactile refresh and stale-data indicators. Toggleable in Settings.
  - **Alphabetical Fast-Scroll Index**: Vertical `A–Z` + `#` index scrubber with tactile haptic feedback.
  - **Matching App Highlighting**: When scrubbing the alphabet index, matching apps are highlighted in crisp bold typography and full opacity, while non-matching apps and icons gracefully dim to muted grey so the drawer does not appear all white.
  - **Customizable Position**: Option to place the index on either the **Left** or **Right** edge.
  - **Niagara-Style Fluid Wave Animation**: When scrubbing the alphabet index, apps smoothly curve and wave towards your thumb with organic spring physics at 120 FPS.
  - **Animation Toggle**: Option to disable the wave animation for a completely static, zero-motion text list.
  - Long-press menu on any app: Rename, Pin to Home, Hide, App Info, Uninstall.
- **Hidden Apps**:
  - Hide distracting apps from the drawer list.
  - Hidden apps only appear if searched for by name or in Settings -> Hidden Apps.
- **Offline Settings Backup & Restore (JSON)**:
  - Export and import pinned apps, hidden apps, custom app labels, and launcher preferences as an offline JSON file.
  - Uses Android's native Storage Access Framework (`CreateDocument` and `OpenDocument`) with zero network transmission, perfectly fitting our offline and zero-telemetry architecture.
- **Gestures**:
  - **Swipe up**: Open drawer
  - **Swipe down**: Close drawer
  - **Swipe left / right**: Launch user-assigned quick apps
  - **Double-tap**: Screen lock
  - **Long-press**: Open Settings
- **Crypto Price Integration (Binance Public API)**:
  - Small header ticker in the top-right corner of the drawer (tap to cycle coins).
  - Color-coded daily percentage change: **Green** (`#00E676`) for positive, **Red** (`#FF5252`) for negative.
  - **Custom Coin List & Arbitrary Pairs**: Track any Binance symbols or full pairs (e.g. `BTC`, `ETH`, `SOL`, `PEPE`, `BTCUSDT`, `ETHBIDR`).
  - **Quote Currency Toggle**: Select quick presets (`USDT`, `USDC`, `EUR`, `BIDR` for IDR, `TRY`, `BRL`, `GBP`, `AUD`, `BTC`) or input any custom quote currency.
  - **Rate-Limit Handling with Exponential Backoff**: Automatically backs off exponentially on HTTP 429 rate limits or network errors (up to 240s).
  - **Mobile Data & Battery Saver Awareness**: Automatically stretches poll intervals from 15s to 60s on cellular data or battery saver mode (90s if both) to preserve battery life and data.
  - **Optional Live WebSocket on Home**: Stream live real-time Binance ticks directly on the Home screen via a native RFC 6455 WebSocket client. Immediately closed when the screen turns off or when leaving Home.
  - DataStore caching for instant offline viewing with stale-data age markers.

---

## Tech Stack & Architecture

- **Language**: Kotlin 1.9.24
- **UI Toolkit**: Jetpack Compose (Material3)
- **Minimum SDK**: 26 (Android 8.0 Oreo)
- **Target SDK**: 34 (Android 14)
- **Architecture**: Single Activity (`MainActivity`) + `MainViewModel` (MVI / MVVM pattern)
- **Storage**: Jetpack DataStore Preferences
- **Document Management**: Android Storage Access Framework (SAF) for offline JSON backup/restore
- **Networking**: Native `HttpURLConnection` and custom zero-dependency RFC 6455 `BinanceWebSocketClient` (zero bulky external libraries)
- **Footprint**: **~1.05 MB** optimized release APK with R8 minification and resource shrinking

---

## Project Structure

```
nounishlauncher/
├── app/
│   ├── build.gradle.kts                         # Android build config, Compose BOM, R8 shrinking
│   ├── proguard-rules.pro                       # R8 optimization rules
│   └── src/main/
│       ├── AndroidManifest.xml                  # HOME launcher declaration, network permissions
│       ├── java/com/nounish/launcher/
│       │   ├── MainActivity.kt                  # SingleTask launcher activity, SAF backup/restore, screen transitions
│       │   ├── MainViewModel.kt                 # StateFlow coordination (apps, search, settings, crypto, labels)
│       │   ├── model/
│       │   │   ├── AppItem.kt                   # App label, customLabel, displayLabel, packageName
│       │   │   └── PassiveNote.kt               # Passive learning notes model and Nounish/crypto definitions
│       │   ├── data/
│       │   │   ├── AppRepository.kt             # PackageManager loader (labels only, no icons)
│       │   │   ├── SettingsStore.kt             # Jetpack DataStore wrapper, JSON backup/restore serialization
│       │   │   ├── PriceRepository.kt           # Binance 24hr ticker, klines sparkline, rate-limit backoff
│       │   │   ├── IdxRepository.kt             # IDX stock quotes, indices, top gainers/losers, number formatting
│       │   │   ├── NounsRepository.kt           # Nouns DAO proposals & candidates loader (zero-dependency)
│       │   │   ├── BinanceWebSocketClient.kt    # Native RFC 6455 WebSocket client for live Home stream
│       │   │   └── NetworkHelper.kt             # Metered network and battery saver detection
│       │   ├── ui/
│       │   │   ├── HomeScreen.kt                # Clock, date, market overview, text sparklines, pinned apps
│       │   │   ├── DrawerScreen.kt              # Search bar, left/right alphabet scrubber, Niagara wave list
│       │   │   ├── SettingsScreen.kt            # Settings, coins & quote dialogs, WebSocket toggle, backup/restore
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
└── PROJECT_OVERVIEW.md                          # In-depth architectural & technical documentation
```

---

## Building & Running

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
Output: `app/build/outputs/apk/release/app-release.apk` (**~1.05 MB**)

### Automated CI / GitHub Releases
A GitHub Actions workflow (`.github/workflows/release.yml`) automatically builds the signed, minified release APK and attaches it to GitHub Releases whenever a git tag is pushed:
```bash
git tag v1.0.0
git push origin v1.0.0
```

---

## Privacy & Permissions

- **INTERNET**: Used solely for the public Binance market ticker. Completely inert when crypto features are toggled off in Settings.
- **ACCESS_NETWORK_STATE**: Used strictly on-device to check for mobile data vs Wi-Fi to stretch polling intervals.
- **QUERY_ALL_PACKAGES / CATEGORY_LAUNCHER**: Used strictly on-device to query installed apps for the drawer.
- **Zero tracking, zero analytics, zero external logging, zero telemetry.**
