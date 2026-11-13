package com.ozin.music.feature.classic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ozin.music.R
import com.ozin.music.core.data.model.Song
import com.ozin.music.core.domain.EqPresetId
import com.ozin.music.core.player.ClassicAudioFrame
import com.ozin.music.core.player.PlaybackUiState
import com.ozin.music.core.player.RepeatUiMode
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private enum class ClassicMenu { APP, PRESETS, PLAYLIST }
private enum class MeterMode { SPECTRUM, WAVE, OFF }

@Composable
fun ClassicPlayerScreen(
    onLibrary: () -> Unit,
    onPlaylists: () -> Unit,
    onAdvancedEq: () -> Unit,
    onModernPlayer: () -> Unit,
    onDj: () -> Unit,
    onSettings: () -> Unit,
    onDashboard: () -> Unit,
    onExit: () -> Unit,
    model: ClassicPlayerViewModel = hiltViewModel(),
) {
    val state by model.playback.collectAsStateWithLifecycle()
    val settings by model.settings.collectAsStateWithLifecycle()
    val library by model.library.collectAsStateWithLifecycle()
    val volume by model.volume.collectAsStateWithLifecycle()
    val scanning by model.scanning.collectAsStateWithLifecycle()
    var showEq by rememberSaveable { mutableStateOf(true) }
    var showPlaylist by rememberSaveable { mutableStateOf(true) }
    var shadePlayer by rememberSaveable { mutableStateOf(false) }
    var shadeEq by rememberSaveable { mutableStateOf(false) }
    var shadeList by rememberSaveable { mutableStateOf(false) }
    var remainingTime by rememberSaveable { mutableStateOf(false) }
    var meter by rememberSaveable { mutableStateOf(MeterMode.SPECTRUM) }
    var menu by remember { mutableStateOf<ClassicMenu?>(null) }
    var picker by rememberSaveable { mutableStateOf(false) }
    var save by rememberSaveable { mutableStateOf(false) }
    var clear by rememberSaveable { mutableStateOf(false) }
    var selectedIndex by rememberSaveable { mutableIntStateOf(-1) }
    val isCapturing = state.isPlaying && !shadePlayer && meter != MeterMode.OFF
    DisposableEffect(model, isCapturing) {
        model.setCapture(isCapturing)
        onDispose { model.setCapture(false) }
    }

    fun openPicker() { picker = true }
    fun toggleEq() { showEq = !showEq }
    fun toggleList() { showPlaylist = !showPlaylist }
    val openAppMenu = { menu = ClassicMenu.APP }
    val openPresetMenu = { menu = ClassicMenu.PRESETS }
    val openPlaylistMenu = { menu = ClassicMenu.PLAYLIST }
    LaunchedEffect(state.queue.size) {
        if (selectedIndex !in state.queue.indices) selectedIndex = -1
    }

    BoxWithConstraints(
        Modifier.fillMaxSize().background(Color(0xFF11111B)).padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        val sideBySide = maxWidth >= 620.dp && maxWidth > maxHeight
        val windowWidth = if (sideBySide) minOf((maxWidth - 4.dp) / 2, 480.dp) else minOf(maxWidth, 520.dp)
        val u = windowWidth / 275
        val panelHeight = if (sideBySide) minOf(maxHeight, u * 280) else u * 248
        val main: @Composable () -> Unit = {
            MainWindow(
                u, state, volume, settings.stereoBalancePercent / 100f, shadePlayer, showEq, showPlaylist,
                remainingTime, meter, isCapturing, model, onPlay = {
                    if (!state.isConnected || (state.queue.isEmpty() && library.isEmpty())) openPicker() else model.play()
                },
                onAdd = ::openPicker,
                onEq = ::toggleEq,
                onList = ::toggleList,
                onMenu = openAppMenu,
                onShade = { shadePlayer = !shadePlayer },
                onExit = onExit,
                onTime = { remainingTime = !remainingTime },
                onMeter = { meter = MeterMode.entries[(meter.ordinal + 1) % MeterMode.entries.size] },
            )
        }
        val equalizer: @Composable () -> Unit = {
            if (showEq) EqualizerWindow(
                u, shadeEq, settings.eqEnabled, settings.eqSuggestionEnabled, settings.eqPreampMb, classicEqCurve(settings), model,
                onShade = { shadeEq = !shadeEq },
                onClose = { showEq = false },
                onPresets = openPresetMenu,
            )
        }
        val playlist: @Composable () -> Unit = {
            if (showPlaylist) PlaylistWindow(
                u, if (shadeList) u * 14 else maxOf(panelHeight, u * 122), shadeList, state, selectedIndex,
                onSong = { index -> selectedIndex = index; model.player.playQueueIndex(index) },
                onAdd = ::openPicker,
                onRemove = {
                    val index = selectedIndex.takeIf { it in state.queue.indices } ?: state.currentIndex
                    if (index in state.queue.indices) model.player.removeFromQueue(index)
                    selectedIndex = -1
                },
                onMenu = openPlaylistMenu,
                onSelect = {
                    if (state.queue.isNotEmpty()) selectedIndex = (selectedIndex + 1) % state.queue.size
                },
                onShade = { shadeList = !shadeList },
                onClose = { showPlaylist = false },
                model = model,
            )
        }
        if (sideBySide) {
            Row(Modifier.verticalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Column { main(); equalizer() }
                playlist()
            }
        } else {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                main()
                equalizer()
                playlist()
            }
        }
        val activeMenu = menu
        DropdownMenu(expanded = activeMenu != null, onDismissRequest = { menu = null }) {
            when (activeMenu) {
                ClassicMenu.APP -> {
                    ClassicMenuItem(R.string.classic_menu_add) { menu = null; openPicker() }
                    ClassicMenuItem(R.string.nav_music) { menu = null; onLibrary() }
                    ClassicMenuItem(R.string.nav_lists) { menu = null; onPlaylists() }
                    ClassicMenuItem(R.string.nav_dj_mode) { menu = null; onDj() }
                    ClassicMenuItem(R.string.classic_modern_player) { menu = null; onModernPlayer() }
                    ClassicMenuItem(R.string.classic_dashboard) { menu = null; onDashboard() }
                    ClassicMenuItem(R.string.nav_settings) { menu = null; onSettings() }
                }
                ClassicMenu.PRESETS -> {
                    EqPresetId.entries.filter { it != EqPresetId.CUSTOM }.forEach { id ->
                        DropdownMenuItem(
                            text = { Text((if (settings.eqPreset == id) "✓ " else "") + id.name.replace('_', ' ')) },
                            onClick = { menu = null; model.applyPreset(id) },
                        )
                    }
                    ClassicMenuItem(R.string.classic_advanced_eq) { menu = null; onAdvancedEq() }
                }
                ClassicMenu.PLAYLIST -> {
                    ClassicMenuItem(R.string.classic_save_queue, state.queue.any { it.id >= 0 }) { menu = null; save = true }
                    ClassicMenuItem(R.string.classic_clear_queue, state.queue.isNotEmpty()) { menu = null; clear = true }
                    ClassicMenuItem(R.string.nav_lists) { menu = null; onPlaylists() }
                }
                null -> Unit
            }
        }
    }
    if (picker) SongPicker(
        library, scanning, state.isConnected,
        onAdd = { model.add(it); showPlaylist = true; shadeList = false },
        onScan = model::rescan,
        onClose = { picker = false },
    )
    if (save) SaveQueueDialog(onClose = { save = false }, onSave = { model.saveQueue(it); save = false })
    if (clear) AlertDialog(
        onDismissRequest = { clear = false },
        title = { Text(stringResource(R.string.classic_clear_queue)) },
        text = { Text(stringResource(R.string.classic_clear_info)) },
        confirmButton = {
            TextButton(onClick = { model.player.clearQueue(); clear = false }) {
                Text(stringResource(R.string.classic_clear_queue))
            }
        },
        dismissButton = { TextButton(onClick = { clear = false }) { Text(stringResource(R.string.classic_cancel)) } },
    )
}

