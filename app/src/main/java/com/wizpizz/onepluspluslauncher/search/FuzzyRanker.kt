package com.wizpizz.onepluspluslauncher.search

import me.xdrop.fuzzywuzzy.FuzzySearch
import java.util.Locale
import kotlin.math.roundToInt

data class SearchItem<T>(
    val identity: String,
    val title: String,
    val value: T,
    val sourceOrder: Int,
    val stockResult: Boolean,
    val eligible: Boolean = true,
)

object FuzzyRanker {
    const val MATCH_THRESHOLD = 50
    private const val PREFIX_MULTIPLIER = 1.5
    private const val SUBSTRING_MULTIPLIER = 1.3
    private const val SUBSEQUENCE_MULTIPLIER = 1.1

    fun normalize(value: String): String = buildString(value.length) {
        value.lowercase(Locale.ROOT).forEach { character ->
            if (!character.isWhitespace() && character != '\'') append(character)
        }
    }

    fun score(title: String, query: String): Int {
        val normalizedTitle = normalize(title)
        val normalizedQuery = normalize(query)
        if (normalizedTitle.isEmpty() || normalizedQuery.isEmpty()) return 0

        val base = FuzzySearch.weightedRatio(normalizedTitle, normalizedQuery)
        val multiplier = when {
            normalizedTitle.startsWith(normalizedQuery) -> PREFIX_MULTIPLIER
            normalizedTitle.contains(normalizedQuery) -> SUBSTRING_MULTIPLIER
            normalizedQuery.isSubsequenceOf(normalizedTitle) -> SUBSEQUENCE_MULTIPLIER
            else -> 1.0
        }
        return (base * multiplier).roundToInt()
    }

    fun <T> mergeAndRank(
        stockResults: List<SearchItem<T>>,
        candidates: List<SearchItem<T>>,
        query: String,
    ): List<T> {
        val unique = LinkedHashMap<String, RankedItem<T>>()

        stockResults.forEach { item ->
            unique.putIfAbsent(
                item.identity,
                RankedItem(item, score(item.title, query).coerceAtLeast(MATCH_THRESHOLD)),
            )
        }
        candidates.forEach { item ->
            if (!item.eligible) return@forEach
            if (item.identity in unique) return@forEach
            val score = score(item.title, query)
            if (score >= MATCH_THRESHOLD) unique[item.identity] = RankedItem(item, score)
        }

        return unique.values
            .sortedWith(
                compareByDescending<RankedItem<T>> { it.score }
                    .thenByDescending { it.item.stockResult }
                    .thenBy { it.item.sourceOrder }
                    .thenBy { it.item.title.lowercase(Locale.ROOT) },
            )
            .map { it.item.value }
    }

    private fun String.isSubsequenceOf(text: String): Boolean {
        var index = 0
        text.forEach { character ->
            if (index < length && this[index] == character) index++
        }
        return index == length
    }

    private data class RankedItem<T>(val item: SearchItem<T>, val score: Int)
}
