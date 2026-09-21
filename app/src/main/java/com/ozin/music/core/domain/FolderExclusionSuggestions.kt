package com.ozin.music.core.domain

/**
 * Pure heuristic that flags scanned folders which commonly hold
 * non-music voice recordings/voice notes, so the Folders screen can show a
 * dismissible "exclude this?" suggestion banner. Never excludes anything by
 * itself — purely advisory.
 */
object FolderExclusionSuggestions {

    private val suspiciousNameFragments = listOf(
        "whatsapp audio",
        "whatsapp voice notes",
        "telegram",
        "voice recorder",
        "voice notes",
        "call recordings",
        "callrecordings",
        "recordings",
    )

    fun suggestedFolders(folderPaths: List<String>, alreadyExcluded: Set<String> = emptySet()): List<String> =
        folderPaths
            .filter { it !in alreadyExcluded }
            .filter { path ->
                val lower = path.lowercase()
                suspiciousNameFragments.any { lower.contains(it) }
            }
            .distinct()
}
