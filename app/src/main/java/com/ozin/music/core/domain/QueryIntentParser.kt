package com.ozin.music.core.domain

/**
 * Result of parsing a free-text search into a filter this app can actually
 * run. [rules] are AND-combined the same way as a smart playlist's rules
 * (reusing [SmartPlaylistEngine.evaluate] directly - no separate filtering
 * mechanism). [sortByPlayCountDescending] is a hint for phrases like "most
 * played" that describe an ordering rather than a filter.
 */
data class QueryIntent(
    val rules: List<SmartPlaylistRule>,
    val sortByPlayCountDescending: Boolean = false,
    /** Leftover free text once known keywords are stripped out. Rules are
     * AND-combined by [SmartPlaylistEngine], which cannot express "matches
     * title OR artist OR album", so this text is returned separately for the
     * caller to apply as a plain substring search (the same OR-across-fields
     * search the regular Library search box already does) rather than as a
     * rule. Empty when nothing is left over. */
    val remainderText: String = "",
)

/**
 * A local, offline, keyword-based query parser: it maps a handful of known
 * Turkish/English words and phrases onto [SmartPlaylistRule]s reusing the
 * exact rule model the smart-playlist feature already uses, so "Smart
 * search" and smart playlists share one evaluator ([SmartPlaylistEngine]).
 *
 * IMPORTANT: this is a simple keyword mapper, not natural language
 * understanding. It does not parse grammar, negation or combinations beyond
 * "any of these known phrases appeared in the text"; unrecognized words are
 * silently ignored. Never describe this to the user as AI - "smart search"
 * or "keyword search" is accurate, "AI search" would not be.
 */
object QueryIntentParser {

    // Each entry: substrings to look for (checked case-insensitively) -> the
    // rule(s) it contributes. Multiple matching entries are AND-combined,
    // exactly like the rules of a single smart playlist.
    private val moodPhrases: Map<String, Mood> = mapOf(
        "enerjik" to Mood.ENERGETIC, "energetic" to Mood.ENERGETIC, "energy" to Mood.ENERGETIC,
        "sakin" to Mood.CALM, "calm" to Mood.CALM, "relax" to Mood.CALM,
        "hüzünlü" to Mood.SAD, "huzunlu" to Mood.SAD, "sad" to Mood.SAD,
        "mutlu" to Mood.HAPPY, "happy" to Mood.HAPPY,
        "karanlık" to Mood.DARK, "karanlik" to Mood.DARK, "dark" to Mood.DARK,
        "antrenman" to Mood.WORKOUT, "spor" to Mood.WORKOUT, "workout" to Mood.WORKOUT, "gym" to Mood.WORKOUT,
        "gece" to Mood.NIGHT, "night" to Mood.NIGHT,
    )

    fun parse(text: String): QueryIntent {
        val query = text.trim().lowercase()
        if (query.isEmpty()) return QueryIntent(rules = emptyList())

        val rules = mutableListOf<SmartPlaylistRule>()
        var sortByPlayCount = false

        // Favorite.
        if (containsAny(query, "favori", "favorite", "favourites")) {
            rules += SmartPlaylistRule(SmartRuleField.FAVORITE, SmartRuleOperator.EQUALS, "true")
        }

        // Most played -> sort hint, not a hard filter, since "most played"
        // describes an order rather than a threshold.
        if (containsAny(query, "en çok dinlenen", "en cok dinlenen", "most played", "top played")) {
            sortByPlayCount = true
        }

        // Recently added, reusing the exact built-in "Son 30 gün" pattern.
        if (containsAny(query, "son 30 gün", "son 30 gun", "last 30 days", "yeni eklenen", "recently added")) {
            rules += BuiltInSmartPlaylists.recentlyAdded()
        }

        // Mood words -> a MOOD_TAG rule per recognized mood (first match wins
        // per mood so duplicates in the text don't add repeat rules).
        moodPhrases.entries
            .filter { (phrase, _) -> query.contains(phrase) }
            .map { it.value }
            .distinct()
            .forEach { mood ->
                rules += SmartPlaylistRule(SmartRuleField.MOOD_TAG, SmartRuleOperator.CONTAINS, mood.name)
            }

        // "arabada"/"car" has no direct song-field equivalent in this app (it
        // is closer to a Bluetooth/car-mode concept than a per-song
        // attribute), so it intentionally maps to nothing here rather than a
        // made-up rule. It is still consumed as ordinary free text below.

        // Anything left over after removing recognized phrases is treated as
        // a plain free-text search against title/artist/album, the same
        // fields the existing library search already searches.
        var remainder = query
        (moodPhrases.keys + listOf(
            "favori", "favorite", "favourites",
            "en çok dinlenen", "en cok dinlenen", "most played", "top played",
            "son 30 gün", "son 30 gun", "last 30 days", "yeni eklenen", "recently added",
            "arabada", "car",
        )).forEach { phrase -> remainder = remainder.replace(phrase, " ") }
        remainder = remainder.trim()

        return QueryIntent(rules = rules, sortByPlayCountDescending = sortByPlayCount, remainderText = remainder)
    }

    private fun containsAny(text: String, vararg phrases: String): Boolean = phrases.any { text.contains(it) }
}