@Composable
private fun MainWindow(
    u: Dp,
    state: PlaybackUiState,
    volume: Float,
    balance: Float,
    shaded: Boolean,
    eqVisible: Boolean,
    listVisible: Boolean,
    remaining: Boolean,
    meter: MeterMode,
    capture: Boolean,
    model: ClassicPlayerViewModel,
    onPlay: () -> Unit,
    onAdd: () -> Unit,
    onEq: () -> Unit,
    onList: () -> Unit,
    onMenu: () -> Unit,
    onShade: () -> Unit,
    onExit: () -> Unit,
    onTime: () -> Unit,
    onMeter: () -> Unit,
) {
    val fallback = stringResource(R.string.classic_ready)
    val song = state.currentSong
    val title = song?.let {
        val name = listOf(it.artist, it.title).filter(String::isNotBlank).joinToString(" - ")
        if (state.currentIndex >= 0) "${state.currentIndex + 1}. $name" else name
    } ?: fallback
    var tick by remember(title, shaded) { mutableIntStateOf(0) }
    LaunchedEffect(title, shaded) {
        if (shaded || title.length <= 29) return@LaunchedEffect
        delay(900)
        while (true) {
            tick = (tick + 1) % (title.length + 5)
            delay(170)
        }
    }
    val marquee = if (tick > 0) ("$title     $title").drop(tick) else title
    val duration = state.durationMs.takeIf { it > 0 } ?: (song?.durationMs ?: 0L)
    val current = state.positionMs.coerceIn(0L, duration.coerceAtLeast(0L))
    var seekPreview by remember { mutableStateOf<Float?>(null) }
    var volumePreview by remember { mutableStateOf<Float?>(null) }
    var panPreview by remember { mutableStateOf<Float?>(null) }
    val time = seekPreview?.let { (it * duration).toLong() } ?: current
    val clock = (if (remaining && duration > 0) "-" else "") +
        classicTime(if (remaining && duration > 0) duration - time else time).padStart(5, '0')
    val estimatedBitrate = song?.let {
        if (it.durationMs > 0 && it.sizeBytes > 0) (it.sizeBytes * 8 / it.durationMs).coerceAtMost(9999L).toInt() else 0
    } ?: 0
    val bitrate = (state.audioBitrate / 1000).takeIf { it > 0 } ?: estimatedBitrate
    val h = if (shaded) 14 else 116

    Box(Modifier.size(u * 275, u * h)) {
        ClassicCanvas(u, h) {
            shell(h.toFloat())
            titleRail(if (shaded) "R.A.P MUSIC   $clock" else "R.A.P MUSIC", menu = true)
            if (!shaded) {
                meterGrid()
                val indicator = when {
                    state.isPlaying -> Transport.PLAY
                    song != null && !state.isStopped -> Transport.PAUSE
                    else -> Transport.STOP
                }
                transportIcon(indicator, 23f, 27f, if (song != null) ClassicGreen else Color(0xFF235044))
                digitalTime(clock, if (clock.startsWith("-")) 39f else 43f, 25f)
                well(108f, 23f, 159f, 14f)
                clipRect(110f, 24f, 265f, 36f) {
                    pixelText(marquee, 111f, 32f, 8f, ClassicGreen)
                }
                well(108f, 40f, 20f, 12f)
                well(153f, 40f, 15f, 12f)
                if (bitrate > 0) pixelText("$bitrate", 118f, 49f, 7.5f, ClassicGreen, true)
                if (state.audioSampleRate > 0) pixelText("${state.audioSampleRate / 1000}", 160f, 49f, 7.5f, ClassicGreen, true)
                pixelText("kbps", 131f, 49f, 7.3f, ClassicWhite)
                pixelText("kHz", 172f, 49f, 7.3f, ClassicWhite)
                pixelText("MONO", 213f, 49f, 6.3f, if (state.audioChannels == 1) ClassicGreen else Color(0xFF777A90))
                pixelText("STEREO", 236f, 49f, 6.3f, if (state.audioChannels > 1) ClassicGreen else Color(0xFF777A90))
                horizontalControl(107f, 57f, 68f, 12f, volumePreview ?: volume)
                horizontalControl(177f, 57f, 38f, 12f, ((panPreview ?: balance) + 1f) / 2f, balance = true)
                labeledButton("EQ", 219f, 57f, 23f, 12f, eqVisible)
                labeledButton("PL", 242f, 57f, 23f, 12f, listVisible)
                positionControl(seekPreview ?: if (duration > 0) current.toFloat() / duration else 0f, duration > 0)
                Transport.entries.forEachIndexed { i, action ->
                    val x = if (action == Transport.EJECT) 136f else 16f + i * 23
                    val width = if (i < 4) 23f else 22f
                    val y = if (action == Transport.EJECT) 89f else 88f
                    val height = if (action == Transport.EJECT) 16f else 18f
                    buttonFace(x, y, width, height, state.isPlaying && action == Transport.PLAY)
                    transportIcon(action, x + 7f, y + 5.5f, Color(0xFF202737))
                }
                labeledButton("SHUFFLE", 164f, 89f, 47f, 15f, state.shuffleEnabled)
                labeledButton(if (state.repeatMode == RepeatUiMode.ONE) "REP 1" else "⟳",
                    211f, 89f, 28f, 15f, state.repeatMode != RepeatUiMode.OFF)
                if (state.shuffleEnabled) pxRect(167f, 91f, 2f, 2f, ClassicGreen)
                if (state.repeatMode != RepeatUiMode.OFF) pxRect(214f, 91f, 2f, 2f, ClassicGreen)
                bolt(251f, 91f, 1.45f)
            }
        }
        ClassicHit(u, 2, 0, 24, 18, R.string.classic_menu, onClick = onMenu)
        ClassicHit(u, 240, 0, 18, 18, R.string.classic_rollup, onClick = onShade)
        ClassicHit(u, 258, 0, 17, 18, R.string.classic_exit, onClick = onExit)
        if (!shaded) {
            if (capture) MeterOverlay(u, meter, model)
            ClassicHit(u, 12, 20, 92, 20, R.string.classic_time, onClick = onTime)
            ClassicHit(u, 12, 40, 92, 24, R.string.classic_visualizer, onClick = onMeter)
            Box(Modifier.offset(u * 109, u * 22).size(u * 156, u * 17).semantics { contentDescription = title })
            ClassicSlider(u, 105, 54, 71, 17, volumePreview ?: volume, R.string.classic_volume,
                onChange = { volumePreview = it; model.setVolume(it) }, onEnd = { volumePreview = null })
            ClassicSlider(u, 177, 54, 40, 17, ((panPreview ?: balance) + 1) / 2, R.string.classic_balance,
                onChange = { panPreview = it * 2 - 1 },
                onEnd = { panPreview?.let(model::setBalance); panPreview = null })
            ClassicHit(u, 218, 54, 24, 17, R.string.nav_eq, onClick = onEq)
            ClassicHit(u, 242, 54, 25, 17, R.string.classic_playlist, onClick = onList)
            if (duration > 0) ClassicSlider(u, 15, 71, 252, 14, seekPreview ?: (current.toFloat() / duration),
                R.string.classic_seek, onChange = { seekPreview = it },
                onEnd = { seekPreview?.let { model.player.seekTo((it * duration).toLong()) }; seekPreview = null })
            val actions: List<Pair<Int, () -> Unit>> = listOf(
                R.string.classic_previous to { model.player.skipToPrevious(); Unit },
                R.string.classic_play to onPlay,
                R.string.classic_pause to model.player::pause,
                R.string.classic_stop to model.player::stopAndRewind,
                R.string.classic_next to { model.player.skipToNext(); Unit },
                R.string.classic_menu_add to onAdd,
            )
            actions.forEachIndexed { i, (label, callback) ->
                ClassicHit(u, if (i == 5) 134 else 16 + i * 23, 86, 23, 25, label, onClick = callback)
            }
            ClassicHit(u, 162, 86, 49, 24, R.string.classic_shuffle, onClick = model.player::toggleShuffle)
            ClassicHit(u, 211, 86, 29, 24, R.string.classic_repeat, onClick = model.player::cycleRepeat)
            ClassicHit(u, 244, 85, 27, 26, R.string.classic_menu, onClick = onMenu)
        }
    }
}

