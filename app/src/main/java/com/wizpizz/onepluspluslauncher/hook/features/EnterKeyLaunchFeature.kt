package com.wizpizz.onepluspluslauncher.hook.features

import android.content.Intent
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import com.wizpizz.onepluspluslauncher.hook.HookContext
import com.wizpizz.onepluspluslauncher.hook.LauncherFeature
import com.wizpizz.onepluspluslauncher.hook.SearchFocusController
import com.wizpizz.onepluspluslauncher.preferences.FeaturePreferences

object EnterKeyLaunchFeature : LauncherFeature {
    override val name = "Enter-key launch"

    override fun install(context: HookContext) {
        val controller = context.classForName(
            "com.android.launcher3.allapps.search.OplusAllAppsSearchBarController",
        )
        val launcher = context.classForName(SearchFocusController.LAUNCHER_CLASS)
        val allAppsContainer = context.classForName(
            "com.android.launcher3.allapps.BaseAllAppsContainerView",
        )
        val adapterHolder = context.classForName(
            "com.android.launcher3.allapps.BaseAllAppsContainerView\$AdapterHolder",
        )
        val appsList = context.classForName("com.android.launcher3.allapps.AlphabeticalAppsList")
        val adapterItem = context.classForName(
            "com.android.launcher3.allapps.BaseAllAppsAdapter\$AdapterItem",
        )
        val itemInfo = context.classForName("com.android.launcher3.model.data.ItemInfo")
        val initialize = context.method(
            controller,
            "initialize",
            context.classForName("com.android.launcher3.allapps.ActivityAllAppsContainerView"),
            context.classForName("com.android.launcher3.search.SearchAlgorithm"),
            EditText::class.java,
            context.classForName("com.android.launcher3.views.ActivityContext"),
            context.classForName("com.android.launcher3.search.SearchCallback"),
            context.classForName("com.coui.appcompat.searchview.COUISearchBar"),
        )
        val hideKeyboard = context.method(controller, "hideKeyboard")
        val getAppsView = launcher.getMethod("getAppsView")
        val getSearchAdapterHolder = allAppsContainer.getMethod("getSearchAdapterHolder")
        val getAppsList = adapterHolder.getMethod("getAppsListInHolder")
        val getSearchResults = appsList.getMethod("getSearchResults")
        val resultItemInfo = adapterItem.getField("itemInfo")
        val getIntent = itemInfo.getMethod("getIntent")
        val startActivitySafely = context.method(
            launcher,
            "startActivitySafely",
            View::class.java,
            Intent::class.java,
            itemInfo,
        )

        context.install(name, initialize) { chain ->
            val result = chain.proceed()
            val controllerInstance = chain.thisObject
            val input = chain.getArg(2) as EditText
            val launcherInstance = chain.getArg(3)
            input.setOnEditorActionListener { textView, actionId, keyEvent ->
                val actionable = actionId == IME_ACTION_GO ||
                    actionId == IME_ACTION_SEARCH ||
                    actionId == IME_ACTION_DONE ||
                    (keyEvent?.action == KeyEvent.ACTION_DOWN &&
                        (keyEvent.keyCode == KeyEvent.KEYCODE_ENTER ||
                            keyEvent.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER))
                if (!actionable || TextUtils.isEmpty(textView.text)) {
                    false
                } else if (context.enabled(FeaturePreferences.ENTER_KEY_LAUNCH)) {
                    runCatching {
                        val launcherAppsView = getAppsView.invoke(launcherInstance)
                        val searchAdapterHolder = getSearchAdapterHolder.invoke(launcherAppsView)
                        val alphabeticalApps = getAppsList.invoke(searchAdapterHolder)
                        val searchResults = getSearchResults.invoke(alphabeticalApps) as? List<*>
                            ?: return@runCatching false

                        searchResults.firstNotNullOfOrNull { resultItem ->
                            resultItem ?: return@firstNotNullOfOrNull null
                            val launchItem = resultItemInfo.get(resultItem)
                                ?: return@firstNotNullOfOrNull null
                            val intent = getIntent.invoke(launchItem) as? Intent
                                ?: return@firstNotNullOfOrNull null
                            startActivitySafely.invoke(
                                launcherInstance,
                                textView,
                                intent,
                                launchItem,
                            ) as? Boolean
                        } ?: false
                    }.onFailure { error ->
                        context.error(name, "could not launch the first search result", error)
                    }.getOrDefault(false)
                } else {
                    if (keyEvent != null) hideKeyboard.invoke(controllerInstance)
                    false
                }
            }
            result
        }
    }

    private const val IME_ACTION_GO = 2
    private const val IME_ACTION_SEARCH = 3
    private const val IME_ACTION_DONE = 6
}
