package com.ozin.music.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ozin.music.core.ui.theme.Spacing

/**
 * Consistent section-title styling, used above a [SettingsCard] group or a
 * carousel on Home. Replaces bare [Text] calls with ad hoc styles.
 */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.padding(bottom = Spacing.xs),
    )
}

/**
 * A rounded, translucent "glassmorphism" container used to group related
 * settings rows/controls so a screen reads as distinct sections instead of
 * one flat wall of controls stacked directly on the background. Meant to sit
 * on top of a non-flat (gradient/ambient) background so the semi-transparent
 * fill actually reads as "glass" rather than a plain dim box. This is the
 * "Dark Studio / Neon Accent" restyle of the original solid-fill card; call
 * sites are unchanged.
 */
@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnContentScope.() -> Unit,
) {
    GlassCard(modifier = modifier) {
        ColumnContentScope.content()
    }
}

/**
 * The shared glass-card visual: a semi-transparent surface fill plus a
 * subtle 1dp light-alpha border, giving the illusion of a frosted panel
 * floating above the ambient background gradient behind it.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnContentScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Spacing.xs / 2),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.08f), MaterialTheme.shapes.medium)
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            ColumnContentScope.content()
        }
    }
}

/** Marker receiver so [SettingsCard]'s content block reads clearly as a Column body. */
object ColumnContentScope

/**
 * A single "pick one of N" option rendered as a filled/outlined chip, used to
 * replace rows of identical full-width [androidx.compose.material3.Button]s
 * for choices like theme preset, repeat mode, artwork quality, etc.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = MaterialTheme.shapes.small,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = modifier,
    )
}
