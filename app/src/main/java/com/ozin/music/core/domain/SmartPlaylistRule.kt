package com.ozin.music.core.domain

/** Fields a smart-playlist rule can filter on. */
enum class SmartRuleField {
    GENRE, ARTIST, ALBUM, YEAR, DURATION_MS, RATING, FAVORITE, PLAY_COUNT, DATE_ADDED, DATE_PLAYED,
    /** Matches against [com.ozin.music.core.data.model.Song.moodTags] (a
     * comma-separated list of heuristic [Mood] names); use CONTAINS with a
     * mood name such as "ENERGETIC". */
    MOOD_TAG,
}

/** Comparison operators. Not every operator is meaningful for every field
 * (see [SmartPlaylistEngine]); string fields use EQUALS/CONTAINS, numeric and
 * date fields use EQUALS/GREATER_OR_EQUAL/LESS_OR_EQUAL, favorite uses EQUALS. */
enum class SmartRuleOperator { EQUALS, CONTAINS, GREATER_OR_EQUAL, LESS_OR_EQUAL }

/**
 * One AND-combined condition of a smart playlist. [value] is a raw string;
 * for DATE_ADDED/DATE_PLAYED it holds a number of days (relative to "now" at
 * evaluation time), for FAVORITE it holds "true"/"false", for other numeric
 * fields a plain integer/long, and for string fields free text.
 */
data class SmartPlaylistRule(
    val field: SmartRuleField,
    val operator: SmartRuleOperator,
    val value: String,
)

/**
 * Hand-rolled, dependency-free serialization for a rule list into a single
 * String column (avoids pulling in a JSON library just for this). Format:
 * rules joined by ";;", each rule's three parts joined by "::", with a
 * backslash-escape for any literal occurrence of those separators inside a
 * value.
 */
object SmartPlaylistRuleCodec {
    private const val RULE_SEP = ";;"
    private const val FIELD_SEP = "::"

    fun encode(rules: List<SmartPlaylistRule>): String =
        rules.joinToString(RULE_SEP) { rule ->
            listOf(rule.field.name, rule.operator.name, escape(rule.value)).joinToString(FIELD_SEP)
        }

    fun decode(raw: String): List<SmartPlaylistRule> {
        if (raw.isBlank()) return emptyList()
        return raw.split(RULE_SEP).mapNotNull { chunk ->
            if (chunk.isBlank()) return@mapNotNull null
            val parts = chunk.split(FIELD_SEP, limit = 3)
            if (parts.size != 3) return@mapNotNull null
            val field = runCatching { SmartRuleField.valueOf(parts[0]) }.getOrNull() ?: return@mapNotNull null
            val operator = runCatching { SmartRuleOperator.valueOf(parts[1]) }.getOrNull() ?: return@mapNotNull null
            SmartPlaylistRule(field, operator, unescape(parts[2]))
        }
    }

    private fun escape(value: String): String =
        value.replace("\\", "\\\\").replace(FIELD_SEP, "\\c").replace(RULE_SEP, "\\s")

    private fun unescape(value: String): String =
        value.replace("\\s", RULE_SEP).replace("\\c", FIELD_SEP).replace("\\\\", "\\")
}
