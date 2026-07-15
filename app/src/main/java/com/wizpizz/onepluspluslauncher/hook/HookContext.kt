package com.wizpizz.onepluspluslauncher.hook

import android.content.SharedPreferences
import android.util.Log
import com.wizpizz.onepluspluslauncher.BuildConfig
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Method

class HookContext(
    val module: XposedModule,
    val classLoader: ClassLoader,
    private val preferences: SharedPreferences,
) {
    fun classForName(name: String): Class<*> = Class.forName(name, false, classLoader)

    fun method(owner: Class<*>, name: String, vararg parameterTypes: Class<*>): Method =
        owner.getDeclaredMethod(name, *parameterTypes).apply { isAccessible = true }

    fun enabled(key: String): Boolean =
        preferences.getBoolean(key, com.wizpizz.onepluspluslauncher.preferences.FeaturePreferences.DEFAULT_ENABLED)

    fun install(feature: String, method: Method, interceptor: XposedInterface.Hooker) {
        module.hook(method)
            .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
            .intercept(interceptor)
        module.log(Log.INFO, TAG, "$feature: installed ${method.toGenericString()}")
    }

    fun error(feature: String, message: String, error: Throwable? = null) {
        if (error == null) module.log(Log.ERROR, TAG, "$feature: $message")
        else module.log(Log.ERROR, TAG, "$feature: $message", error)
    }

    fun debug(feature: String, message: String) {
        if (BuildConfig.DEBUG) module.log(Log.DEBUG, TAG, "$feature: $message")
    }

    private companion object {
        const val TAG = "OnePlusPlusLauncher"
    }
}
