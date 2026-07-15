package com.wizpizz.onepluspluslauncher.hook.features

import android.content.Intent
import com.wizpizz.onepluspluslauncher.hook.HookContext
import com.wizpizz.onepluspluslauncher.hook.LauncherFeature
import com.wizpizz.onepluspluslauncher.hook.SearchFocusController
import com.wizpizz.onepluspluslauncher.preferences.FeaturePreferences

class GlobalSearchRedirectFeature(
    private val focusController: SearchFocusController,
) : LauncherFeature {
    override val name = "Global-search redirect"

    override fun install(context: HookContext) {
        val indicatorEntry = context.classForName("com.android.launcher3.search.IndicatorEntry")
        val launcher = context.classForName(SearchFocusController.LAUNCHER_CLASS)
        val launcherState = context.classForName("com.android.launcher3.LauncherState")
        // Resolve the value only inside live launcher callbacks to avoid initializing
        // LauncherState before LauncherApplication has attached its context.
        val allAppsState = launcherState.getField("ALL_APPS")
        val startIndicatorApp = context.method(
            indicatorEntry,
            "startIndicatorApp",
            Intent::class.java,
        )
        val showAllApps = context.method(
            launcher,
            "showAllAppsFromIntent",
            Boolean::class.javaPrimitiveType!!,
        )
        val stateSetStart = context.method(
            launcher,
            "onStateSetStart",
            launcherState,
            launcherState,
        )
        val launcherField = indicatorEntry.getDeclaredField("mLauncher").apply { isAccessible = true }

        context.install(name, stateSetStart) { chain ->
            val result = chain.proceed()
            if (chain.getArg(1) !== allAppsState.get(null) ||
                !SearchFocusController.redirectInProgress.getAndSet(false)
            ) return@install result

            focusController.postOpenSearch(
                launcher = requireNotNull(chain.thisObject),
                onFailure = { error -> context.error(name, "could not open search", error) },
            )
            result
        }

        context.install(name, startIndicatorApp) { chain ->
            if (!context.enabled(FeaturePreferences.GLOBAL_SEARCH_REDIRECT)) {
                chain.proceed()
            } else {
                val launcherInstance = requireNotNull(
                    launcherField.get(requireNotNull(chain.thisObject)),
                )
                SearchFocusController.redirectInProgress.set(true)
                try {
                    showAllApps.invoke(launcherInstance, true)
                    false
                } catch (error: Throwable) {
                    SearchFocusController.redirectInProgress.set(false)
                    context.error(name, "redirect failed; using the stock action", error)
                    chain.proceed()
                }
            }
        }
    }
}
