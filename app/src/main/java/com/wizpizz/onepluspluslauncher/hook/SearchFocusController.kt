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

    fun postOpenSearch(
        launcher: Any,
        onFailure: (Throwable) -> Unit = {},
    ): Boolean {
        val appsView = getAppsView.invoke(launcher) as? View ?: return false
        return appsView.post {
            try {
                val searchUiManager = getSearchUiManager.invoke(appsView)
                onSearchBarClick.invoke(searchUiManager)
            } catch (error: Throwable) {
                onFailure(error)
            }
        }
    }

    companion object {
        const val LAUNCHER_CLASS = "com.android.launcher3.Launcher"
        val redirectInProgress = AtomicBoolean(false)
    }
}
