package com.wizpizz.onepluspluslauncher.hook

interface LauncherFeature {
    val name: String
    fun install(context: HookContext)
}