@Composable
private fun MeterOverlay(u: Dp, meter: MeterMode, model: ClassicPlayerViewModel) {
    val audio by model.audioFrame.collectAsStateWithLifecycle()
    Canvas(Modifier.offset(u * 24, u * 43).size(u * 76, u * 19)) {
        val factor = size.width / 76
        scale(factor, factor, Offset.Zero) {
            when (meter) {
                MeterMode.SPECTRUM -> spectrum(audio)
                MeterMode.WAVE -> {
                    val wave = audio.waveform
                    for (i in 1 until wave.size) {
                        drawLine(ClassicGreen,
                            Offset((i - 1).toFloat(), 9f - wave[i - 1] * 9f),
                            Offset(i.toFloat(), 9f - wave[i] * 9f), 1f)
                    }
                }
                MeterMode.OFF -> Unit
            }
        }
    }
}

private fun DrawScope.spectrum(audio: ClassicAudioFrame) {
    audio.spectrum.forEachIndexed { i, height ->
        val cells = (height * 9).roundToInt()
        for (cell in 0 until cells) {
            val tint = when (cell) {
                in 0..4 -> ClassicGreen
                in 5..6 -> Color(0xFFCEDB0A)
                7 -> Color(0xFFFFA20A)
                else -> Color(0xFFF55324)
            }
            pxRect(i * 4f, 17f - cell * 2, 3f, 1f, tint)
        }
    }
}

