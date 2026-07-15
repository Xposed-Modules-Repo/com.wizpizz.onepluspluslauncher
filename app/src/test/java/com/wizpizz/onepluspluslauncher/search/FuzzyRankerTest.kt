package com.wizpizz.onepluspluslauncher.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FuzzyRankerTest {
    @Test
    fun normalizationRemovesImeSeparators() {
        assertEquals("oneplus", FuzzyRanker.normalize(" One' Plus "))
    }

    @Test
    fun usefulMatchTypesScoreAboveThreshold() {
        assertTrue(FuzzyRanker.score("Camera", "camera") >= FuzzyRanker.MATCH_THRESHOLD)
        assertTrue(FuzzyRanker.score("Calculator", "calc") >= FuzzyRanker.MATCH_THRESHOLD)
        assertTrue(FuzzyRanker.score("Google Maps", "maps") >= FuzzyRanker.MATCH_THRESHOLD)
        assertTrue(FuzzyRanker.score("OnePlus Store", "opstr") >= FuzzyRanker.MATCH_THRESHOLD)
        assertTrue(FuzzyRanker.score("Calendar", "calndar") >= FuzzyRanker.MATCH_THRESHOLD)
    }

    @Test
    fun unrelatedTitleDoesNotMatch() {
        assertFalse(FuzzyRanker.score("Camera", "weather") >= FuzzyRanker.MATCH_THRESHOLD)
    }

    @Test
    fun typoMatchesAreAddedAndRanked() {
        val ranked = FuzzyRanker.mergeAndRank(
            stockResults = listOf(item("calendar", "Calendar", stock = true, order = 0)),
            candidates = listOf(
                item("calculator", "Calculator", order = 0),
                item("camera", "Camera", order = 1),
            ),
            query = "calclator",
        )

        assertEquals(listOf("Calculator", "Calendar"), ranked)
    }

    @Test
    fun stockAliasOrPinyinResultIsPreservedWithoutTitleMatch() {
        val ranked = FuzzyRanker.mergeAndRank(
            stockResults = listOf(item("weather", "Weather", stock = true, order = 0)),
            candidates = emptyList(),
            query = "tianqi",
        )

        assertEquals(listOf("Weather"), ranked)
    }

    @Test
    fun duplicatesAreRemovedAndDifferentUsersRemainIndependent() {
        val ranked = FuzzyRanker.mergeAndRank(
            stockResults = listOf(item("camera:0", "Camera", stock = true, order = 0)),
            candidates = listOf(
                item("camera:0", "Camera", order = 0),
                item("camera:10", "Camera", order = 1),
                item("hidden:0", "Hidden Camera", order = 2, eligible = false),
            ),
            query = "camera",
        )

        assertEquals(listOf("Camera", "Camera"), ranked)
    }

    @Test
    fun stableSourceOrderBreaksEqualScoreTies() {
        val ranked = FuzzyRanker.mergeAndRank(
            stockResults = emptyList(),
            candidates = listOf(
                SearchItem("second", "Clock", "second", 1, false),
                SearchItem("first", "Clock", "first", 0, false),
            ),
            query = "clock",
        )

        assertEquals(listOf("first", "second"), ranked)
    }

    private fun item(
        identity: String,
        title: String,
        stock: Boolean = false,
        order: Int,
        eligible: Boolean = true,
    ) = SearchItem(
        identity = identity,
        title = title,
        value = title,
        sourceOrder = order,
        stockResult = stock,
        eligible = eligible,
    )
}
