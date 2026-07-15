# Hook maintenance

## Current compatibility fixture

- Application: OnePlus System Launcher
- Package: `com.android.launcher`
- Version: `16.6.9` (`160060009`)
- Minimum/target SDK reported by the APK: 33/36
- APK SHA-256: `d0b9afa99a0e80e496b1297760ac7cb64ffc0b3a259636f6bc8eb1182b65935f`

The production implementation supports this fixture only. Exact class and method names are intentional because this launcher build retains descriptive symbols. If a later release obfuscates these targets or produces ambiguous alternatives, reassess whether DexKit is justified rather than adding broad fallback reflection.

## Target descriptors

### Swipe-up autofocus

```text
com.android.launcher3.Launcher.onStateSetStart(
    com.android.launcher3.LauncherState,
    com.android.launcher3.LauncherState
): void
```

The second argument is the destination state. Focus only when it equals `LauncherState.ALL_APPS`. The supported focus path is:

```text
Launcher.getAppsView()
ActivityAllAppsContainerView.getSearchUiManager()
LauncherTaskbarAppsSearchContainerLayout.showKeyboard()
```

### Enter-key launch

Install the editor listener after:

```text
com.android.launcher3.allapps.search.OplusAllAppsSearchBarController.initialize(
    ActivityAllAppsContainerView,
    SearchAlgorithm,
    EditText,
    ActivityContext,
    SearchCallback,
    COUISearchBar
): void
```

When enabled, delegate Search/Go actions to:

```text
com.android.launcher3.allapps.search.AllAppsSearchBarController.onEditorAction(
    TextView,
    int,
    KeyEvent
): boolean
```

This preserves the launcher's own statistics and `launchHighlightedItem()` path.

### Ranked fuzzy search

```text
com.android.launcher3.allapps.search.OplusDefaultAppSearchAlgorithm.getTitleMatchResult(
    java.util.List,
    java.lang.String
): java.util.ArrayList
```

Always run the original method first. Preserve its DMP, pinyin, alias, and multi-app results. Newly added title matches must be checked with:

```text
com.android.launcher.filter.DeepProtectedAppsManager
    .isAppHiddenEntryAndSupportPrivacyLock(ItemInfo): boolean
```

Create additional results only through:

```text
BaseAllAppsAdapter.AdapterItem.asApp(int, String, AppInfo)
```

Never rebuild the entire search list without OnePlus's privacy filter.

### Global-search-button redirect

```text
com.android.launcher3.search.IndicatorEntry.startIndicatorApp(Intent): boolean
```

Read its exact private `mLauncher` field, call:

```text
com.android.launcher3.Launcher.showAllAppsFromIntent(boolean): void
```

and return `false` after a successful redirect. If reflection or redirection fails, proceed with the original launcher action.

## Updating for another launcher release

1. Put the APK under the ignored `decompiled/` directory and record its version and SHA-256.
2. Decompile it with JADX and inspect all four descriptors above before editing production code.
3. Trace callers and side effects, not just matching method names. Prefer the launcher's public behavior over manually recreating it.
4. Update only the affected feature and add or adjust pure unit fixtures where applicable.
5. Build the debug APK and confirm each feature reports exactly one installed hook.
6. Restart System Launcher and validate one feature at a time with its toggle enabled and disabled.
7. Use user-led checks for visual behavior; use ADB for logs, process state, and deterministic verification.

Do not commit launcher APKs, decompiled sources, mappings, or device logs.