@Composable
private fun EqualizerWindow(
    u: Dp,
    shaded: Boolean,
    enabled: Boolean,
    autoSuggestion: Boolean,
    preamp: Int,
    bands: List<Int>,
    model: ClassicPlayerViewModel,
    onShade: () -> Unit,
    onClose: () -> Unit,
    onPresets: () -> Unit,
) {
    var bandPreview by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var preampPreview by remember { mutableStateOf<Int?>(null) }
    val h = if (shaded) 14 else 116
    Box(Modifier.size(u * 275, u * h)) {
        ClassicCanvas(u, h) {
            shell(h.toFloat())
            titleRail("R.A.P MUSIC EQUALIZER")
            if (!shaded) {
                labeledButton("ON", 14f, 17f, 27f, 12f, enabled)
                if (enabled) pxRect(17f, 20f, 3f, 2f, ClassicGreen)
                labeledButton("AUTO", 43f, 17f, 33f, 12f, autoSuggestion)
                if (autoSuggestion) pxRect(46f, 20f, 3f, 2f, ClassicGreen)
                labeledButton("PRESETS", 218f, 17f, 43f, 12f)
                well(86f, 16f, 112f, 20f, Color(0xFF292939))
                for (i in 1..9) pxRect(86f + i * 11, 17f, 1f, 17f, Color(0xFF47475B))
                pxRect(88f, 26f, 107f, 1f, Color(0xFF5B5B6F))
                val graph = Path()
                bands.forEachIndexed { i, raw ->
                    val gain = bandPreview?.takeIf { it.first == i }?.second ?: raw
                    val x = 89f + i * 11.5f
                    val y = 26f - gain * 8f / 1200
                    if (i == 0) graph.moveTo(x, y) else graph.lineTo(x, y)
                }
                drawPath(graph, if (enabled) ClassicGreen else Color(0xFF6F8068), style = Stroke(1f))
                pixelText("+12dB", 46f, 42f, 6.8f, Color(0xFFEFCA4F))
                pixelText("+0dB", 46f, 70f, 6.8f, Color(0xFFEFCA4F))
                pixelText("-12dB", 46f, 98f, 6.8f, Color(0xFFEFCA4F))
                eqTrack(17f, preampPreview ?: preamp)
                pixelText("PREAMP", 20f, 109f, 6.7f, ClassicWhite, true)
                bands.forEachIndexed { i, gain ->
                    val x = 81f + i * 18
                    eqTrack(x, bandPreview?.takeIf { it.first == i }?.second ?: gain)
                    pixelText(classicEqLabels[i], x + 4f, 109f, 6.8f, ClassicWhite, true)
                }
            }
        }
        ClassicHit(u, 243, 0, 15, 15, R.string.classic_rollup, onClick = onShade)
        ClassicHit(u, 258, 0, 17, 15, R.string.classic_close_eq, onClick = onClose)
        if (!shaded) {
            ClassicHit(u, 11, 15, 31, 17, R.string.eq_enabled, onClick = { model.setEqEnabled(!enabled) })
            ClassicHit(u, 42, 15, 36, 17, R.string.classic_eq_auto, onClick = { model.setEqAuto(!autoSuggestion) })
            ClassicHit(u, 215, 15, 51, 17, R.string.eq_presets, onClick = onPresets)
            ClassicSlider(u, 10, 35, 22, 66, ((preampPreview ?: preamp) + 1200) / 2400f,
                R.string.eq_preamp, vertical = true,
                onChange = { preampPreview = (it * 2400 - 1200).roundToInt() },
                onEnd = { preampPreview?.let(model::setPreamp); preampPreview = null })
            bands.forEachIndexed { i, gain ->
                val activeGain = bandPreview?.takeIf { it.first == i }?.second ?: gain
                ClassicSlider(u, 76 + i * 18, 35, 18, 66, (activeGain + 1200) / 2400f,
                    R.string.classic_eq_band, detail = classicEqLabels[i], vertical = true,
                    onChange = { bandPreview = i to (it * 2400 - 1200).roundToInt() },
                    onEnd = {
                        bandPreview?.takeIf { it.first == i }?.let { model.setBand(it.first, it.second) }
                        bandPreview = null
                    })
            }
        }
    }
}

