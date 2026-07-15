package com.wizpizz.onepluspluslauncher.hook.features

import android.content.ComponentName
import android.content.Context
import android.os.UserHandle
import com.wizpizz.onepluspluslauncher.hook.HookContext
import com.wizpizz.onepluspluslauncher.hook.LauncherFeature
import com.wizpizz.onepluspluslauncher.preferences.FeaturePreferences
import com.wizpizz.onepluspluslauncher.search.FuzzyRanker
import com.wizpizz.onepluspluslauncher.search.SearchItem
import java.util.ArrayList

object RankedFuzzySearchFeature : LauncherFeature {
    override val name = "Ranked fuzzy search"

    override fun install(context: HookContext) {
        val algorithm = context.classForName(
            "com.android.launcher3.allapps.search.OplusDefaultAppSearchAlgorithm",
        )
        val appInfo = context.classForName("com.android.launcher3.model.data.AppInfo")
        val itemInfo = context.classForName("com.android.launcher3.model.data.ItemInfo")
        val adapterItem = context.classForName(
            "com.android.launcher3.allapps.BaseAllAppsAdapter\$AdapterItem",
        )
        val privacyManager = context.classForName("com.android.launcher.filter.DeepProtectedAppsManager")
        val getTitleMatchResult = context.method(
            algorithm,
            "getTitleMatchResult",
            List::class.java,
            String::class.java,
        )
        val getPrivacyManager = context.method(
            privacyManager,
            "getInstance",
            Context::class.java,
        )
        val isHidden = context.method(
            privacyManager,
            "isAppHiddenEntryAndSupportPrivacyLock",
            itemInfo,
        )
        val asApp = context.method(
            adapterItem,
            "asApp",
            Int::class.javaPrimitiveType!!,
            String::class.java,
            appInfo,
        )
        val appContext = algorithm.getDeclaredField("mContext").apply { isAccessible = true }
        val adapterItemInfo = adapterItem.getField("itemInfo")
        val adapterPosition = adapterItem.getField("position")
        val title = itemInfo.getField("title")
        val user = itemInfo.getField("user")
        val componentName = appInfo.getField("componentName")

        context.install(name, getTitleMatchResult) { chain ->
            val stockResult = chain.proceed()
            if (!context.enabled(FeaturePreferences.RANKED_FUZZY_SEARCH)) {
                stockResult
            } else {
                @Suppress("UNCHECKED_CAST")
                val stock = stockResult as? ArrayList<Any>
                @Suppress("UNCHECKED_CAST")
                val apps = chain.getArg(0) as? List<Any>
                val query = chain.getArg(1) as? String

                if (stock == null || apps == null || query == null || FuzzyRanker.normalize(query).isEmpty()) {
                    stockResult
                } else runCatching {
                    val manager = getPrivacyManager.invoke(null, appContext.get(chain.thisObject))
                    val stockItems = stock.mapIndexed { index, item ->
                        val info = adapterItemInfo.get(item)
                        if (info == null) {
                            SearchItem(
                                identity = "stock:$index",
                                title = "",
                                value = item,
                                sourceOrder = index,
                                stockResult = true,
                            )
                        } else {
                            runtimeItem(info, item, index, true, title, user, componentName)
                                ?: SearchItem(
                                    identity = "stock:$index",
                                    title = "",
                                    value = item,
                                    sourceOrder = index,
                                    stockResult = true,
                                )
                        }
                    }
                    val candidates = apps.mapIndexedNotNull { index, info ->
                        if (isHidden.invoke(manager, info) as Boolean) return@mapIndexedNotNull null
                        val item = asApp.invoke(null, index, "", info) ?: return@mapIndexedNotNull null
                        runtimeItem(info, item, index, false, title, user, componentName)
                    }
                    val ranked = FuzzyRanker.mergeAndRank(stockItems, candidates, query)
                    ranked.forEachIndexed { index, item -> adapterPosition.setInt(item, index) }
                    ArrayList(ranked)
                }.getOrElse { error ->
                    context.error(name, "ranking failed; returning stock results", error)
                    stockResult
                }
            }
        }
    }

    private fun runtimeItem(
        info: Any,
        adapterItem: Any,
        order: Int,
        stock: Boolean,
        titleField: java.lang.reflect.Field,
        userField: java.lang.reflect.Field,
        componentField: java.lang.reflect.Field,
    ): SearchItem<Any>? {
        val component = componentField.get(info) as? ComponentName ?: return null
        val user = userField.get(info) as? UserHandle ?: return null
        val title = titleField.get(info)?.toString().orEmpty()
        return SearchItem(
            identity = "${component.flattenToString()}:${user.hashCode()}",
            title = title,
            value = adapterItem,
            sourceOrder = order,
            stockResult = stock,
        )
    }
}
