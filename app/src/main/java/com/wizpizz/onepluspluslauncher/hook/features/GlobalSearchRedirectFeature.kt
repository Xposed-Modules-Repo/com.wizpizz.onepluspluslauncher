package com.wizpizz.onepluspluslauncher.hook.features

import android.content.Intent
import com.wizpizz.onepluspluslauncher.hook.HookContext
import com.wizpizz.onepluspluslauncher.hook.LauncherFeature
import com.wizpizz.onepluspluslauncher.hook.SearchFocusController
import com.wizpizz.onepluspluslauncher.preferences.FeaturePreferences

class GlobalSearchRedirectFeature : LauncherFeature {
    override val name = "Global-search redirect"

    override fun install(context: HookContext) {
        val indicatorEntry = context.classForName("com.android.launcher3.search.IndicatorEntry")
        val launcher = context.classForName(SearchFocusController.LAUNCHER_CLASS)
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
        val launcherField = indicatorEntry.getDeclaredField("mLauncher").apply { isAccessible = true }

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
