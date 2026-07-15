package com.wizpizz.onepluspluslauncher.hook

import android.annotation.SuppressLint
import android.view.View
import java.util.concurrent.atomic.AtomicBoolean

@SuppressLint("PrivateApi")
class SearchFocusController(classLoader: ClassLoader) {
    private val launcherClass = Class.forName(LAUNCHER_CLASS, false, classLoader)
    private val getAppsView = launcherClass.getMethod("getAppsView")
    private val allAppsContainerClass = Class.forName(
        "com.android.launcher3.allapps.ActivityAllAppsContainerView",
        false,
        classLoader,
    )
    private val getSearchUiManager = allAppsContainerClass.getMethod("getSearchUiManager")
    private val searchUiManagerClass = Class.forName(
        "com.android.launcher3.allapps.search.LauncherTaskbarAppsSearchContainerLayout",
        false,
        classLoader,
    )
    private val onSearchBarClick = searchUiManagerClass.getMethod("onSearchBarClick")
    private val imeControllable = Class.forName(
        "com.android.launcher3.allapps.search.LauncherAppsSearchContainerLayout",
        false,
        classLoader,
    ).getDeclaredField("imeControllable").apply {
        isAccessible = true
    }

    fun postOpenSearch(
        launcher: Any,
        onFailure: (Throwable) -> Unit = {},
    ): Boolean {
        val appsView = getAppsView.invoke(launcher) as? View ?: return false
        return appsView.post {
            try {
                val searchUiManager = checkNotNull(getSearchUiManager.invoke(appsView)) {
                    "Launcher search UI manager is unavailable"
                }
                openSearchWithSystemImeAnimation(searchUiManager)
            } catch (error: Throwable) {
                onFailure(error)
            }
        }
    }

    /**
     * OnePlus normally takes manual control of the IME and drives a deliberately slow spring
     * animation. Its own non-controllable fallback still enters search mode correctly, but lets
     * Android show the keyboard normally. Select that fallback only for module-triggered opens;
     * restoring the field immediately leaves ordinary launcher interactions unchanged.
     */
    private fun openSearchWithSystemImeAnimation(searchUiManager: Any) {
        val wasImeControllable = imeControllable.getBoolean(searchUiManager)
        try {
            imeControllable.setBoolean(searchUiManager, false)
            onSearchBarClick.invoke(searchUiManager)
        } finally {
            imeControllable.setBoolean(searchUiManager, wasImeControllable)
        }
    }

    companion object {
        const val LAUNCHER_CLASS = "com.android.launcher3.Launcher"
        val redirectInProgress = AtomicBoolean(false)
    }
}