@Composable
private fun PlaylistWindow(
    u: Dp,
    height: Dp,
    shaded: Boolean,
    state: PlaybackUiState,
    selected: Int,
    onSong: (Int) -> Unit,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onMenu: () -> Unit,
    onSelect: () -> Unit,
    onShade: () -> Unit,
    onClose: () -> Unit,
    model: ClassicPlayerViewModel,
) {
    val scroll = rememberLazyListState()
    val h = (height / u).roundToInt()
    val total = remember(state.queue) { state.queue.sumOf { it.durationMs.coerceAtLeast(0) } }
    LaunchedEffect(state.currentIndex, selected, shaded) {
        val target = selected.takeIf { it in state.queue.indices } ?: state.currentIndex
        if (!shaded && target in state.queue.indices &&
            scroll.layoutInfo.visibleItemsInfo.none { it.index == target }
        ) scroll.scrollToItem(target)
    }
    Box(Modifier.size(u * 275, height)) {
        ClassicCanvas(u, h) {
            shell(h.toFloat())
            titleRail(if (shaded) "R.A.P MUSIC PLAYLIST   ${classicTime(total)}" else "R.A.P MUSIC PLAYLIST")
            if (!shaded) {
                well(11f, 18f, 253f, h - 55f)
                pxRect(253f, 20f, 9f, h - 59f, Color(0xFF232338))
                val scrollThumb = (if (state.queue.size < 2) 0f else
                    scroll.firstVisibleItemIndex.toFloat() / (state.queue.size - 1)).coerceIn(0f, 1f)
                val thumbTop = 21f + (h - 76f) * scrollThumb
                buttonFace(254f, thumbTop, 7f, 14f)
                val top = h - 32f
                labeledButton("ADD", 12f, top, 25f, 19f)
                labeledButton("REM", 40f, top, 25f, 19f)
                labeledButton("SEL", 68f, top, 25f, 19f)
                labeledButton("MISC", 96f, top, 25f, 19f)
                well(128f, top, 94f, 11f)
                pixelText("${state.queue.size}  |  ${classicTime(total)}", 132f, top + 8, 7.1f, ClassicGreen)
                Transport.entries.take(5).forEachIndexed { i, a -> transportIcon(a, 130f + 11 * i, top + 14, Color(0xFFFFD985)) }
                well(185f, top + 13, 37f, 9f)
                pixelText(classicTime(state.positionMs), 203f, top + 20f, 7f, ClassicGreen, true)
                labeledButton("LIST", 228f, top, 24f, 19f)
                for (i in 0..3) drawLine(Color(0xFF67687C), Offset(260f + i * 3, h - 5f), Offset(270f, h - 15f + i * 3), 0.8f)
            }
        }
        ClassicHit(u, 243, 0, 15, 16, R.string.classic_rollup, onClick = onShade)
        ClassicHit(u, 258, 0, 17, 16, R.string.classic_close_playlist, onClick = onClose)
        if (!shaded) {
            val queueModifier = Modifier.offset(u * 13, u * 20).size(u * 238, u * (h - 58)).clipToBounds()
            if (state.queue.isEmpty()) {
                Box(queueModifier.clickable(onClick = onAdd), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.classic_empty_queue), color = Color(0xFF6B9A74),
                        fontFamily = FontFamily.Monospace, fontSize = (u.value * 8).sp)
                }
            } else {
                LazyColumn(queueModifier, state = scroll) {
                    itemsIndexed(state.queue) { index, song ->
                        val active = index == state.currentIndex
                        val color = if (active) Color.White else ClassicGreen
                        Row(
                            Modifier.fillMaxWidth().height(u * 17)
                                .background(if (index == selected || active) Color(0xFF0000A0) else ClassicBlack)
                                .clickable { onSong(index) }.padding(horizontal = u * 2),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${index + 1}. ${song.artist} - ${song.title}", Modifier.weight(1f), color = color,
                                fontFamily = FontFamily.Monospace, fontSize = (u.value * 8).sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.width(u * 4))
                            Text(classicTime(song.durationMs), color = color, fontFamily = FontFamily.Monospace,
                                fontSize = (u.value * 8).sp, maxLines = 1)
                        }
                    }
                }
            }
            val top = h - 34
            ClassicHit(u, 10, top, 28, 24, R.string.classic_menu_add, onClick = onAdd)
            ClassicHit(u, 39, top, 27, 24, R.string.classic_remove, onClick = onRemove)
            ClassicHit(u, 67, top, 28, 24, R.string.classic_select, onClick = onSelect)
            ClassicHit(u, 95, top, 28, 24, R.string.classic_list_options, onClick = onMenu)
            ClassicHit(u, 225, top, 31, 24, R.string.classic_list_options, onClick = onMenu)
            val actions: List<Pair<Int, () -> Unit>> = listOf(
                R.string.classic_previous to { model.player.skipToPrevious(); Unit },
                R.string.classic_play to model::play,
                R.string.classic_pause to model.player::pause,
                R.string.classic_stop to model.player::stopAndRewind,
                R.string.classic_next to { model.player.skipToNext(); Unit },
            )
            actions.forEachIndexed { i, (label, callback) ->
                ClassicHit(u, 128 + 11 * i, h - 21, 11, 16, label, onClick = callback)
            }
        }
    }
}

