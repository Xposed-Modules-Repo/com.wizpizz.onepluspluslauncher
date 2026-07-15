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
        val launcher = context.classForName(SearchFocusController.LAUNCHER_CLASS)
        val launcherState = context.classForName("com.android.launcher3.LauncherState")
        // Reading this static field during onPackageReady initializes LauncherState before
        // LauncherApplication has attached its context. Keep the Field only and read its value
        // after the launcher invokes onStateSetEnd, when LauncherState is already initialized.
        val allAppsState = launcherState.getField("ALL_APPS")
        val stateSetEnd = context.method(
            launcher,
            "onStateSetEnd",
            launcherState,
        )

        context.install(name, stateSetEnd) { chain ->
            val result = chain.proceed()
            if (chain.getArg(0) !== allAppsState.get(null)) return@install result

            val redirectedSearch = SearchFocusController.redirectInProgress.getAndSet(false)
            if (redirectedSearch || context.enabled(FeaturePreferences.SWIPE_UP_AUTOFOCUS)) {
                focusController.postOpenSearch(
                    launcher = chain.thisObject,
                    onFailure = { error -> context.error(name, "could not open search", error) },
                )
            }
            result
        }
    }
}
