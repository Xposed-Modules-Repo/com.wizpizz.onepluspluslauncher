package com.wizpizz.onepluspluslauncher.hook

import android.util.Log
import com.wizpizz.onepluspluslauncher.LAUNCHER_PACKAGE
import com.wizpizz.onepluspluslauncher.hook.features.EnterKeyLaunchFeature
import com.wizpizz.onepluspluslauncher.hook.features.GlobalSearchRedirectFeature
import com.wizpizz.onepluspluslauncher.hook.features.RankedFuzzySearchFeature
import com.wizpizz.onepluspluslauncher.hook.features.SwipeUpAutofocusFeature
import com.wizpizz.onepluspluslauncher.preferences.FeaturePreferences
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import java.util.concurrent.atomic.AtomicBoolean

/** Modern libxposed entry point, loaded only in the OnePlus launcher scope. */
class HookEntry : XposedModule() {
    private val initialized = AtomicBoolean(false)
    private var processName: String? = null

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        processName = param.processName
    }

    override fun onPackageReady(param: PackageReadyParam) {
        if (param.packageName != LAUNCHER_PACKAGE ||
            processName != LAUNCHER_PACKAGE ||
            !initialized.compareAndSet(false, true)
        ) return

        val context = HookContext(
            module = this,
            classLoader = param.classLoader,
            preferences = getRemotePreferences(FeaturePreferences.GROUP),
        )
        val features = mutableListOf<LauncherFeature>(
            EnterKeyLaunchFeature,
            RankedFuzzySearchFeature,
        )
        runCatching { SearchFocusController(param.classLoader) }
            .onSuccess { focusController ->
                features += SwipeUpAutofocusFeature(focusController)
                features += GlobalSearchRedirectFeature(focusController)
            }
            .onFailure { error ->
                log(Log.ERROR, TAG, "Focus-dependent features could not be initialized", error)
            }

        features.forEach { feature ->
            runCatching { feature.install(context) }
                .onFailure { error -> context.error(feature.name, "initialization failed", error) }
        }
    }

    private companion object {
        const val TAG = "OnePlusPlusLauncher"
    }
}
