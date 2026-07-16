<div align="center">

# OnePlusPlusLauncher

### Focused LSPosed enhancements for OnePlus System Launcher

</div>

OnePlusPlusLauncher is a small, open-source libxposed module for improving app-drawer search on OxygenOS. The current source intentionally supports only OnePlus System Launcher **16.6.9** (`160060009`) on Android 13 or newer.

## Features

- **Swipe-up search autofocus** — focuses app-drawer search and opens the keyboard automatically.
- **Enter-key launch** — launches the highlighted result from the keyboard's Search or Go action.
- **Ranked fuzzy search** — preserves OnePlus pinyin, alias, DMP, multi-user, and privacy-aware results while adding typo-tolerant title matches.
- **Global-search-button redirect** — opens focused app-drawer search instead of the external global-search application.
- **Swipe-down search redirect** — opens focused app-drawer search when the home-screen swipe-down action targets Global Search.

Every feature has an independent toggle in a single Material 3 configuration screen and defaults to enabled.

## Requirements

- OnePlus System Launcher **16.6.9**
- Android 13+
- Root with an API-101-compatible LSPosed implementation

Compatibility with older launcher releases is not maintained. Launcher updates may change internal classes or methods and require a module update.

## Installation

1. Install the module APK.
2. Enable OnePlusPlusLauncher in LSPosed.
3. Select **System Launcher** (`com.android.launcher`) in its scope.
4. Restart System Launcher.
5. Open OnePlusPlusLauncher to configure the five features.

The **Ready** status confirms that LSPosed is reachable and System Launcher is selected. It cannot directly prove that the manager toggle is enabled or that the launcher process has loaded the hooks.

## Architecture

- The configuration app is one Jetpack Compose Material 3 screen.
- libxposed remote preferences are the only settings store.
- `HookEntry` installs exact, fail-closed reflection hooks in the launcher process.
- The fuzzy ranker is pure Kotlin and unit tested independently from Android and the launcher.
- Decompiled launcher sources and APKs stay local under the ignored `decompiled/` directory.

See [Hook maintenance](https://github.com/wizpizz/OnePlusPlusLauncher/blob/master/docs/HOOK_MAINTENANCE.md) for the current target descriptors and adaptation workflow.

## Building

Use JDK 17 and run:

```shell
./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease
```

Local release builds are unsigned when signing credentials are unavailable. CI accepts the following secrets for signed releases:

- `SIGNING_KEY_STORE_BASE64`
- `SIGNING_KEY_ALIAS`
- `SIGNING_KEY_STORE_PASSWORD`
- `SIGNING_KEY_PASSWORD`

## Disclaimer

This project is not affiliated with or endorsed by OnePlus, OPPO, or Google. It modifies another application at runtime; use it at your own risk.

## License

Licensed under the terms in the [project license](https://github.com/wizpizz/OnePlusPlusLauncher/blob/master/LICENSE).
