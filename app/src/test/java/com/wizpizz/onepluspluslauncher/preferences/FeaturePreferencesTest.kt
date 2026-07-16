package com.wizpizz.onepluspluslauncher.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeaturePreferencesTest {
    @Test
    fun allFiveFeaturesDefaultToEnabled() {
        assertTrue(FeaturePreferences.DEFAULT_ENABLED)
        assertEquals(5, FeaturePreferences.keys.size)
    }

    @Test
    fun preferenceKeysAreStableAndIndependent() {
        assertEquals(
            setOf(
                "swipe_up_autofocus_enabled",
                "enter_key_launch_enabled",
                "ranked_fuzzy_search_enabled",
                "global_search_redirect_enabled",
                "swipe_down_search_redirect_enabled",
            ),
            FeaturePreferences.keys,
        )
    }
}
