package com.ozin.music

import com.ozin.music.core.domain.FolderExclusionSuggestions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FolderExclusionSuggestionsTest {

    @Test
    fun `flags known voice-note folder names`() {
        val folders = listOf(
            "/storage/emulated/0/WhatsApp/Media/WhatsApp Audio",
            "/storage/emulated/0/Telegram/Telegram Audio",
            "/storage/emulated/0/Music/MyBand",
        )
        val suggestions = FolderExclusionSuggestions.suggestedFolders(folders)
        assertTrue(suggestions.any { it.contains("WhatsApp Audio") })
        assertTrue(suggestions.any { it.contains("Telegram") })
        assertTrue(suggestions.none { it.contains("MyBand") })
    }

    @Test
    fun `is case-insensitive`() {
        val suggestions = FolderExclusionSuggestions.suggestedFolders(listOf("/sdcard/VOICE RECORDER"))
        assertEquals(1, suggestions.size)
    }

    @Test
    fun `excludes already-excluded folders from suggestions`() {
        val folders = listOf("/sdcard/Voice Notes")
        val suggestions = FolderExclusionSuggestions.suggestedFolders(folders, alreadyExcluded = setOf("/sdcard/Voice Notes"))
        assertTrue(suggestions.isEmpty())
    }

    @Test
    fun `folders with no suspicious keyword are never suggested`() {
        val suggestions = FolderExclusionSuggestions.suggestedFolders(listOf("/sdcard/Music", "/sdcard/Podcasts"))
        assertTrue(suggestions.isEmpty())
    }
}
