package com.ozin.music.core.domain

/**
 * Maps a [Mood] tag (already computed on a [com.ozin.music.core.data.model.Song]
 * by [MoodClassifier] and decoded via [MoodTagCodec]) to whichever existing
 * [EqPresetId] best matches it, for the one-time "try this EQ preset?"
 * suggestion shown on Now Playing (feature 2). This is a small, static,
 * hand-picked table - not a computed/learned mapping - and only ever points
 * at presets that already exist in [EqPresets.referenceTables].
 */
object EqAutoSuggest {

    private val moodToPreset: Map<Mood, EqPresetId> = mapOf(
        Mood.ENERGETIC to EqPresetId.BASS,
        Mood.WORKOUT to EqPresetId.DEEP_BASS,
        Mood.DARK to EqPresetId.ROCK,
        Mood.SAD to EqPresetId.VOCAL,
        Mood.CALM to EqPresetId.VOCAL,
        Mood.NIGHT to EqPresetId.CLASSICAL,
        Mood.HAPPY to EqPresetId.POP,
    )

    /** When a song has more than one mood tag, the first mood in this list
     * that is present wins - workout/energetic (the most audibly distinct
     * presets, bass-heavy) take priority over the softer/ambient ones. */
    private val priority: List<Mood> = listOf(
        Mood.WORKOUT, Mood.ENERGETIC, Mood.DARK, Mood.SAD, Mood.HAPPY, Mood.CALM, Mood.NIGHT,
    )

    /** Returns the (mood, preset) pair to suggest for [moods], or null if
     * none of the tags present has a mapped preset. */
    fun suggestFor(moods: Set<Mood>): Pair<Mood, EqPresetId>? {
        val mood = priority.firstOrNull { it in moods } ?: return null
        val preset = moodToPreset[mood] ?: return null
        return mood to preset
    }
}
