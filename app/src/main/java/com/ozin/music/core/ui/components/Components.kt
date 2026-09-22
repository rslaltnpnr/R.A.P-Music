package com.ozin.music.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
 * One tappable row in the Settings category list: an icon, a title, an
 * optional one-line subtitle, and a trailing chevron - rendered as its own
 * [GlassCard] so the whole list reads as a set of distinct destinations
 * rather than a single flat list. Tapping navigates to that category's own
 * page (see `SettingsSubScreens.kt`), replacing the old single
 * scroll-everything Settings screen.
 */
@Composable
fun SettingsCategoryRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Spacing.xs / 2),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.08f), MaterialTheme.shapes.medium)
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Shared scaffold for a Settings category's own page: a back button + title
 * row, then a scrollable column of [GlassCard]-grouped content. Used by every
 * screen in `SettingsSubScreens.kt` so they all share one consistent header
 * and scroll/padding behavior instead of each reimplementing it.
 */
@Composable
fun SettingsSubScreen(
    title: String,
    onBack: () -> Unit,
    backContentDescription: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnContentScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.xl),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = backContentDescription)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        ColumnContentScope.content()
    }
}

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
