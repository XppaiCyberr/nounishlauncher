# Nounish Launcher — Project Overview & Documentation

Nounish Launcher is a calm, distraction-free, text-only Android launcher built with **Kotlin** and **Jetpack Compose**. It strips away app icons, badges, notifications, and dopamine triggers to provide a focused, AMOLED-optimized interface that saves battery and honors your attention.

**Current Version**: `v1.0.27` (versionCode `28`) · **Optimized Release APK**: `~1.12 MB` (1,174,749 bytes)

---

## Design Philosophy

1. **Minimalist App Icons & Pure Text Mode**: Toggleable option to display subtle, minimalist app icons beside each app with an optional monochrome/grayscale filter that preserves the calm AMOLED aesthetic, or toggle off completely for a pure text-only experience.
2. **AMOLED Monochrome & Signature Nounish Accent Colors**: Pitch black background (`#000000`) paired with crisp white and subtle grey typography. Includes 6 curated Nounish accent palettes (Pure Monochrome, Nounish Red `⌐◨-◨`, Warm Amber, AMOLED Mint Green, Muted Slate, Soft Lavender) with targeted tinting, an optional light theme, monospace mode, and fine-grained font size scaling (Micro 65%, Extra Small 75%, Small 85%, Compact 92%, Default 100%, Large 115%, Extra Large 130%).
3. **Calm & Distraction-Free**: No notification badges, algorithmic feeds, unread counters, or telemetry traps.
4. **Privacy-Focused & Offline by Default**: Zero analytics, telemetry, or tracking. Network operations are strictly confined to user-enabled public market tickers (Binance public market data, IDX mirror, and Nouns DAO feeds), with zero background services.
5. **Instantaneous & Lightweight**: App cold starts in under 300 ms with an optimized release APK footprint of only **~1.11 MB** (1,164,866 bytes). Pinned shortcuts render on frame 1 from DataStore without waiting for `PackageManager`.
6. **Robust Process-Death Safety**: Full state restoration for active screen, drawer search, scroll offset, and settings across Android system process kills.
7. **Strict Background Discipline**: App lists are cached in memory and refreshed exclusively on package broadcast events. All network polling and WebSocket connections terminate immediately on `onPause()` or when navigating away from Home.
8. **Window Hardware Alpha Compositing Architecture (Zero-Ghosting)**: Translucent RGBA_8888 window surface composited directly by SurfaceFlinger against the system wallpaper, paired with a single static root scrim layer and sequential `AnimatedContent` transitions that prevent afterimage ghosting.

---

## Comprehensive Feature List

