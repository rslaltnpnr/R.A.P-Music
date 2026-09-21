package com.ozin.music

import androidx.compose.ui.graphics.Color
import com.ozin.music.core.ui.theme.AccentColorOption
import com.ozin.music.core.ui.theme.ThemePreset
import com.ozin.music.core.ui.theme.colorSchemeFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ThemePresetTest {

    @Test
    fun `accent color is threaded into the scheme's primary color`() {
        ThemePreset.entries.forEach { preset ->
            val scheme = colorSchemeFor(preset, AccentColorOption.TEAL.color)
            assertEquals(AccentColorOption.TEAL.color, scheme.primary)
        }
    }

    @Test
    fun `amoled preset uses true black background and surface`() {
        val scheme = colorSchemeFor(ThemePreset.AMOLED, AccentColorOption.PURPLE.color)
        assertEquals(Color(0xFF000000), scheme.background)
        assertEquals(Color(0xFF000000), scheme.surface)
    }

    @Test
    fun `presets produce visually distinct backgrounds`() {
        val backgrounds = ThemePreset.entries.map { colorSchemeFor(it, AccentColorOption.PURPLE.color).background }
        assertEquals(backgrounds.size, backgrounds.toSet().size)
    }

    @Test
    fun `default dark preset is unchanged from the original theme`() {
        val scheme = colorSchemeFor(ThemePreset.DEFAULT_DARK, AccentColorOption.PURPLE.color)
        assertEquals(Color(0xFF0B0F19), scheme.background)
        assertNotEquals(Color(0xFF000000), scheme.background)
    }
}
