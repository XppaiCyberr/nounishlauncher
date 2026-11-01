# Minimal Text-Only Android Launcher: Plan

## 1. Goal
A home screen that is only text. No icons, no widgets, no ads, no tracking, and no network use unless you turn on the optional price label. Fast, tiny, and calm.

## 2. Design Principles
- **Text only**: apps are names, nothing else.
- **Monochrome**: pure black background, white/grey text (AMOLED friendly). Optional light theme.
- **One typeface, two sizes**: large for home shortcuts, smaller for the app list.
- **No animation noise**: only quick fades (under 150 ms).
- **Zero clutter**: no status widgets, no badges, no suggestions.
- **Offline by default**: the only network feature is the optional crypto price label (off by default; see section 7).

## 3. Features

### MVP
1. **Home screen**: clock/date (optional) and 4 to 6 pinned apps as large left-aligned text.
2. **App drawer**: swipe up opens an alphabetical text list with a search field. Typing filters instantly. If only one match remains, it can auto-launch (optional setting).
3. **Gestures**
   - Swipe up: app drawer
   - Swipe left / right: launch user-chosen apps
   - Double tap: lock screen (optional)
   - Long press: settings
4. **Pin / unpin apps**: long-press an item in the drawer.
5. **Hide apps**: hidden apps only appear when searched for by exact name.
6. **Set as default launcher** (HOME intent).
7. **Crypto price label** (optional, off by default): small text in the top right corner of the app list showing a coin price from the Binance public API.

### Later (v2)
- App rename (custom labels)
- Per-app usage timer or "are you sure?" delay for distracting apps
- Daily wallpaper or plain-color wallpaper
- Font choice (system sans / monospace)
- Backup/restore settings as JSON

## 4. Tech Stack
| Area | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose (or plain Views if you want the smallest APK) |
| Min SDK | 26 (Android 8.0) |
| Target SDK | Latest stable |
| Storage | Jetpack DataStore (pinned apps, hidden apps, settings) |
| Architecture | Single Activity + ViewModel, no DI framework |
| Networking | `HttpURLConnection` or OkHttp (only for the price label) |
| Dependencies | Keep to AndroidX only (plus optional OkHttp) |
| Build | Gradle (Kotlin DSL) |

## 5. Project Structure
```
app/src/main/
  java/.../launcher/
    MainActivity.kt        // home + drawer host
    HomeScreen.kt          // pinned text list, clock
    DrawerScreen.kt        // searchable app list
    SettingsScreen.kt
    AppRepository.kt       // load installed apps via PackageManager
    SettingsStore.kt       // DataStore wrapper
    PriceRepository.kt     // Binance price fetch + cache
    GestureModifier.kt     // swipe / double tap handling
    Theme.kt               // colors, typography
  AndroidManifest.xml
```

## 6. Key Implementation Notes

**Manifest: become a launcher**
```xml
<activity
    android:name=".MainActivity"
    android:launchMode="singleTask"
    android:exported="true"
    android:stateNotNeeded="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.HOME" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</activity>
```

**Load apps (labels only, never load icons)**
```kotlin
val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
val apps = pm.queryIntentActivities(intent, 0)
    .map { AppItem(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
    .sortedBy { it.label.lowercase() }
```
Android 11+ needs `<queries>` in the manifest (or `QUERY_ALL_PACKAGES`) to see all apps. Prefer a `<queries>` block with the LAUNCHER intent.

**Launch an app**: `pm.getLaunchIntentForPackage(pkg)` then `startActivity`.

**Keep it fresh**: register a `BroadcastReceiver` for `PACKAGE_ADDED`, `PACKAGE_REMOVED` so the list updates on install or uninstall.

**Lock screen gesture**: needs either an Accessibility Service (`GLOBAL_ACTION_LOCK_SCREEN`, API 28+) or Device Admin. Make it opt-in, since it adds permissions.

**Back button**: on the home screen, do nothing. In the drawer, return home.

**Skipping icons** gives a big speed and memory win, so the drawer can be a plain `LazyColumn` of `Text`.

## 7. Crypto Price Label (optional)

**What**: a small text label in the top right corner of the app list, e.g. `BTC 67,420`, in the secondary grey color.

**Behavior**
- Off by default. A toggle in settings enables it. With it off, the app makes no network calls.
- Tap the label to cycle through the chosen coins (default: BTC, ETH, SOL).
- Fetch once when the drawer opens, then refresh every 15 seconds while the drawer is visible. Stop when it closes. No background service.
- Show the last cached price instantly (stored in DataStore), then update when the fresh one arrives. If the request fails, keep the cached value and dim it slightly.
- Round for readability: no decimals above 100, 2 decimals below.

**Data source: Binance public API (no API key)**
```
GET https://api.binance.com/api/v3/ticker/price?symbol=BTCUSDT
-> {"symbol":"BTCUSDT","price":"67420.12000000"}
```
Fetch several coins in one request:
```
GET https://api.binance.com/api/v3/ticker/price?symbols=["BTCUSDT","ETHUSDT","SOLUSDT"]
```
For 24h change (optional, colored text only, no arrows):
```
GET https://api.binance.com/api/v3/ticker/24hr?symbol=BTCUSDT
```

**Implementation notes**
- Add `<uses-permission android:name="android.permission.INTERNET" />`.
- Run requests on `Dispatchers.IO` from the ViewModel, and cancel the refresh loop when the drawer is not visible (`repeatOnLifecycle` or a `LaunchedEffect` tied to the drawer state).
- Use a short timeout (5 s) and parse the JSON with `org.json`, so no extra library is needed.
- Settings: enable toggle, coin list (symbols), refresh interval, quote currency (USDT default).
- If `api.binance.com` is blocked on the user's network, try `api.binance.me` or `data-api.binance.vision` as fallback hosts.
- Design: 14 sp, right aligned, 24 dp from the edge, same top line as the search field.

## 8. Design Spec
- Background: `#000000`
- Primary text: `#FFFFFF`; secondary text: `#8A8A8A`
- Home shortcuts: 28 sp, light weight, left aligned, generous line spacing
- Drawer list: 18 sp
- Padding: 24 dp horizontal
- Clock: 14 sp, secondary color, top left
- Status bar: transparent; navigation bar: transparent

## 9. Milestones
1. **Setup**: new project, manifest as HOME launcher, blank black screen.
2. **App list**: load and show all apps as text, launch on tap.
3. **Drawer**: search filter, swipe-up open, alphabetical list.
4. **Home**: pinned apps with DataStore persistence.
5. **Gestures**: swipe left/right shortcuts, long press settings.
6. **Hide apps + settings screen.**
7. **Crypto price label**: PriceRepository, top right label in the drawer, settings toggle, caching.
8. **Polish**: theme, fades, edge-to-edge, test on multiple devices.
9. **Release**: R8 shrink, signed APK, publish to GitHub (and F-Droid if desired).

## 10. Testing Checklist
- [ ] Set as default launcher works and survives reboot
- [ ] Install/uninstall updates the list without restart
- [ ] Work profile or clone apps behave sanely
- [ ] Gesture navigation vs 3-button navigation both OK
- [ ] Small screen and large screen layouts
- [ ] APK size stays under about 1 MB
- [ ] Price label hidden and no network calls when the toggle is off
- [ ] Price refresh stops when the drawer closes
- [ ] Offline or failed request shows the cached price without crashing

## 11. Open Questions
- Compose or plain Views (smaller APK)?
- Include the lock-screen gesture (needs extra permission)?
- Show one coin or several at once in the label?
- Light theme at all, or black only?
- License: MIT or GPLv3?