### 1. Home Screen
- **Time & Date Display**:
  - Clean, minimal time and date in the top-left corner (`14 sp`, secondary color).
  - Tapping the clock launches the system default alarm/clock application.
  - Toggles available to show/hide time and date independently.
  - **Clock Format Options**: Select between System Default (respects Android's 24-hour setting), 24-hour (`HH:mm`), 12-hour with AM/PM (`h:mm a`), or 12-hour without AM/PM (`h:mm`).
  - **Optional Seconds**: Live second display (`:ss`) for atomic precision.
  - **Date Format Presets & Custom Pattern**: Choose from presets (`Full`, `Short`, `Standard with year`, `ISO 8601`, `US style`, `Compact`) or specify any custom `SimpleDateFormat` pattern.
- **Live Binance Crypto Market Overview (Center Space)**:
  - Fills the spacious center area between the clock and pinned shortcuts.
  - Shows real-time tickers for your configured watchlist (e.g. `BTC`, `ETH`, `SOL`, `PEPE`, `BTCUSDT`, `ETHBIDR`).
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
- **Nouns DAO Live Auction, Governance & Candidate Proposals (Center Space)**:
  - **Live Daily Auction (Direct On-Chain RPC Query)**: Directly queries the canonical Nouns Auction House proxy contract [`0x830bd73e4184cef73443c15111a1df14e495c706`](https://etherscan.io/address/0x830bd73e4184cef73443c15111a1df14e495c706) via `eth_call` using selector `0x7d9f6db5` (`auction()`).
    - Decodes current Token ID (`nounId`), highest bid in Wei converted to ETH (`amount`), auction start/end timestamps (`startTime`, `endTime`), current bidder address (`bidder`), and settlement state (`settled`).
    - 100% decentralized and serverless with multi-endpoint RPC fallback (Alchemy, Cloudflare, PublicNode).
    - **Pixel-Art Noun Image Display (`https://noun.pics/{id}.png`)**: Displays the authentic 64x64 pixel-art avatar for the active auction Noun from `https://noun.pics/{nounId}.png` rendered with `FilterQuality.None` for crisp AMOLED sharpness. Cached on disk and in memory with `⌐◨-◨` placeholder while loading or offline. Toggleable in Settings (**Show Noun auction image**).
    - AMOLED typography: `[🖼️] Noun #2070    8.50 ETH    2h 14m left`.
    - Tap to expand: reveals larger avatar preview (`44.dp`/`52.dp`), `High Bidder: 0x3a41...f91c`, and `Bid on nouns.wtf ↗` link opening `https://nouns.wtf`.
    - **On-Chain Write Transactions Specification**: Full EIP-1193 payload specification and ABI encoding for `createBid(nounId)` and `settleCurrentAndCreateNewAuction()` detailed in [NOUNS_TRANSACTION_PAYLOAD.md](NOUNS_TRANSACTION_PAYLOAD.md).
  - **Governance & Candidates Feed**: Live Nouns DAO governance proposals and candidate proposals powered by `https://xpp.wtf/nouns/` with Ethereum RPC fallback.
  - Minimalist AMOLED text-only presentation: `#1002  Proposal Title  Status`.
  - Color-coded status indicator: **Active** in bold green (`CryptoGreen`), **Executed** in muted grey, **Defeated/Canceled/Vetoed** in subtle red.
  - Interactive voting expansion: Tapping any proposal reveals live quorum and vote counts (`For: 215 · Against: 0 · Quorum: 136 · Vote ↗`).
  - Direct voting action: Tapping `Vote ↗` opens `https://nouns.wtf/vote/{id}` directly in the system web browser. Tapping a candidate proposal opens `https://nouns.wtf/candidates/{proposer}-{slug}`.
  - Display modes: `Auto Rotate (Auction, Proposals & Candidates)`, `Auction only`, `Proposals only`, or `Candidates only`.
  - Status filtering: Option to filter by `All`, `Active`, or `Executed` proposals via Settings.
  - Configurable display limit: Configure 2 to 6 items displayed simultaneously.
  - Instant frame-1 local DataStore caching and resilient offline loading with stale markers (`·5m ago`, `·1h ago`).
  - Toggleable in Settings under **Nouns DAO -> Show Nouns DAO feed**.
- **Separated Market & Community Views (Maximize Middle Screen Space)**:
  - Option to display Crypto, IDX Saham, and Nouns DAO simultaneously on the Home screen to maximize the spacious middle area between the clock and pinned shortcuts.
  - Two selectable layout arrangements:
    - **Stacked vertically**: Clean stacked sections with subtle section headers (`CRYPTO (USDT)`, `IDX SAHAM (IDR) | Market Indices`, `NOUNS DAO ⌐◨-◨`).
    - **Multi-column (Side by side)**: Displays active widgets side by side in balanced vertical columns.
  - Middle market viewport automatically scales to available vertical space without crowding or colliding with the top clock or bottom pinned shortcuts.
  - Toggleable in Settings under **Market Layout -> Separate home widgets**.
- **Auto-Rotating Carousel (Auto-Advance Slideshow)**:
  - When separation is disabled, an auto-rotating carousel alternates smoothly between all active widgets (Crypto, IDX, Nouns DAO).
  - Animation transitions: Horizontal slide with subtle fade (`slide`) or smooth crossfade (`crossfade`).
  - Configurable slideshow rotation interval (3s to 15s, default 7s).
  - Manual interactive tabs: Tap `CRYPTO (USDT)`, `IDX SAHAM (IDR)`, or `NOUNS DAO ⌐◨-◨` to switch immediately and pause/resume smoothly. Selected tab is highlighted in your chosen accent color.
  - Toggleable in Settings under **Market Layout -> Auto-rotate market carousel**.
- **Pinned App Shortcuts & Reordering**:
  - Up to 8 pinned apps positioned in the lower-left area for ergonomic one-handed thumb reach, with zero minimum limit (unpin down to 0 apps freely).
  - **Dedicated Up/Down Reordering**: In Settings -> Pinned Apps, dedicated up (`↑`) and down (`↓`) controls allow direct reordering of shortcuts without unpinning and re-pinning.
  - **Maximum 8 Limit**: Cleanly enforces a maximum of 8 pinned apps. Attempting to pin a 9th app warns: "Maximum 8 pinned apps reached. Unpin an app first."
  - Large (`28 sp`), light-weight typography with generous vertical spacing.
  - Respects custom labels assigned by the user.
  - **Prunes Dead Entries**: If a pinned app is uninstalled, it is automatically dropped from pinned shortcuts and DataStore, leaving zero dead entries.
  - Long-press any shortcut to rename, unpin, or open system App Info.
- **Custom Corner Text (Bottom Right)**:
  - Displays user-defined text (such as your username, handle, or motivational quote) in the bottom-right corner of the Home screen, styled in muted secondary tone.
  - Configurable in Settings (default: `xppaicyber`) or toggled off completely.
- **Live Nouns Auction Card (Top Right Corner)**:
  - Live daily Nouns auction is featured prominently at the top-right corner of the Home screen opposite the Clock & Date.
  - Titled **`Nouns Auction`** in accent color (clean header, replacing previous Noggle title).
  - Balanced vertical structure: displays the pixel-art Noun avatar (`https://noun.pics/{id}.png`) alongside Noun ID and current bid (`Noun #2070`, `8.50 ETH`), with the real-time `timeleft` countdown (`2h 14m left` or `Settled`) positioned directly below them.
  - Interactive expansion: tap to reveal highest bidder address (`0x3a41...f91c`) and one-tap `Bid on nouns.wtf ↗` link opening `https://nouns.wtf`.
  - Smart de-duplication: When the top-right auction card is visible, the middle Nouns feed prioritizes governance Proposals and Candidates during auto-rotation, preventing duplicate auctions on screen.
- **Closeable Passive Learning Notes (Top Right Fallback)**:
  - Minimalist floating card at the top-right corner of the Home screen for passive learning when auction is off or unavailable.
  - Displays the definition of Nounish `⌐◨-◨` ("do good with no expectation of return, create positive externalities, embrace absurdity & difference, teach people about nouns & crypto, have fun and be honest. - PIP3"), with tap-to-cycle bites of crypto culture and web3 terminology.
  - Title rendered in signature accent color. Dismissable via a subtle `x` button and easily restored anytime from Settings.
- **Currency Format (Crypto USDT & IDX Saham IDR)**:
  - Real-time market prices clearly specify currency format across tickers, section headers, and 24h ranges—using `USDT` for Crypto (e.g. `68,450 USDT`) and `IDR` for IDX Saham (e.g. `9,850 IDR`).
  - Configurable in Settings (Suffix, Prefix, or None).
- **Manual Pull-to-Refresh by Swiping Down (Breathing & Typing ⌐◨-◨)**:
  - Swipe down anywhere on Home to trigger an instantaneous refresh across all active market feeds (Crypto, IDX Saham, Nouns DAO).
  - When enabled, automatic background polling is paused to conserve battery and data; feeds update strictly on demand when swiping down.
  - Features an organic pull interaction where the iconic `⌐◨-◨` glasses dynamically type out as your finger pulls down, accompanied by tactile haptic feedback.
  - While data is being fetched, the glasses pulse with an organic breathing animation and character typing loop, finishing with a completion tick and smooth spring retraction.
  - Toggleable in Settings under **General -> Gestures -> Swipe down action**.
- **Blank Area Long-Press**: Opens launcher Settings.

### 2. App Drawer & Search
- **Multi-Activity & Work Profile Support**:
  - Uses Android's official `LauncherApps` API across all user profiles (personal, work profile, secondary users).
  - Work profile apps are identified with a clean `(Work)` label suffix and launched directly into the managed profile via `launcherApps.startMainActivity`.
  - Apps with multiple launcher activities are keyed uniquely as `packageName/activityName`, eliminating duplicate collisions.
- **Accent- & Case-Insensitive Alphabetical Sorting**:
  - Uses Unicode NFD normalization to strip combining marks, sorting diacritics naturally (`Écran` under `E`, `Über` under `U`).
  - Apps starting with numbers or symbols (`1Password`, `7-Zip`, `@voice`) are cleanly sorted under `#` at the bottom of the list.
- **Recent Apps**:
  - Displays your most recently used apps in a horizontal scrollable pill row at the top (or anchored above the bottom search bar for instant one-handed reach).
  - Automatically records launched applications with LRU deduplication (up to 5 recent apps).
  - Seamlessly hides while typing a search query to prioritize filtered app matches.
  - Fully toggleable in Settings under **Apps & Backup -> Show recent apps in drawer**.
- **Instant Search**:
  - Swipe up from anywhere on the Home screen to open the app list.
  - Minimalist search input filters apps in real time as you type.
  - Text cursor tinted with your selected signature accent color (`cursorBrush = SolidColor(colors.accent)`).
  - Searches through both original app labels and user custom labels.
  - **Auto-Launch (Optional)**: Automatically launches the app when search results narrow down to exactly 1 match.
- **Inline Minimalist Calculator**:
  - Type arithmetic expressions directly into the search bar (`125 * 8`, `45000 / 12`, `(15 + 5) * 4`, `25% * 80`, `2^8`, `100 - 15%`) to calculate in real time.
  - Powered by a zero-dependency recursive descent math evaluator (`MathEvaluator.kt`).
  - Displays a clean `= <result>` row in your chosen accent color above app search results.
  - One-tap copy to clipboard with tactile haptic feedback and a subtle "Copied" indicator.
  - Pressing keyboard search immediately copies the calculated result.
  - Safe division-by-zero and modulo-by-zero handling; safely returns `null` on normal app queries.
- **Deep App Shortcuts (Android 7.1+)**:
  - Long-pressing any app on Home or Drawer asynchronously queries dynamic, manifest, and pinned shortcuts via native `LauncherApps.getShortcuts()`.
  - Displays shortcuts as clean AMOLED text rows above standard options (e.g., Chrome's "New Tab", messaging chats).
  - Tapping any shortcut launches it immediately via `launcherApps.startShortcut(...)` and dismisses the context menu/drawer.
- **Live USD/IDR Exchange Rate (Top Right)**:
  - Displays the live USD to Indonesian Rupiah exchange rate at the top right of the drawer (e.g. `USD 16,142` with 24h percentage change).
  - Fetched via Binance public market feeds (`USDTIDR` / `USDTBIDR`) with automated fallback to `open.er-api.com` and offline DataStore caching.
  - Interactive: tap with haptic tick to refresh immediately.
  - Stale-data indicator (`·5m`, `·1h`) dims the display when offline or using cached data.
  - Toggleable via Settings under **Crypto -> Show USD/IDR rate in drawer**.
- **Bottom Search Bar Option**:
  - Choose whether the search input bar (and live USD/IDR ticker) is anchored at the **Top** or **Bottom** of the drawer for optimal one-handed reachability on modern tall screens.
- **Alphabetical Fast-Scroll Index**:
  - Vertical `A–Z` + `#` scrubber for quick navigation.
  - **Configurable Position**: Move the index to either the **Left** or **Right** edge via Settings.
  - Active letters with installed apps are highlighted; empty letters are subtly dimmed.
  - **Tactile Haptic Feedback**: Light haptic tick as your thumb crosses each letter.
  - **Matching App Highlighting & Non-Matching Dimming**: When interacting with or scrubbing the index, all apps starting with the selected letter are highlighted in bold typography and accent color, while non-matching apps dim down to soft muted grey (`0.28` alpha) so the list is never an undifferentiated sea of white.
  - **Smart Visibility**: Scrubber automatically hides when typing a search query to give maximum screen width to results.
- **Niagara-Style Fluid Wave Animation**:
  - When scrubbing the alphabet index, visible apps smoothly arc and wave towards your thumb.
  - The targeted app shifts up to **`+90 dp`** towards the index with a **`1.16x`** scale and bold typography, while neighboring items curve along a smooth Hermite curve.
  - Runs at 120 FPS via draw-phase `graphicsLayer` rendering without triggering recompositions.
  - **Thumb-Aligned Auto-Scroll**: Selected apps center around your thumb rather than jumping to the top of the viewport.
  - **Organic Spring Physics**: When released, the list gently springs back to baseline with a physics-based spring animation.
  - **Animation Toggle**: Option in Settings to disable the wave animation for a static, zero-motion list.
- **App Context Menu (Long-Press on any item)**:
  - **Deep App Shortcuts**: Direct access to dynamic and manifest shortcuts.
  - **Rename App**: Assign a custom alias or label (e.g. "Chrome" -> "web"). Includes a one-tap Reset to default option.
  - **Pin to Home / Unpin from Home**
  - **Hide App / Unhide App**
  - **App Info** (Opens system application details)
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
- **Swipe Down on Home**: Fully configurable action:
  - **Open Notification Shade** (default): Pulls down the system notification shade from anywhere on the screen for effortless one-handed ergonomics.
  - **Refresh Market & Nouns Data**: Manually refresh market and Nouns data (Binance Crypto, IDX, Nouns DAO) with animated `⌐◨-◨` pull, breathing pulse, and character typing indicator. When selected, automatic background polling is paused.
  - **Open App Drawer**: Opens the search and app drawer on downward swipe.
  - **Disabled**: Turns off downward swipe on Home.
- **Swipe Down in Drawer**: Close App Drawer.
- **Swipe Left**: Launch user-assigned quick app.
- **Swipe Right**: Launch user-assigned quick app.
- **Double-Tap**: Screen lock trigger via `LockScreenService`.
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

### 7. Aesthetics, Wallpaper & Personalization
- **Signature Nounish Accent Colors**: Choose between Pure Monochrome, Nounish Red `⌐◨-◨` (`#E53935`), Warm Amber (`#FFB300`), AMOLED Mint Green (`#00E676`), Muted Slate (`#90A4AE`), and Soft Lavender (`#B39DDB`).
- **Customizable Haptic Feedback Intensity**: Choose between `Standard`, `Subtle (Light Tick)`, `Firm (Strong)`, and `Off (Disabled)` for tactile feedback across gestures, alphabet scrubbing, pull-to-refresh, and copying.
- **Wallpaper Change Support**: Direct integration with Gallery / Photos (crop-and-set) and Android System Wallpaper & Style picker from within Settings.
- **AMOLED Dark Scrim Dimmer**: 50% to 95% dark overlay slider for optimal contrast against custom wallpapers.
- **Font Size Scaling**: 7 distinct scale tiers (`Micro 65%`, `Extra Small 75%`, `Small 85%`, `Compact 92%`, `Default 100%`, `Large 115%`, `Extra Large 130%`).
- **Monospace Font Toggle**: Switch between system sans-serif and monospace typography.
- **Minimalist App Icons & Monochrome Filter**: Optional subtle icons with grayscale filter or pure text mode.

---

## Window Hardware Alpha Compositing Architecture

To display custom wallpapers beneath an AMOLED launcher without sacrificing battery life, frame rates, or visual clarity, Nounish Launcher implements a specialized **Window Hardware Alpha Compositing Architecture**:

```
┌────────────────────────────────────────────────────────┐
│             Android System Wallpaper Layer             │
└───────────────────────────▲────────────────────────────┘
                            │ Hardware Composited via SurfaceFlinger
┌───────────────────────────┴────────────────────────────┐
│   Window Surface: PixelFormat.TRANSLUCENT (RGBA_8888)  │
│   Themes: windowIsTranslucent=true, showWallpaper=true │
├────────────────────────────────────────────────────────┤
│   Root Compose Surface: LauncherTheme.colors.background │
│   Static Scrim Layer: Color.Black.copy(alpha=dimAlpha) │
├────────────────────────────────────────────────────────┤
│   AnimatedContent Sequential Transitions (Zero Ghosting)│
│   fadeIn(tween(120ms)) togetherWith fadeOut(tween(90ms))│
│   • Home Screen                                        │
│   • App Drawer (clean disposal of outgoing screens)    │
│   • Settings Hub                                       │
└────────────────────────────────────────────────────────┘
```

### 1. RGBA_8888 Translucent Window Surface
In `MainActivity.onCreate()`:
```kotlin
window.setFormat(PixelFormat.TRANSLUCENT)
enableEdgeToEdge()
```
Setting `PixelFormat.TRANSLUCENT` instructs the Android window manager to allocate an RGBA_8888 framebuffer for the window rather than an opaque RGB_565 or RGBX_8888 buffer. This enables Android's hardware compositor (`SurfaceFlinger`) to blend the activity surface directly above the wallpaper buffer at hardware level, avoiding dithering, color banding, and composition stalls.

### 2. Window Translucency & Wallpaper Flags
In `res/values/themes.xml` and `res/values-v28/themes.xml`:
```xml
<style name="Theme.NounishLauncher" parent="android:Theme.Material.NoActionBar">
    <item name="android:windowShowWallpaper">true</item>
    <item name="android:windowBackground">@android:color/transparent</item>
    <item name="android:colorBackground">@android:color/transparent</item>
    <item name="android:windowIsTranslucent">true</item>
    <item name="android:statusBarColor">@android:color/transparent</item>
    <item name="android:navigationBarColor">@android:color/transparent</item>
</style>
```
When wallpaper mode is enabled, `MainActivity` dynamically synchronizes the window flags:
```kotlin
if (settings.showWallpaper) {
    window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
    window.setFormat(PixelFormat.TRANSLUCENT)
    window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
} else {
    window.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
    window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.BLACK))
}
```

### 3. Static Root Scrim Layer
Traditional implementations apply semi-transparent backgrounds to every screen composable (`HomeScreen`, `DrawerScreen`, `SettingsScreen`), which results in:
- Multiple layers of semi-transparent alpha multiplying together during screen transitions.
- Visual darkening and stutter as overlapping scrims blend simultaneously.
- Ghosting of text and UI elements against the wallpaper.

Nounish Launcher resolves this by hosting all screens inside a single root `Surface`:
```kotlin
Surface(
    modifier = Modifier.fillMaxSize(),
    color = LauncherTheme.colors.background
) { ... }
```
`LauncherTheme.colors.background` evaluates the wallpaper scrim once at the root:
```kotlin
val bg = if (showWallpaper) Color.Black.copy(alpha = dimAlpha) else AmoledBackground
```
Child screens render on transparent surfaces. This guarantees the wallpaper scrim remains completely static, uniform, and flicker-free regardless of user navigation.

### 4. `AnimatedContent` Sequential Transitions with Clean Disposal
Previously, `Crossfade` transitions rendered the incoming screen and the outgoing screen simultaneously over the translucent window surface. Against wallpaper scrims, this caused noticeable afterimages and ghosting of text lines.

Nounish Launcher replaces `Crossfade` with `AnimatedContent`:
```kotlin
AnimatedContent(
    targetState = currentScreen,
    transitionSpec = {
        fadeIn(animationSpec = tween(durationMillis = 120)) togetherWith
            fadeOut(animationSpec = tween(durationMillis = 90))
    },
    label = "ScreenTransition"
) { screen ->
    when (screen) {
        Screen.Home -> HomeScreen(...)
        Screen.Drawer -> DrawerScreen(...)
        Screen.Settings -> SettingsScreen(...)
    }
}
```
The outgoing screen fades out rapidly (90 ms) and is cleanly disposed from the composition tree, while the incoming screen smoothly fades in (120 ms). This sequential transition architecture eliminates all afterimages and produces zero ghosting.

---

## Technical Stack & Architecture

| Component | Choice | Details |
|---|---|---|
| **Language** | Kotlin 1.9.24 | Idiomatic, coroutine-based asynchronous processing |
| **UI Framework** | Jetpack Compose (Material3) | Declarative UI, Compose BOM `2024.04.01`, 120 FPS hardware-accelerated animations |
| **Target / Min SDK** | Target SDK 34 / Min SDK 26 | Android 8.0 (Oreo) through Android 14+ |
| **Architecture** | Single Activity + MVI / MVVM | `MainActivity` coordinating `MainViewModel` and immutable `StateFlow` |
| **Compositing Architecture** | Hardware Alpha Compositing | `PixelFormat.TRANSLUCENT` (RGBA_8888), `windowIsTranslucent`, static root scrim layer, `AnimatedContent` transitions |
| **Haptic Feedback** | `LauncherHaptic` Utility | 4 configurable intensity levels (`Standard`, `Subtle`, `Firm`, `Off`) |
| **Inline Calculator** | `MathEvaluator` Engine | Zero-dependency recursive descent math parser with division-by-zero protection |
| **Deep Shortcuts** | Android `LauncherApps` API | Queries dynamic, manifest, and pinned shortcuts for Android 7.1+ |
| **State Management** | Kotlin Coroutines & `StateFlow` | Reactive UI binding without boilerplate |
| **Persistence** | Jetpack DataStore Preferences | Asynchronous, non-blocking disk persistence |
| **Backup / Restore** | Android SAF (`CreateDocument` / `OpenDocument`) | Permissionless, offline JSON export and import |
| **Network Engine** | Java `HttpURLConnection` & RFC 6455 `SSLSocket` | Zero external networking libraries; parses JSON with `org.json` |
| **Code Shrinking** | R8 Full Mode | Minified bytecode and resource shrinking |
| **Release Footprint** | **~1.11 MB** (1,164,866 bytes) | Ultra-lightweight signed release APK |
| **Release Signing** | Custom Keystore (v1 + v2 + v3 + v4) | Passes Google Play Protect and security audits |

---

## File & Directory Structure

```
nounishlauncher/
├── app/
│   ├── build.gradle.kts                         # Android build config, Compose BOM, R8 shrinking
│   ├── proguard-rules.pro                       # Keep rules for R8 optimization
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml              # HOME launcher declaration, network permissions, <queries>
│       │   ├── java/com/nounish/launcher/
│       │   │   ├── MainActivity.kt              # SingleTask launcher activity, SAF backup/restore, screen transitions
│       │   │   ├── MainViewModel.kt             # StateFlow coordination (apps, search, settings, crypto, labels)
│       │   │   ├── model/
│       │   │   │   ├── AppItem.kt               # App item model (labels, composite keys, user handles)
│       │   │   │   ├── AppShortcut.kt           # Deep shortcut model (dynamic, manifest, pinned)
│       │   │   │   └── PassiveNote.kt           # Passive learning notes model and Nounish definitions
│       │   │   ├── data/
│       │   │   │   ├── AppRepository.kt         # PackageManager loader, LauncherApps, deep shortcuts
│       │   │   │   ├── SettingsStore.kt         # Jetpack DataStore wrapper for all preferences & cached prices
│       │   │   │   ├── PriceRepository.kt       # Binance public 24hr ticker client, sparklines, rate limit backoff
│       │   │   │   ├── IdxRepository.kt         # IDX stock quotes, indices, top gainers/losers, formatting
│       │   │   │   ├── NounsRepository.kt       # Nouns DAO governance proposals & candidates loader
│       │   │   │   ├── BinanceWebSocketClient.kt# Native RFC 6455 WebSocket client for live Home stream
│       │   │   │   ├── IconCache.kt             # LRU memory cache for app icons
│       │   │   │   └── NetworkHelper.kt         # Metered network and battery saver detection
│       │   │   ├── ui/
│       │   │   │   ├── HomeScreen.kt            # Clock, date, market overview, text sparklines, pinned apps
│       │   │   │   ├── DrawerScreen.kt          # Search bar, recent apps, calculator row, Niagara wave index
│       │   │   │   ├── SettingsScreen.kt        # Settings hub, accent dialog, wallpaper, haptic level, SAF
│       │   │   │   ├── GestureModifier.kt       # Pointer detection for swipes, taps, and long-presses
│       │   │   │   └── theme/
│       │   │   │       ├── Color.kt             # AMOLED pitch black, accent colors (Red, Amber, Mint, Slate, Lavender)
│       │   │   │       ├── Type.kt              # Typography scale (28sp home, 18sp drawer, 14sp clock/ticker)
│       │   │   │       └── Theme.kt             # Dynamic Compose theme provider and static scrim background
│       │   │   ├── util/
│       │   │   │   ├── LauncherHaptic.kt        # Multi-level haptic feedback dispatcher (Standard, Subtle, Firm, Off)
│       │   │   │   └── MathEvaluator.kt         # Zero-dependency arithmetic tokenizer & recursive descent evaluator
│       │   │   └── service/
│       │   │       ├── LockScreenService.kt     # Screen lock accessibility action
│       │   │       ├── NotificationHelper.kt    # Notification shade expansion via StatusBarManager reflection
│       │   │       └── PackageChangeReceiver.kt # BroadcastReceiver for app install/uninstall updates
│       │   └── res/
│       │       ├── drawable/                    # Vector assets for monochrome launcher icon
│       │       ├── mipmap-anydpi-v26/           # Adaptive launcher icons
│       │       ├── values/                      # Strings, window themes (windowIsTranslucent, transparent)
│       │       └── values-v28/                  # Display cutout support and window themes
│       └── test/java/com/nounish/launcher/
│           ├── AestheticsAndHapticsTest.kt      # Accent color mappings, default settings, haptic null-safety
│           ├── AppShortcutTest.kt               # Deep shortcuts rank sorting, label fallbacks, filtering
│           ├── MathEvaluatorTest.kt             # Arithmetic operations, precedence, decimals, safety checks
│           ├── RecentAppsAndWallpaperTest.kt    # Recent apps LRU history, wallpaper dim level coercion
│           └── SwipeDownActionTest.kt           # Swipe down gesture actions mapping and defaults
├── build.gradle.kts                             # Root Gradle build script
├── settings.gradle.kts                          # Module inclusions & repository config
├── gradle.properties                            # JVM memory allocation & AndroidX flags
├── release.jks                                  # Dedicated release signing keystore
├── PLAY_STORE_COMPLIANCE_GUIDE.md               # Google Play Store compliance, package visibility & publishing guide
└── README.md                                    # Repository overview
```

---

## Configuration & Settings Guide

Accessible by long-pressing any empty area on the Home screen. The Settings experience is organized into a modular Settings Hub with direct category selector pills (`General` · `Crypto` · `IDX` · `Nouns` · `Theme` · `Layout` · `Apps`) and nested back navigation:

### 1. General
- `Default launcher`: Direct shortcut to Android's default home app settings.
- `Show clock & time format`: Toggle time display in top-left corner, 12h/24h format, and seconds display.
- `Show date & date format`: Toggle date display below the clock and configure date pattern.
- `Corner text`: Toggle custom text display in the bottom-right corner and edit the string (default: `xppaicyber`).
- `Passive learning note`: Toggle passive learning card in top-right corner (features Nounish `⌐◨-◨` ethos, crypto slang, closeable with `x`, tap to cycle next tip).
- `Gestures`:
  - `Swipe down action`: Configure downward swipe action on Home: Open notification shade (default), Refresh market & Nouns data (pull-down with ⌐◨-◨ indicator; pauses background polling), Open app drawer, or Disabled.
  - `Swipe left app`: Assign an app to launch on left swipe.
  - `Swipe right app`: Assign an app to launch on right swipe.
  - `Double tap to lock screen`: Toggle double-tap screen lock.
- `App Drawer`:
  - `Auto-launch on single match`: Instantly opens an app when search results narrow to 1.
  - `Search bar at bottom`: Moves the search input and live USD/IDR ticker to the bottom of the drawer for optimal one-handed reachability.
  - `Alphabet index on right`: Places the alphabetical scrubber on the right edge (default: left).
  - `Alphabet index animation`: Enables/disables the Niagara-style fluid wave animation.

### 2. Crypto
- `Enable crypto price label`: Master toggle for all crypto features.
- `Show crypto on home screen`: Displays the multi-coin market widget on the Home screen.
- `Show USD/IDR rate in drawer`: Shows live exchange rate header inside the app drawer.
- `Live WebSocket for Home screen`: Toggle live real-time price streaming directly on the Home screen.
- `Coins`: Comma-separated list of symbols (e.g. `BTC, ETH, SOL, PEPE, BTCUSDT, ETHBIDR`).
- `Quote currency`: Pair currency presets (`USDT`, `USDC`, `EUR`, `BIDR`, `TRY`, `BRL`, `GBP`, `AUD`, `BTC`) or custom quote.

### 3. IDX Saham
- `Show IDX on home screen`: Toggle Indonesian stock market data on the Home screen.
- `IDX display mode`: Choose between `Auto Rotate` (cycles through indices, gainers, losers, watchlist), `Watchlist`, `Indices` (IHSG + LQ45), `Top Gainers`, or `Top Losers`.
- `Rotation interval`: Configure rotation timing (3s, 5s, 6s, 8s, 10s, 15s).
- `Watchlist symbols`: Comma-separated list of IDX stocks and indices (e.g. `IHSG, BBCA, BBRI, BMRI, TLKM, ASII, GOTO`).

### 4. Nouns DAO ⌐◨-◨
- `Show Nouns DAO feed`: Displays live governance proposals and candidate proposals on the Home screen.
- `Nouns display mode`: Choose between `Auto Rotate (Proposals & Candidates)`, `Proposals only`, or `Candidates only`.
- `Items to display`: Configure maximum items displayed on the Home screen (2, 3, 4, 5, or 6 items).
- `Proposal status filter`: Filter proposals by status (`All`, `Active`, `Executed`).

### 5. Theme & Aesthetics
- `Theme mode`: Switch between AMOLED Black (`#000000`) and Light theme.
- `Show system wallpaper & dark scrim dimmer`: Toggle displaying the device wallpaper behind a customizable dark AMOLED scrim overlay (50% to 95%, default 80%).
- `Change wallpaper`: Update device wallpaper directly from Gallery / Photos (crop-and-set) or open the Android System Wallpaper & Style picker.
- `Accent color`: Choose from signature Nounish accent palettes (`Pure Monochrome`, `Nounish Red ⌐◨-◨ #E53935`, `Warm Amber #FFB300`, `AMOLED Mint Green #00E676`, `Muted Slate #90A4AE`, `Soft Lavender #B39DDB`) to tint the glasses, category pills, switches, active tabs, and index scrubber.
- `Haptic feedback intensity`: Configure tactile vibration response across the launcher (`Standard`, `Subtle (Light Tick)`, `Firm (Strong)`, `Off (Disabled)`).
- `Monospace font`: Switch between system sans-serif and monospace typeface.
- `Font size scaling`: Adjust global font size across 7 scale levels (`Micro`, `Extra Small`, `Small`, `Compact`, `Default`, `Large`, `Extra Large`).
- `Show app icons`: Toggle visibility of app icons in drawer and pinned lists.
- `Monochrome icons`: Toggle black & white tinted app icons for ultra-minimalist aesthetics.

### 6. Market Layout
- `Separate home widgets`: Toggle separate widgets for Crypto, IDX, and Nouns vs single combined carousel widget.
- `Separated layout mode`: Choose between `Stacked` (vertical list) or `Side-by-side` (multi-column horizontal).
- `Carousel auto-rotate`: Automatically cycles through market widgets.
- `Carousel interval & transition`: Configure timing (3s, 5s, 7s, 8s, 10s, 15s) and animation style (`Slide` or `Crossfade`).
- `Currency format`: Select display format for market prices (`Suffix 68,450 USDT / 9,850 IDR`, `Prefix`, or `None`).

### 7. Apps & Backup
- `Show recent apps in drawer`: Toggle horizontal scrollable pill row of recent apps at the top of the drawer.
- `Pinned apps manager`: Reorder pinned home screen applications using dedicated move up/down (`↑`/`↓`) buttons or unpin.
- `Hidden apps manager`: View hidden applications and unhide with a single tap.
- `Export settings (JSON)`: Save all settings, pinned apps, hidden apps, and custom labels to an offline JSON file via SAF.
- `Import settings (JSON)`: Restore setup from a previous JSON backup file via SAF.

---

## Comprehensive Unit Test Suites

The codebase includes an extensive suite of unit tests validating core algorithms, parsers, and aesthetics:

1. **`MathEvaluatorTest`**:
   - Arithmetic evaluation: addition, subtraction, multiplication, division, exponentiation (`^`), and modulo (`%`).
   - Operator precedence: PEMDAS compliance and parentheses handling.
   - Postfix percentage syntax (`25% * 80`, `100 - 15%`).
   - Floating point and decimal operations (`0.1 + 0.2 -> "0.3"`).
   - Division-by-zero and modulo-by-zero safety checks returning `null`.
   - App search query rejection: ensures strings like `"Chrome"`, `"YouTube"`, or `"Settings"` safely return `null`.
2. **`AppShortcutTest`**:
   - Deep shortcut data models and attributes.
   - Label fallback logic: preferring `shortLabel` -> `longLabel` -> `id`.
   - Enabled status filtering and rank-based sorting.
   - Shortcut collection capping (up to 5 items).
3. **`AestheticsAndHapticsTest`**:
   - Signature accent color mappings for Dark and Light modes.
   - Default launcher settings verification (`"monochrome"`, `"standard"`).
   - Null-safety execution across all 4 haptic levels (`Standard`, `Subtle`, `Firm`, `Off`).
4. **`RecentAppsAndWallpaperTest`**:
   - Recent apps LRU history deduplication, most recent promotion, and max capacity capping (5 items).
   - Wallpaper dark scrim level clamping (`0.2f` to `0.98f`).
5. **`SwipeDownActionTest`**:
   - Swipe-down gesture mappings (`notifications`, `refresh`, `drawer`, `none`).
   - Default action fallback verification.

Run unit tests via Gradle:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat test
```

---

## Building & Releasing

### Requirements
- JDK 17 or JDK 21 (e.g. from Android Studio JBR: `C:\Program Files\Android\Android Studio\jbr`)
- Android SDK Platform 34 & Build-Tools 34.0.0

### Build Commands

```powershell
# Set JDK from Android Studio JBR
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"

# Build Debug APK
.\gradlew.bat assembleDebug

# Build R8-Shrunk & Signed Release APK
.\gradlew.bat assembleRelease

# Build Android App Bundle (.aab)
.\gradlew.bat bundleRelease
```

### Outputs
- **Release APK**: `app/build/outputs/apk/release/app-release.apk` (**~1.11 MB** / 1,164,866 bytes)
- **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk` (~8.7 MB)
- **Release App Bundle**: `app/build/outputs/bundle/release/app-release.aab`

### Continuous Integration & Automated Releases
A GitHub Actions workflow (`.github/workflows/release.yml`) automatically builds the release APK and publishes a GitHub Release whenever a tag is pushed:
```bash
git tag v1.0.24
git push origin v1.0.24
```

---

## Security, Privacy & Google Play Protect

- **Zero Accessibility Exploits**: The release build does not declare background accessibility service permissions in the manifest, ensuring it passes Google Play Protect without security alerts.
- **Valid Release Signature**: Signed with a dedicated 2048-bit RSA key using APK Signature Schemes v1, v2, v3, and v4.
- **Zero Telemetry**: Backup and restore operations are 100% offline via Android Storage Access Framework.
- **Minimal Permissions**:
  - `INTERNET`: Exclusively for public Binance price tickers, IDX quotes, and Nouns DAO feeds. Completely dormant when market features are turned off.
  - `ACCESS_NETWORK_STATE`: Used locally to stretch poll intervals on mobile cellular data or battery saver mode.
  - `EXPAND_STATUS_BAR`: Standard normal-level Android permission enabling the swipe-down gesture to expand the system notification shade.
  - `SET_WALLPAPER`: Standard normal-level Android permission enabling setting the system wallpaper when chosen via Gallery or the system wallpaper picker.
- **Package Visibility (`<queries>`)**: Uses Android 11+ `<queries>` matching `CATEGORY_LAUNCHER` and `LauncherApps` API. Does **not** declare broad `QUERY_ALL_PACKAGES` permission, ensuring full compliance with Google Play Store policies.
