package com.ozin.music.core.domain

import com.ozin.music.core.data.model.Song

/**
 * Evaluates a rule-based smart playlist against the real song library. Rules
 * within one smart playlist are AND-combined, matching the spec's own
 * examples (e.g. "favorite = true AND not played in 60 days").
 */
object SmartPlaylistEngine {

    private const val MS_PER_DAY = 24L * 60L * 60L * 1000L

    fun evaluate(songs: List<Song>, rules: List<SmartPlaylistRule>, nowMs: Long = System.currentTimeMillis()): List<Song> {
        if (rules.isEmpty()) return emptyList()
        return songs.filter { song -> rules.all { rule -> matches(song, rule, nowMs) } }
    }

    fun matches(song: Song, rule: SmartPlaylistRule, nowMs: Long): Boolean = when (rule.field) {
        SmartRuleField.GENRE -> matchesString(song.genre, rule)
        SmartRuleField.ARTIST -> matchesString(song.artist, rule)
        SmartRuleField.ALBUM -> matchesString(song.album, rule)
        SmartRuleField.YEAR -> matchesNumber(song.year.toLong(), rule)
        SmartRuleField.DURATION_MS -> matchesNumber(song.durationMs, rule)
        SmartRuleField.RATING -> matchesNumber(song.rating.toLong(), rule)
        SmartRuleField.PLAY_COUNT -> matchesNumber(song.playCount.toLong(), rule)
        SmartRuleField.FAVORITE -> {
            val expected = rule.value.toBooleanStrictOrNull() ?: false
            song.isFavorite == expected
        }
        SmartRuleField.DATE_ADDED -> matchesRelativeDate(song.dateAdded, rule, nowMs)
        SmartRuleField.DATE_PLAYED -> matchesRelativeDate(song.lastPlayedAt, rule, nowMs)
    }

    private fun matchesString(actual: String, rule: SmartPlaylistRule): Boolean = when (rule.operator) {
        SmartRuleOperator.EQUALS -> actual.equals(rule.value, ignoreCase = true)
        SmartRuleOperator.CONTAINS -> actual.contains(rule.value, ignoreCase = true)
        else -> false
    }

    private fun matchesNumber(actual: Long, rule: SmartPlaylistRule): Boolean {
        val expected = rule.value.toLongOrNull() ?: return false
        return when (rule.operator) {
            SmartRuleOperator.EQUALS -> actual == expected
            SmartRuleOperator.GREATER_OR_EQUAL -> actual >= expected
            SmartRuleOperator.LESS_OR_EQUAL -> actual <= expected
            SmartRuleOperator.CONTAINS -> false
        }
    }

    /** [rule].value is a number of days, relative to [nowMs]. GREATER_OR_EQUAL
     * means "within the last N days", LESS_OR_EQUAL means "more than N days
     * ago (or never)". */
    private fun matchesRelativeDate(actualMs: Long, rule: SmartPlaylistRule, nowMs: Long): Boolean {
        val days = rule.value.toLongOrNull() ?: return false
        val thresholdMs = nowMs - days * MS_PER_DAY
        return when (rule.operator) {
            SmartRuleOperator.GREATER_OR_EQUAL -> actualMs >= thresholdMs
            SmartRuleOperator.LESS_OR_EQUAL -> actualMs <= thresholdMs
            SmartRuleOperator.EQUALS -> actualMs in thresholdMs..(thresholdMs + MS_PER_DAY)
            SmartRuleOperator.CONTAINS -> false
        }
    }
}

/** The three built-in smart playlists, expressed through the same rule
 * engine rather than special-cased queries. */
object BuiltInSmartPlaylists {

    fun recentlyAdded(): List<SmartPlaylistRule> = listOf(
        SmartPlaylistRule(SmartRuleField.DATE_ADDED, SmartRuleOperator.GREATER_OR_EQUAL, "30"),
    )

    fun popularThisWeek(): List<SmartPlaylistRule> = listOf(
        SmartPlaylistRule(SmartRuleField.DATE_PLAYED, SmartRuleOperator.GREATER_OR_EQUAL, "7"),
        SmartPlaylistRule(SmartRuleField.PLAY_COUNT, SmartRuleOperator.GREATER_OR_EQUAL, "5"),
    )

    fun neglectedFavorites(): List<SmartPlaylistRule> = listOf(
        SmartPlaylistRule(SmartRuleField.FAVORITE, SmartRuleOperator.EQUALS, "true"),
        SmartPlaylistRule(SmartRuleField.DATE_PLAYED, SmartRuleOperator.LESS_OR_EQUAL, "30"),
    )

    const val RECENTLY_ADDED_NAME = "Son 30 gün"
    const val POPULAR_THIS_WEEK_NAME = "Bu hafta çok dinlenen"
    const val NEGLECTED_FAVORITES_NAME = "Uzun süredir dinlemediklerim"
}
