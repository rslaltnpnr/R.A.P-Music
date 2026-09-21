package com.ozin.music.feature.dj

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ozin.music.R
import com.ozin.music.core.data.model.Song
import com.ozin.music.core.domain.dj.LoopMath
import java.io.File

@Composable
private fun rememberJogRotation(isPlaying: Boolean, degreesPerSecond: Float): Float {
    var angle by remember { mutableStateOf(0f) }
    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        var lastFrameNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (lastFrameNanos != 0L) {
                    val deltaSeconds = (frameNanos - lastFrameNanos) / 1_000_000_000f
                    angle = (angle + deltaSeconds * degreesPerSecond) % 360f
                }
                lastFrameNanos = frameNanos
            }
        }
    }
    return angle
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DjModeScreen(onExit: () -> Unit) {
    val viewModel: DjViewModel = hiltViewModel()
    var proMode by remember { mutableStateOf(true) }
    val library by viewModel.library.collectAsState()
    val engineState by viewModel.engineState.collectAsState()
    var trackBrowserFor by remember { mutableStateOf<DeckId?>(null) }
    var showRecordDialog by remember { mutableStateOf(false) }
    var showAutomixSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val projectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir
            val file = File(dir, "dj_recording_${System.currentTimeMillis()}.m4a")
            viewModel.engine.recorder.start(result.resultCode, data, file)
        }
    }
    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            val intent = DjRecorder.createCaptureIntent(context as Activity)
            projectionLauncher.launch(intent)
        }
    }

    engineState.syncMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearSyncMessage() },
            confirmButton = { TextButton(onClick = { viewModel.clearSyncMessage() }) { Text(stringResourceCompat(R.string.dj_ok)) } },
            title = { Text(stringResourceCompat(R.string.dj_sync)) },
            text = { Text(stringResourceCompat(R.string.dj_bpm_unknown)) },
        )
    }

    if (showRecordDialog) {
        AlertDialog(
            onDismissRequest = { showRecordDialog = false },
            title = { Text(stringResourceCompat(R.string.dj_record)) },
            text = { Text(stringResourceCompat(R.string.dj_record_consent_explainer)) },
            confirmButton = {
                TextButton(onClick = {
                    showRecordDialog = false
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        val intent = DjRecorder.createCaptureIntent(context as Activity)
                        projectionLauncher.launch(intent)
                    } else {
                        recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }) { Text(stringResourceCompat(R.string.dj_continue)) }
            },
            dismissButton = { TextButton(onClick = { showRecordDialog = false }) { Text(stringResourceCompat(R.string.dj_cancel)) } },
        )
    }

    if (showAutomixSheet) {
        ModalBottomSheet(onDismissRequest = { showAutomixSheet = false }) {
            AutomixSettingsPanel(
                settings = engineState.automix,
                onSettingsChange = { viewModel.setAutomixSettings(it) },
            )
        }
    }

    trackBrowserFor?.let { deck ->
        ModalBottomSheet(onDismissRequest = { trackBrowserFor = null }) {
            TrackBrowserSheet(
                library = library,
                onPick = { song ->
                    viewModel.loadSong(deck, song)
                    trackBrowserFor = null
                },
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DjColors.panelBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(8.dp),
    ) {
        DjTopBar(
            proMode = proMode,
            onProModeChange = { proMode = it },
            isRecording = engineState.isRecording,
            recordingSupported = engineState.recordingSupported,
            onRecordToggle = {
                if (engineState.isRecording) {
                    viewModel.engine.recorder.stop()
                } else {
                    showRecordDialog = true
                }
            },
            automixEnabled = engineState.automix.enabled,
            onAutomixClick = { showAutomixSheet = true },
            onExit = onExit,
        )

        // Adaptive layout: a real 3-column deck/mixer/deck Row only fits
        // comfortably on a wide screen (landscape phone or tablet). On a
        // narrow one (portrait phone) it gets crushed, so stack the two
        // decks and the mixer vertically instead, in a scrollable column so
        // nothing is ever clipped regardless of screen size.
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = maxWidth >= 600.dp
            if (isWide) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.weight(1f)) {
                        DeckPanel(
                            deckId = DeckId.A,
                            viewModel = viewModel,
                            accent = DjColors.deckA,
                            proMode = proMode,
                            onOpenBrowser = { trackBrowserFor = DeckId.A },
                            modifier = Modifier.weight(1f),
                        )
                        MixerStrip(
                            viewModel = viewModel,
                            engineState = engineState,
                            proMode = proMode,
                            modifier = Modifier.width(140.dp),
                        )
                        DeckPanel(
                            deckId = DeckId.B,
                            viewModel = viewModel,
                            accent = DjColors.deckB,
                            proMode = proMode,
                            onOpenBrowser = { trackBrowserFor = DeckId.B },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (proMode) {
                        SamplerRow(viewModel = viewModel, library = library)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    DeckPanel(
                        deckId = DeckId.A,
                        viewModel = viewModel,
                        accent = DjColors.deckA,
                        proMode = proMode,
                        onOpenBrowser = { trackBrowserFor = DeckId.A },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    MixerStrip(
                        viewModel = viewModel,
                        engineState = engineState,
                        proMode = proMode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                    )
                    DeckPanel(
                        deckId = DeckId.B,
                        viewModel = viewModel,
                        accent = DjColors.deckB,
                        proMode = proMode,
                        onOpenBrowser = { trackBrowserFor = DeckId.B },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (proMode) {
                        SamplerRow(viewModel = viewModel, library = library)
                    }
                }
            }
        }
    }
}

@Composable
private fun DjTopBar(
    proMode: Boolean,
    onProModeChange: (Boolean) -> Unit,
    isRecording: Boolean,
    recordingSupported: Boolean,
    onRecordToggle: () -> Unit,
    automixEnabled: Boolean,
    onAutomixClick: () -> Unit,
    onExit: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResourceCompat(R.string.dj_mode_title), color = DjColors.textPrimary, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResourceCompat(R.string.dj_basic), color = DjColors.textSecondary)
            Switch(checked = proMode, onCheckedChange = onProModeChange)
            Text(stringResourceCompat(R.string.dj_pro), color = DjColors.textSecondary)
            Spacer(modifier = Modifier.width(4.dp))
            if (proMode) {
                TextButton(onClick = onAutomixClick) {
                    Text(
                        stringResourceCompat(R.string.dj_automix) + if (automixEnabled) " ✓" else "",
                        color = if (automixEnabled) DjColors.ledCyan else DjColors.textSecondary,
                    )
                }
                if (recordingSupported) {
                    IconButton(onClick = onRecordToggle) {
                        Icon(
                            Icons.Filled.FiberManualRecord,
                            contentDescription = stringResourceCompat(R.string.dj_record),
                            tint = if (isRecording) Color.Red else DjColors.textSecondary,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onExit) {
                Icon(Icons.Filled.Close, contentDescription = stringResourceCompat(R.string.dj_close), tint = DjColors.textSecondary)
            }
        }
    }
}

@Composable
private fun DeckPanel(
    deckId: DeckId,
    viewModel: DjViewModel,
    accent: Color,
    proMode: Boolean,
    onOpenBrowser: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by (if (deckId == DeckId.A) viewModel.deckAState else viewModel.deckBState).collectAsState()
    val rotation = rememberJogRotation(state.isPlaying, degreesPerSecond = 40f)
    val progress = if (state.durationMs > 0) (state.positionMs.toFloat() / state.durationMs) else 0f

    Column(modifier = modifier.padding(4.dp)) {
        Text(
            text = state.song?.let { "${it.title} — ${it.artist}" } ?: stringResourceCompat(R.string.dj_no_track_loaded),
            color = DjColors.textPrimary,
            maxLines = 1,
        )
        Text(
            text = state.originalBpm?.let { "%.0f BPM".format(it) } ?: "-- BPM",
            color = DjColors.textSecondary,
        )
        TextButton(onClick = onOpenBrowser) { Text(stringResourceCompat(R.string.dj_load_track), color = accent) }

        WaveformView(
            amplitudes = state.waveform,
            progress = progress,
            hotCues = state.hotCues.values.mapNotNull { cue ->
                if (state.durationMs > 0) cue.toFloat() / state.durationMs else null
            },
            loopRange = state.loop.takeIf { it.isValid }?.let { loop ->
                if (state.durationMs > 0) {
                    (loop.inMs!!.toFloat() / state.durationMs)..(loop.outMs!!.toFloat() / state.durationMs)
                } else null
            },
            accent = accent,
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            JogWheel(
                isPlaying = state.isPlaying,
                rotationDegrees = rotation,
                onSeekDelta = { delta -> viewModel.nudgeSeek(deckId, delta) },
                accent = accent,
            )
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    LedButton(
                        label = stringResourceCompat(R.string.dj_play),
                        active = state.isPlaying,
                        ledColor = DjColors.ledGreen,
                        onClick = { viewModel.togglePlayPause(deckId) },
                    )
                    LedButton(
                        label = stringResourceCompat(R.string.dj_cue),
                        active = false,
                        ledColor = DjColors.ledOrange,
                        onClick = { viewModel.cue(deckId) },
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LedButton(
                    label = stringResourceCompat(R.string.dj_sync),
                    active = false,
                    ledColor = DjColors.ledCyan,
                    onClick = { viewModel.sync(deckId) },
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(stringResourceCompat(R.string.dj_hot_cues), color = DjColors.textSecondary)
        HotCueGrid(deckId = deckId, hotCues = state.hotCues, accent = accent, viewModel = viewModel)

        if (proMode) {
            Spacer(modifier = Modifier.height(4.dp))
            LoopRow(deckId = deckId, state = state, viewModel = viewModel, accent = accent)
        }
    }
}

@Composable
private fun HotCueGrid(deckId: DeckId, hotCues: Map<Int, Long>, accent: Color, viewModel: DjViewModel) {
    Column {
        for (row in 0 until 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (col in 0 until 4) {
                    val pad = row * 4 + col
                    HotCuePad(
                        label = (pad + 1).toString(),
                        isSet = hotCues.containsKey(pad),
                        accent = accent,
                        onTap = { viewModel.onHotCuePress(deckId, pad) },
                        onLongPress = { viewModel.onHotCueLongPress(deckId, pad) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun LoopRow(deckId: DeckId, state: DeckUiState, viewModel: DjViewModel, accent: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        TextButton(onClick = { viewModel.setLoopIn(deckId) }) { Text(stringResourceCompat(R.string.dj_loop_in), color = accent) }
        TextButton(onClick = { viewModel.setLoopOut(deckId) }) { Text(stringResourceCompat(R.string.dj_loop_out), color = accent) }
        TextButton(onClick = { viewModel.toggleLoopActive(deckId) }) {
            Text(
                if (state.loop.active) stringResourceCompat(R.string.dj_loop_exit) else stringResourceCompat(R.string.dj_reloop),
                color = if (state.loop.active) DjColors.ledCyan else accent,
            )
        }
        TextButton(onClick = { viewModel.setAutoLoop(deckId, LoopMath.loopDurationMs(4.0, state.originalBpm)) }) {
            Text(stringResourceCompat(R.string.dj_auto_loop_4), color = accent)
        }
    }
}

@Composable
private fun MixerStrip(
    viewModel: DjViewModel,
    engineState: DjEngineState,
    proMode: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DjColors.panelSurface)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResourceCompat(R.string.dj_mixer), color = DjColors.textPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChannelStrip(deckId = DeckId.A, viewModel = viewModel, engineState = engineState, proMode = proMode, accent = DjColors.deckA)
            ChannelStrip(deckId = DeckId.B, viewModel = viewModel, engineState = engineState, proMode = proMode, accent = DjColors.deckB)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(stringResourceCompat(R.string.dj_crossfader), color = DjColors.textSecondary)
        Crossfader(
            position = engineState.crossfaderPosition,
            onChange = { viewModel.setCrossfader(it) },
        )
    }
}

@Composable
private fun ChannelStrip(
    deckId: DeckId,
    viewModel: DjViewModel,
    engineState: DjEngineState,
    proMode: Boolean,
    accent: Color,
) {
    val state by (if (deckId == DeckId.A) viewModel.deckAState else viewModel.deckBState).collectAsState()
    // Reasonable, documented approximation: without a real-time audio-level
    // tap, the VU meter reflects isPlaying/volume rather than true output
    // level.
    val level = if (state.isPlaying) 0.75f else 0.1f

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(10.dp)
                .height((60 * level).dp)
                .background(accent),
        )
        Spacer(modifier = Modifier.height(4.dp))
        var gain by remember { mutableStateOf(0.5f) }
        RotaryKnob(value = gain, onChange = {
            gain = it
            viewModel.setGain(deckId, it * 2f)
        }, accent = accent)
        Text(stringResourceCompat(R.string.dj_gain), color = DjColors.textSecondary)
        if (proMode) {
            var high by remember { mutableStateOf(0.5f) }
            var mid by remember { mutableStateOf(0.5f) }
            var low by remember { mutableStateOf(0.5f) }
            var filter by remember { mutableStateOf(0.5f) }
            RotaryKnob(value = high, onChange = { high = it; viewModel.setHigh(deckId, ((it - 0.5f) * 3000).toInt()) }, accent = accent)
            Text(stringResourceCompat(R.string.dj_high), color = DjColors.textSecondary)
            RotaryKnob(value = mid, onChange = { mid = it; viewModel.setMid(deckId, ((it - 0.5f) * 3000).toInt()) }, accent = accent)
            Text(stringResourceCompat(R.string.dj_mid), color = DjColors.textSecondary)
            RotaryKnob(value = low, onChange = { low = it; viewModel.setLow(deckId, ((it - 0.5f) * 3000).toInt()) }, accent = accent)
            Text(stringResourceCompat(R.string.dj_low), color = DjColors.textSecondary)
            RotaryKnob(value = filter, onChange = { filter = it; viewModel.setFilter(deckId, (it - 0.5f) * 2f) }, accent = accent)
            Text(stringResourceCompat(R.string.dj_filter), color = DjColors.textSecondary)
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SamplerRow(viewModel: DjViewModel, library: List<Song>) {
    var assigningPad by remember { mutableStateOf<Int?>(null) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (pad in 0 until 8) {
            HotCuePad(
                label = "S${pad + 1}",
                isSet = viewModel.engine.sampler.padAt(pad) != null,
                accent = DjColors.ledOrange,
                onTap = { viewModel.triggerSamplerPad(pad) },
                onLongPress = { assigningPad = pad },
            )
        }
    }
    assigningPad?.let { pad ->
        ModalBottomSheet(onDismissRequest = { assigningPad = null }) {
            TrackBrowserSheet(
                library = library,
                onPick = { song ->
                    viewModel.assignSamplerClip(pad, song)
                    assigningPad = null
                },
            )
        }
    }
}

@Composable
private fun TrackBrowserSheet(library: List<Song>, onPick: (Song) -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, library) {
        if (query.isBlank()) library
        else library.filter { it.title.contains(query, true) || it.artist.contains(query, true) }
    }
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResourceCompat(R.string.dj_search_library)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.height(360.dp)) {
            items(filtered) { song ->
                Text(
                    text = "${song.title} — ${song.artist}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { onPick(song) },
                )
            }
        }
    }
}

@Composable
private fun AutomixSettingsPanel(settings: AutomixSettings, onSettingsChange: (AutomixSettings) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(stringResourceCompat(R.string.dj_automix_enable))
            Switch(checked = settings.enabled, onCheckedChange = { onSettingsChange(settings.copy(enabled = it)) })
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(stringResourceCompat(R.string.dj_automix_prefer_favorites))
            Switch(checked = settings.preferFavorites, onCheckedChange = { onSettingsChange(settings.copy(preferFavorites = it)) })
        }
        Text(stringResourceCompat(R.string.dj_automix_transition) + ": ${settings.transitionSeconds}s")
    }
}

@Composable
private fun stringResourceCompat(id: Int): String = androidx.compose.ui.res.stringResource(id)
