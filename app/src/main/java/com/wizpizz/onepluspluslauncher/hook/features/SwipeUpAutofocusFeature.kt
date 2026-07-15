package com.wizpizz.onepluspluslauncher.hook.features

import com.wizpizz.onepluspluslauncher.hook.HookContext
import com.wizpizz.onepluspluslauncher.hook.LauncherFeature
import com.wizpizz.onepluspluslauncher.hook.SearchFocusController
import com.wizpizz.onepluspluslauncher.preferences.FeaturePreferences

class SwipeUpAutofocusFeature(
    private val focusController: SearchFocusController,
) : LauncherFeature {
    override val name = "Swipe-up autofocus"

    override fun install(context: HookContext) {
        val launcherState = context.classForName("com.android.launcher3.LauncherState")
        val touchController = context.classForName(
            "com.android.launcher3.touch.OplusAbstractStateChangeTouchController",
        )
        val baseTouchController = context.classForName(
            "com.android.launcher3.touch.AbstractStateChangeTouchController",
        )
        // Reading this static field during onPackageReady initializes LauncherState before
        // LauncherApplication has attached its context. Keep the Field only and read its value
        // after a gesture callback, when LauncherState is already initialized.
        val allAppsState = launcherState.getField("ALL_APPS")
        val updateFinalState = context.method(
            touchController,
            "onDragEndUpdateStateInject",
            launcherState,
        )
        val launcherField = baseTouchController.getDeclaredField("mLauncher").apply {
            isAccessible = true
        }

        context.install(name, updateFinalState) { chain ->
            val result = chain.proceed()
            if (chain.getArg(0) !== allAppsState.get(null)) return@install result
            if (!context.enabled(FeaturePreferences.SWIPE_UP_AUTOFOCUS)) return@install result

            val launcher = requireNotNull(
                launcherField.get(requireNotNull(chain.thisObject)),
            )
            focusController.postOpenSearch(
                launcher = launcher,
                onFailure = { error -> context.error(name, "could not open search", error) },
            )
            result
        }
    }
}