@Composable
private fun ClassicCanvas(u: Dp, h: Int, draw: DrawScope.() -> Unit) {
    Canvas(Modifier.size(u * 275, u * h)) {
        val factor = size.width / 275f
        scale(factor, factor, Offset.Zero) { draw() }
    }
}

@Composable
private fun ClassicHit(u: Dp, x: Int, y: Int, w: Int, h: Int, label: Int, onClick: () -> Unit) {
    val description = stringResource(label)
    Box(Modifier.offset(u * x, u * y).size(u * w, u * h)
        .semantics { contentDescription = description }
        .clickable(role = Role.Button, onClick = onClick))
}

@Composable
private fun ClassicSlider(
    u: Dp, x: Int, y: Int, w: Int, h: Int,
    value: Float, label: Int, detail: String = "", vertical: Boolean = false,
    onChange: (Float) -> Unit, onEnd: () -> Unit,
) {
    val description = listOf(stringResource(label), detail).filter(String::isNotBlank).joinToString(" ")
    val change by rememberUpdatedState(onChange)
    val end by rememberUpdatedState(onEnd)
    Box(Modifier.offset(u * x, u * y).size(u * w, u * h)
        .semantics {
            contentDescription = description
            progressBarRangeInfo = ProgressBarRangeInfo(value.coerceIn(0f, 1f), 0f..1f)
            setProgress { change(it.coerceIn(0f, 1f)); end(); true }
        }
        .pointerInput(vertical) {
            fun apply(input: PointerInputChange) {
                val fraction = if (vertical) 1f - input.position.y / size.height else input.position.x / size.width
                change(fraction.coerceIn(0f, 1f))
                input.consume()
            }
            awaitEachGesture {
                val down = awaitFirstDown()
                apply(down)
                try {
                    while (true) {
                        val point = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        if (!point.pressed) break
                        apply(point)
                    }
                } finally {
                    end()
                }
            }
        })
}

