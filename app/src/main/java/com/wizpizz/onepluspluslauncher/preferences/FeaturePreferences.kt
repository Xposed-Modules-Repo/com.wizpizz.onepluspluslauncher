package com.wizpizz.onepluspluslauncher.preferences

object FeaturePreferences {
    const val GROUP = "launcher_features"
    const val SWIPE_UP_AUTOFOCUS = "swipe_up_autofocus_enabled"
    const val ENTER_KEY_LAUNCH = "enter_key_launch_enabled"
    const val RANKED_FUZZY_SEARCH = "ranked_fuzzy_search_enabled"
    const val GLOBAL_SEARCH_REDIRECT = "global_search_redirect_enabled"
    const val DEFAULT_ENABLED = true

    val keys = setOf(
        SWIPE_UP_AUTOFOCUS,
        ENTER_KEY_LAUNCH,
        RANKED_FUZZY_SEARCH,
        GLOBAL_SEARCH_REDIRECT,
    )
}
