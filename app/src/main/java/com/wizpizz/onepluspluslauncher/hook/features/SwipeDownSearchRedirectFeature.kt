package com.wizpizz.onepluspluslauncher.hook.features

import android.os.Bundle
import com.wizpizz.onepluspluslauncher.hook.HookContext
import com.wizpizz.onepluspluslauncher.hook.LauncherFeature
import com.wizpizz.onepluspluslauncher.hook.SearchFocusController
import com.wizpizz.onepluspluslauncher.preferences.FeaturePreferences

object SwipeDownSearchRedirectFeature : LauncherFeature {
    override val name = "Swipe-down search redirect"

    override fun install(context: HookContext) {
        val pullDownController = context.classForName(
            "com.android.launcher.touch.WorkspacePullDownDetectController",
        )
        val launcher = context.classForName(SearchFocusController.LAUNCHER_CLASS)
        val onePlusLauncher = context.classForName("com.android.launcher.Launcher")
        val showSearchBar = context.method(
            pullDownController,
            "showSearchBar",
            onePlusLauncher,
            String::class.java,
            Boolean::class.javaPrimitiveType!!,
            Bundle::class.java,
            Boolean::class.javaPrimitiveType!!,
        )
        val pullCancel = context.method(pullDownController, "pullCancel")
        val showAllApps = context.method(
            launcher,
            "showAllAppsFromIntent",
            Boolean::class.javaPrimitiveType!!,
        )

        context.install(name, showSearchBar) { chain ->
            if (!context.enabled(FeaturePreferences.SWIPE_DOWN_SEARCH_REDIRECT)) {
                return@install chain.proceed()
            }

            val launcherInstance = requireNotNull(chain.getArg(0))
            SearchFocusController.redirectInProgress.set(true)
            try {
                showAllApps.invoke(launcherInstance, true)
            } catch (error: Throwable) {
                SearchFocusController.redirectInProgress.set(false)
                context.error(name, "redirect failed; using the stock action", error)
                return@install chain.proceed()
            }

            runCatching { pullCancel.invoke(requireNotNull(chain.thisObject)) }
                .onFailure { error ->
                    context.error(name, "drawer opened but pull-down cleanup failed", error)
                }
            true
        }
    }
}