@Composable
private fun ClassicMenuItem(label: Int, enabled: Boolean = true, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(stringResource(label)) }, enabled = enabled, onClick = onClick)
}

@Composable
private fun SongPicker(
    songs: List<Song>, scanning: Boolean, connected: Boolean,
    onAdd: (Song) -> Unit, onScan: () -> Unit, onClose: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var count by rememberSaveable { mutableIntStateOf(0) }
    val visible = remember(query, songs) { songs.filter { it.title.contains(query, true) || it.artist.contains(query, true) } }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.classic_menu_add)) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text(stringResource(R.string.library_search_placeholder)) },
                )
                Spacer(Modifier.height(8.dp))
                if (!connected || visible.isEmpty()) {
                    Text(stringResource(when {
                        scanning -> R.string.classic_scanning
                        !connected -> R.string.classic_connecting
                        else -> R.string.classic_no_songs
                    }))
                } else LazyColumn(Modifier.height(248.dp)) {
                    items(visible, key = { it.id }) { song ->
                        Column(Modifier.fillMaxWidth().clickable { onAdd(song); count++ }.padding(vertical = 9.dp)) {
                            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                if (count > 0) Text(stringResource(R.string.classic_added, count), style = MaterialTheme.typography.labelLarge)
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.classic_done)) } },
        dismissButton = {
            TextButton(onClick = onScan, enabled = !scanning) { Text(stringResource(R.string.classic_rescan)) }
        },
    )
}

@Composable
private fun SaveQueueDialog(onClose: () -> Unit, onSave: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.classic_save_queue)) },
        text = {
            OutlinedTextField(name, { name = it }, singleLine = true,
                label = { Text(stringResource(R.string.classic_playlist_name)) })
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.classic_save))
            }
        },
        dismissButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.classic_cancel)) } },
    )
}
