package com.nexauren.drawmotion

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.awaitPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private const val BOARD_WIDTH = 960f
private const val BOARD_HEIGHT = 540f

private val AppBackground = Color(0xFF11131A)
private val PanelBackground = Color(0xFF1A1D27)
private val PanelBorder = Color(0xFF2C3040)
private val Accent = Color(0xFF8D7CFF)
private val TextPrimary = Color(0xFFF2F3F8)
private val TextMuted = Color(0xFF9AA2B4)

private enum class DrawingTool(val label: String, val symbol: String) {
    BRUSH("Pincel", "✎"),
    ERASER("Borracha", "⌫"),
    LINE("Linha", "／"),
    RECTANGLE("Retângulo", "□")
}

private data class InkStroke(
    val points: List<Offset>,
    val color: Int,
    val width: Float,
    val tool: DrawingTool
)

private class AnimationFrame(
    val number: Int,
    val strokes: MutableList<InkStroke> = mutableListOf()
)

private data class SavedProject(
    val frames: List<AnimationFrame>,
    val currentFrame: Int,
    val fps: Int
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                DrawmotionApp()
            }
        }
    }
}

@Composable
private fun DrawmotionApp() {
    val context = LocalContext.current
    val frames = remember { mutableStateListOf(AnimationFrame(1)) }
    var currentFrameIndex by remember { mutableIntStateOf(0) }
    var fps by remember { mutableIntStateOf(12) }
    var selectedTool by remember { mutableStateOf(DrawingTool.BRUSH) }
    var brushColor by remember { mutableIntStateOf(Color(0xFF272A35).toArgb()) }
    var brushSize by remember { mutableStateOf(8f) }
    var onionSkin by remember { mutableStateOf(true) }
    var zoom by remember { mutableStateOf(1f) }
    var rotation by remember { mutableStateOf(0f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var revision by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var showEffects by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    val undoStack = remember { mutableStateListOf<List<InkStroke>>() }
    val redoStack = remember { mutableStateListOf<List<InkStroke>>() }

    val activeFrame = frames.getOrNull(currentFrameIndex) ?: frames.first()

    LaunchedEffect(Unit) {
        val loaded = loadProject(context)
        if (loaded != null && loaded.frames.isNotEmpty()) {
            frames.clear()
            frames.addAll(loaded.frames)
            currentFrameIndex = loaded.currentFrame.coerceIn(0, frames.lastIndex)
            fps = loaded.fps.coerceIn(1, 30)
            revision++
            notice = "Projeto local carregado"
        }
    }

    LaunchedEffect(isPlaying, fps, frames.size) {
        while (isPlaying && frames.isNotEmpty()) {
            delay((1000L / fps.coerceAtLeast(1)).coerceAtLeast(1L))
            currentFrameIndex = (currentFrameIndex + 1) % frames.size
        }
    }

    LaunchedEffect(notice) {
        if (notice.isNotEmpty()) {
            delay(2200)
            notice = ""
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = renderFramePng(frames[currentFrameIndex])
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
                bitmap.recycle()
                notice = "Frame exportado em PNG"
            } catch (_: Exception) {
                notice = "Não foi possível exportar o PNG"
            }
        }
    }

    val commitStroke: (InkStroke) -> Unit = { stroke ->
        val target = frames[currentFrameIndex]
        undoStack.add(target.strokes.toList())
        if (undoStack.size > 100) undoStack.removeAt(0)
        redoStack.clear()
        target.strokes.add(stroke)
        revision++
    }

    val addFrame: (Boolean) -> Unit = { duplicate ->
        val nextNumber = (frames.maxOfOrNull { it.number } ?: 0) + 1
        val newStrokes = if (duplicate) activeFrame.strokes.toMutableList() else mutableListOf()
        frames.add(AnimationFrame(nextNumber, newStrokes))
        currentFrameIndex = frames.lastIndex
        undoStack.clear()
        redoStack.clear()
        revision++
        notice = if (duplicate) "Frame duplicado" else "Novo frame criado"
    }

    val undoAction: () -> Unit = {
        if (undoStack.isNotEmpty()) {
            redoStack.add(activeFrame.strokes.toList())
            val previous = undoStack.removeAt(undoStack.lastIndex)
            activeFrame.strokes.clear()
            activeFrame.strokes.addAll(previous)
            revision++
        }
    }

    val redoAction: () -> Unit = {
        if (redoStack.isNotEmpty()) {
            undoStack.add(activeFrame.strokes.toList())
            val next = redoStack.removeAt(redoStack.lastIndex)
            activeFrame.strokes.clear()
            activeFrame.strokes.addAll(next)
            revision++
        }
    }

    val saveAction: () -> Unit = {
        val ok = saveProject(context, frames, currentFrameIndex, fps)
        notice = if (ok) "Projeto guardado neste dispositivo" else "Falha ao guardar o projeto"
    }

    Surface(modifier = Modifier.fillMaxSize(), color = AppBackground) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HeaderBar(
                notice = notice,
                onSave = saveAction,
                onExport = {
                    exportLauncher.launch("drawmotion-frame-" + (currentFrameIndex + 1) + ".png")
                }
            )

            ToolSelector(selectedTool = selectedTool, onSelect = { selectedTool = it })

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                StageCanvas(
                    modifier = Modifier.fillMaxWidth(),
                    frames = frames,
                    currentFrameIndex = currentFrameIndex,
                    tool = selectedTool,
                    brushColor = brushColor,
                    brushSize = brushSize,
                    onionSkin = onionSkin,
                    zoom = zoom,
                    rotation = rotation,
                    pan = pan,
                    revision = revision,
                    onCommit = commitStroke,
                    onZoomChange = { zoom = it },
                    onRotationChange = { rotation = it },
                    onPanChange = { pan = it }
                )
            }

            ViewportControls(
                zoom = zoom,
                rotation = rotation,
                brushSize = brushSize,
                onZoomChange = { zoom = it.coerceIn(0.35f, 5f) },
                onRotationChange = { rotation = it },
                onBrushSizeChange = { brushSize = it },
                onReset = {
                    zoom = 1f
                    rotation = 0f
                    pan = Offset.Zero
                }
            )

            ColorPalette(selectedColor = brushColor, onSelect = { brushColor = it })

            TimelinePanel(
                frames = frames,
                currentFrameIndex = currentFrameIndex,
                fps = fps,
                onSelectFrame = {
                    currentFrameIndex = it
                    undoStack.clear()
                    redoStack.clear()
                },
                onAddFrame = { addFrame(false) },
                onDuplicateFrame = { addFrame(true) },
                onDeleteFrame = {
                    if (frames.size == 1) {
                        activeFrame.strokes.clear()
                        revision++
                        notice = "Frame limpo"
                    } else {
                        frames.removeAt(currentFrameIndex)
                        currentFrameIndex = currentFrameIndex.coerceAtMost(frames.lastIndex)
                        undoStack.clear()
                        redoStack.clear()
                        revision++
                        notice = "Frame removido"
                    }
                },
                onFpsChange = { fps = it.coerceIn(1, 30) }
            )

            BottomControls(
                playing = isPlaying,
                onionSkin = onionSkin,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                onPlay = { isPlaying = !isPlaying },
                onOnionSkin = { onionSkin = !onionSkin },
                onUndo = undoAction,
                onRedo = redoAction,
                onEffects = { showEffects = true }
            )

            Text(
                text = "Dica: desenha com um dedo. Usa dois dedos para mover, rodar e ampliar o palco.",
                color = TextMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }

    if (showEffects) {
        AlertDialog(
            onDismissRequest = { showEffects = false },
            containerColor = PanelBackground,
            title = { Text("Efeitos do frame", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EffectOption("Espelhar horizontalmente", "Inverte o desenho da esquerda para a direita") {
                        undoStack.add(activeFrame.strokes.toList())
                        activeFrame.strokes.replaceAll { stroke ->
                            stroke.copy(points = stroke.points.map { Offset(BOARD_WIDTH - it.x, it.y) })
                        }
                        revision++
                        showEffects = false
                    }
                    EffectOption("Inverter verticalmente", "Inverte o desenho de cima para baixo") {
                        undoStack.add(activeFrame.strokes.toList())
                        activeFrame.strokes.replaceAll { stroke ->
                            stroke.copy(points = stroke.points.map { Offset(it.x, BOARD_HEIGHT - it.y) })
                        }
                        revision++
                        showEffects = false
                    }
                    EffectOption("Monocromático", "Converte os traços para tons de cinzento") {
                        undoStack.add(activeFrame.strokes.toList())
                        activeFrame.strokes.replaceAll { stroke ->
                            val gray = ((AndroidColor.red(stroke.color) * 0.299) +
                                (AndroidColor.green(stroke.color) * 0.587) +
                                (AndroidColor.blue(stroke.color) * 0.114)).roundToInt()
                            stroke.copy(color = AndroidColor.rgb(gray, gray, gray))
                        }
                        revision++
                        showEffects = false
                    }
                    EffectOption("Limpar frame", "Remove todos os traços do frame selecionado") {
                        undoStack.add(activeFrame.strokes.toList())
                        activeFrame.strokes.clear()
                        revision++
                        showEffects = false
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEffects = false }) {
                    Text("Fechar", color = Accent)
                }
            }
        )
    }
}

@Composable
private fun HeaderBar(notice: String, onSave: () -> Unit, onExport: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(Accent),
            contentAlignment = Alignment.Center
        ) {
            Text("D", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("Drawmotion", color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text(
                text = if (notice.isNotEmpty()) notice else "ESTÚDIO DE ANIMAÇÃO 2D",
                color = if (notice.isNotEmpty()) Color(0xFFBBAFFF) else TextMuted,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        FilledTonalButton(
            onClick = onSave,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text("Guardar", fontSize = 11.sp)
        }
        Button(
            onClick = onExport,
            contentPadding = PaddingValues(horizontal = 11.dp, vertical = 8.dp)
        ) {
            Text("PNG", fontSize = 11.sp)
        }
    }
}

@Composable
private fun ToolSelector(selectedTool: DrawingTool, onSelect: (DrawingTool) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("FERRAMENTAS", color = TextMuted, fontWeight = FontWeight.Bold, fontSize = 10.sp)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            DrawingTool.values().forEach { tool ->
                FilterChip(
                    selected = selectedTool == tool,
                    onClick = { onSelect(tool) },
                    label = { Text(tool.label, fontSize = 12.sp) },
                    leadingIcon = { Text(tool.symbol, fontSize = 16.sp, color = if (selectedTool == tool) Accent else TextPrimary) }
                )
            }
        }
    }
}

@Composable
private fun StageCanvas(
    modifier: Modifier,
    frames: List<AnimationFrame>,
    currentFrameIndex: Int,
    tool: DrawingTool,
    brushColor: Int,
    brushSize: Float,
    onionSkin: Boolean,
    zoom: Float,
    rotation: Float,
    pan: Offset,
    revision: Int,
    onCommit: (InkStroke) -> Unit,
    onZoomChange: (Float) -> Unit,
    onRotationChange: (Float) -> Unit,
    onPanChange: (Offset) -> Unit
) {
    val previewState = remember { mutableStateOf<InkStroke?>(null) }
    val latestZoom = rememberUpdatedState(zoom)
    val latestRotation = rememberUpdatedState(rotation)
    val latestPan = rememberUpdatedState(pan)
    val latestColor = rememberUpdatedState(brushColor)
    val latestWidth = rememberUpdatedState(brushSize)
    val latestTool = rememberUpdatedState(tool)
    val latestOnCommit = rememberUpdatedState(onCommit)
    val latestZoomChange = rememberUpdatedState(onZoomChange)
    val latestRotationChange = rememberUpdatedState(onRotationChange)
    val latestPanChange = rememberUpdatedState(onPanChange)

    Canvas(
        modifier = modifier
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .pointerInput(tool, currentFrameIndex) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startTool = latestTool.value
                    val points = mutableListOf<Offset>()
                    points.add(pointerToBoard(down.position, size, latestZoom.value, latestRotation.value, latestPan.value))
                    var drawing = true
                    previewState.value = InkStroke(points.toList(), latestColor.value, latestWidth.value, startTool)

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.isEmpty()) {
                            event.changes.forEach { it.consume() }
                            break
                        }

                        if (pressed.size >= 2) {
                            if (drawing) {
                                if (points.isNotEmpty()) {
                                    latestOnCommit.value(
                                        InkStroke(points.toList(), latestColor.value, latestWidth.value, startTool)
                                    )
                                }
                                drawing = false
                                previewState.value = null
                            }

                            val first = pressed[0]
                            val second = pressed[1]
                            val prevA = normalizedPosition(first.previousPosition, size)
                            val prevB = normalizedPosition(second.previousPosition, size)
                            val nowA = normalizedPosition(first.position, size)
                            val nowB = normalizedPosition(second.position, size)
                            val prevCenter = Offset((prevA.x + prevB.x) / 2f, (prevA.y + prevB.y) / 2f)
                            val nowCenter = Offset((nowA.x + nowB.x) / 2f, (nowA.y + nowB.y) / 2f)
                            val prevDistance = distance(prevA, prevB)
                            val nowDistance = distance(nowA, nowB)

                            if (prevDistance > 1f && nowDistance > 1f) {
                                latestZoomChange.value(
                                    (latestZoom.value * (nowDistance / prevDistance)).coerceIn(0.35f, 5f)
                                )
                            }
                            latestPanChange.value(latestPan.value + (nowCenter - prevCenter))
                            if (prevDistance > 8f && nowDistance > 8f) {
                                val prevAngle = angleBetween(prevA, prevB)
                                val nowAngle = angleBetween(nowA, nowB)
                                latestRotationChange.value(latestRotation.value + normalizeAngle(nowAngle - prevAngle))
                            }
                        } else if (drawing) {
                            val boardPoint = pointerToBoard(
                                pressed.first().position,
                                size,
                                latestZoom.value,
                                latestRotation.value,
                                latestPan.value
                            )
                            if (points.isEmpty() || distance(points.last(), boardPoint) > 0.5f) {
                                points.add(boardPoint)
                                previewState.value = InkStroke(
                                    points.toList(),
                                    latestColor.value,
                                    latestWidth.value,
                                    startTool
                                )
                            }
                        }

                        event.changes.forEach { it.consume() }
                    }

                    if (drawing && points.isNotEmpty()) {
                        latestOnCommit.value(
                            InkStroke(points.toList(), latestColor.value, latestWidth.value, startTool)
                        )
                    }
                    previewState.value = null
                }
            }
    ) {
        val ignoredRevision = revision
        drawRect(Color.White, size = size)

        withTransform({
            scale(size.width / BOARD_WIDTH, size.height / BOARD_HEIGHT, pivot = Offset.Zero)
        }) {
            for (x in 0..960 step 48) {
                drawLine(
                    color = Color(0xFFEDEFF4),
                    start = Offset(x.toFloat(), 0f),
                    end = Offset(x.toFloat(), BOARD_HEIGHT),
                    strokeWidth = 1f
                )
            }
            for (y in 0..540 step 48) {
                drawLine(
                    color = Color(0xFFEDEFF4),
                    start = Offset(0f, y.toFloat()),
                    end = Offset(BOARD_WIDTH, y.toFloat()),
                    strokeWidth = 1f
                )
            }

            withTransform({
                translate(pan.x, pan.y)
                rotate(rotation, pivot = Offset(BOARD_WIDTH / 2f, BOARD_HEIGHT / 2f))
                scale(zoom, zoom, pivot = Offset(BOARD_WIDTH / 2f, BOARD_HEIGHT / 2f))
            }) {
                if (onionSkin && currentFrameIndex > 0) {
                    frames[currentFrameIndex - 1].strokes.forEach {
                        drawInkStroke(it, opacity = 0.24f, overrideColor = Color(0xFFEF6C86))
                    }
                }
                if (onionSkin && currentFrameIndex < frames.lastIndex) {
                    frames[currentFrameIndex + 1].strokes.forEach {
                        drawInkStroke(it, opacity = 0.22f, overrideColor = Color(0xFF55B8F2))
                    }
                }
                frames.getOrNull(currentFrameIndex)?.strokes?.forEach { drawInkStroke(it) }
                previewState.value?.let { drawInkStroke(it) }
            }
        }
        if (ignoredRevision == Int.MIN_VALUE) drawRect(Color.Transparent)
    }
}

private fun pointerToBoard(position: Offset, bounds: IntSize, zoom: Float, rotation: Float, pan: Offset): Offset {
    val normalized = normalizedPosition(position, bounds)
    val dx = normalized.x - BOARD_WIDTH / 2f - pan.x
    val dy = normalized.y - BOARD_HEIGHT / 2f - pan.y
    val radians = Math.toRadians(rotation.toDouble())
    val c = cos(radians).toFloat()
    val s = sin(radians).toFloat()
    val localX = (dx * c + dy * s) / zoom
    val localY = (-dx * s + dy * c) / zoom
    return Offset(localX + BOARD_WIDTH / 2f, localY + BOARD_HEIGHT / 2f)
}

private fun normalizedPosition(position: Offset, bounds: IntSize): Offset {
    val xScale = if (bounds.width <= 0) 1f else BOARD_WIDTH / bounds.width.toFloat()
    val yScale = if (bounds.height <= 0) 1f else BOARD_HEIGHT / bounds.height.toFloat()
    return Offset(position.x * xScale, position.y * yScale)
}

private fun distance(a: Offset, b: Offset): Float = hypot(a.x - b.x, a.y - b.y)

private fun angleBetween(a: Offset, b: Offset): Float =
    Math.toDegrees(atan2((b.y - a.y).toDouble(), (b.x - a.x).toDouble())).toFloat()

private fun normalizeAngle(angle: Float): Float {
    var result = angle
    while (result > 180f) result -= 360f
    while (result < -180f) result += 360f
    return result
}

private fun DrawScope.drawInkStroke(
    stroke: InkStroke,
    opacity: Float = 1f,
    overrideColor: Color? = null
) {
    if (stroke.points.isEmpty()) return
    val inkColor = overrideColor ?: if (stroke.tool == DrawingTool.ERASER) Color.White else Color(stroke.color)
    val finalColor = inkColor.copy(alpha = inkColor.alpha * opacity)
    val width = stroke.width.coerceAtLeast(1f)
    val first = stroke.points.first()
    val last = stroke.points.last()

    when (stroke.tool) {
        DrawingTool.LINE -> {
            if (stroke.points.size == 1) {
                drawCircle(finalColor, radius = width / 2f, center = first)
            } else {
                drawLine(finalColor, first, last, strokeWidth = width, cap = StrokeCap.Round)
            }
        }
        DrawingTool.RECTANGLE -> {
            if (stroke.points.size == 1) {
                drawCircle(finalColor, radius = width / 2f, center = first)
            } else {
                val left = min(first.x, last.x)
                val top = min(first.y, last.y)
                val right = max(first.x, last.x)
                val bottom = max(first.y, last.y)
                drawRect(
                    color = finalColor,
                    topLeft = Offset(left, top),
                    size = Size(right - left, bottom - top),
                    style = Stroke(width = width)
                )
            }
        }
        DrawingTool.BRUSH, DrawingTool.ERASER -> {
            if (stroke.points.size == 1) {
                drawCircle(finalColor, radius = width / 2f, center = first)
            } else {
                val path = Path().apply {
                    moveTo(first.x, first.y)
                    stroke.points.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(
                    path = path,
                    color = finalColor,
                    style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}

@Composable
private fun ViewportControls(
    zoom: Float,
    rotation: Float,
    brushSize: Float,
    onZoomChange: (Float) -> Unit,
    onRotationChange: (Float) -> Unit,
    onBrushSizeChange: (Float) -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(PanelBackground)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("PALCO", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text((zoom * 100f).roundToInt().toString() + "%", color = TextPrimary, fontSize = 11.sp)
            Text("   " + rotation.roundToInt() + "°", color = TextPrimary, fontSize = 11.sp)
            TextButton(onClick = onReset, contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp)) {
                Text("Repor", color = Accent, fontSize = 11.sp)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Zoom", color = TextMuted, fontSize = 11.sp)
            CompactTextButton("−", onClick = { onZoomChange(zoom - 0.15f) })
            CompactTextButton("+", onClick = { onZoomChange(zoom + 0.15f) })
            CompactTextButton("↶", onClick = { onRotationChange(rotation - 15f) })
            CompactTextButton("↷", onClick = { onRotationChange(rotation + 15f) })
            Spacer(Modifier.weight(1f))
            Text("Pincel " + brushSize.roundToInt(), color = TextMuted, fontSize = 10.sp)
        }
        Slider(
            value = brushSize,
            onValueChange = onBrushSizeChange,
            valueRange = 2f..28f,
            modifier = Modifier.fillMaxWidth().height(26.dp)
        )
    }
}

@Composable
private fun CompactTextButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 30.dp, height = 28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF272B37))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ColorPalette(selectedColor: Int, onSelect: (Int) -> Unit) {
    val colors = listOf(
        0xFF272A35.toInt(),
        0xFFEF476F.toInt(),
        0xFFFF8A3D.toInt(),
        0xFFFFD166.toInt(),
        0xFF06D6A0.toInt(),
        0xFF21A0F6.toInt(),
        0xFF8D7CFF.toInt(),
        0xFFFFFFFF.toInt()
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(PanelBackground)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("COR", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text(if (selectedColor == AndroidColor.WHITE) "Branco / borracha" else "Paleta rápida", color = TextPrimary, fontSize = 11.sp)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            colors.forEach { colorInt ->
                val selected = selectedColor == colorInt
                Box(
                    modifier = Modifier
                        .size(if (selected) 29.dp else 25.dp)
                        .clip(CircleShape)
                        .background(Color(colorInt))
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) Accent else Color(0xFF454A5A),
                            shape = CircleShape
                        )
                        .clickable { onSelect(colorInt) }
                )
            }
        }
    }
}

@Composable
private fun TimelinePanel(
    frames: List<AnimationFrame>,
    currentFrameIndex: Int,
    fps: Int,
    onSelectFrame: (Int) -> Unit,
    onAddFrame: () -> Unit,
    onDuplicateFrame: () -> Unit,
    onDeleteFrame: () -> Unit,
    onFpsChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(PanelBackground)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("TIMELINE", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            CompactTextButton("−", onClick = { onFpsChange(fps - 1) })
            Text(fps.toString() + " FPS", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            CompactTextButton("+", onClick = { onFpsChange(fps + 1) })
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            frames.forEachIndexed { index, frame ->
                FrameTile(
                    number = frame.number,
                    selected = currentFrameIndex == index,
                    strokeCount = frame.strokes.size,
                    onClick = { onSelectFrame(index) }
                )
            }
            Box(
                modifier = Modifier
                    .size(width = 62.dp, height = 59.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, PanelBorder, RoundedCornerShape(10.dp))
                    .background(Color(0xFF20232E))
                    .clickable(onClick = onAddFrame),
                contentAlignment = Alignment.Center
            ) {
                Text("+", color = Accent, fontSize = 25.sp)
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onAddFrame, contentPadding = PaddingValues(horizontal = 9.dp, vertical = 6.dp)) {
                Text("+ Frame", fontSize = 10.sp, maxLines = 1)
            }
            OutlinedButton(onClick = onDuplicateFrame, contentPadding = PaddingValues(horizontal = 9.dp, vertical = 6.dp)) {
                Text("Duplicar", fontSize = 10.sp, maxLines = 1)
            }
            OutlinedButton(onClick = onDeleteFrame, contentPadding = PaddingValues(horizontal = 9.dp, vertical = 6.dp)) {
                Text("Apagar", fontSize = 10.sp, maxLines = 1)
            }
            Spacer(Modifier.weight(1f))
            Text("  " + (currentFrameIndex + 1) + "/" + frames.size, color = TextMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun FrameTile(number: Int, selected: Boolean, strokeCount: Int, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .size(width = 62.dp, height = 59.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color(0xFF332E56) else Color(0xFF222631))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Accent else PanelBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(number.toString().padStart(2, '0'), color = if (selected) Color(0xFFC7BDFF) else TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(5.dp).clip(CircleShape).background(if (strokeCount > 0) Color(0xFF67DDB5) else TextMuted))
            Text("  " + strokeCount + " traços", color = TextMuted, fontSize = 8.sp, maxLines = 1)
        }
    }
}

@Composable
private fun BottomControls(
    playing: Boolean,
    onionSkin: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onPlay: () -> Unit,
    onOnionSkin: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onEffects: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(onClick = onPlay, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp)) {
            Text(if (playing) "Ⅱ Pausa" else "▶ Pré-visualizar", fontSize = 11.sp, maxLines = 1)
        }
        FilterChip(
            selected = onionSkin,
            onClick = onOnionSkin,
            label = { Text("Onion skin", fontSize = 10.sp) }
        )
        OutlinedButton(
            onClick = onUndo,
            enabled = canUndo,
            contentPadding = PaddingValues(horizontal = 9.dp, vertical = 7.dp)
        ) { Text("↶", fontSize = 17.sp) }
        OutlinedButton(
            onClick = onRedo,
            enabled = canRedo,
            contentPadding = PaddingValues(horizontal = 9.dp, vertical = 7.dp)
        ) { Text("↷", fontSize = 17.sp) }
        FilledTonalButton(onClick = onEffects, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)) {
            Text("Efeitos", fontSize = 11.sp)
        }
    }
}

@Composable
private fun EffectOption(title: String, description: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF252936))
            .clickable(onClick = onClick)
            .padding(11.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(description, color = TextMuted, fontSize = 10.sp, lineHeight = 14.sp)
    }
}

private fun saveProject(
    context: Context,
    frames: List<AnimationFrame>,
    currentFrameIndex: Int,
    fps: Int
): Boolean {
    return try {
        val root = JSONObject()
            .put("formatVersion", 1)
            .put("currentFrame", currentFrameIndex)
            .put("fps", fps)
        val frameArray = JSONArray()
        frames.forEach { frame ->
            val frameObject = JSONObject().put("number", frame.number)
            val strokesArray = JSONArray()
            frame.strokes.forEach { stroke ->
                val strokeObject = JSONObject()
                    .put("color", stroke.color)
                    .put("width", stroke.width.toDouble())
                    .put("tool", stroke.tool.name)
                val pointsArray = JSONArray()
                stroke.points.forEach { point ->
                    pointsArray.put(JSONObject().put("x", point.x.toDouble()).put("y", point.y.toDouble()))
                }
                strokeObject.put("points", pointsArray)
                strokesArray.put(strokeObject)
            }
            frameObject.put("strokes", strokesArray)
            frameArray.put(frameObject)
        }
        root.put("frames", frameArray)
        context.getSharedPreferences("drawmotion-project", Context.MODE_PRIVATE)
            .edit().putString("project-json", root.toString()).commit()
    } catch (_: Exception) {
        false
    }
}

private fun loadProject(context: Context): SavedProject? {
    return try {
        val data = context.getSharedPreferences("drawmotion-project", Context.MODE_PRIVATE)
            .getString("project-json", null) ?: return null
        val root = JSONObject(data)
        val frameArray = root.getJSONArray("frames")
        val loadedFrames = mutableListOf<AnimationFrame>()
        for (i in 0 until frameArray.length()) {
            val frameObject = frameArray.getJSONObject(i)
            val frame = AnimationFrame(frameObject.optInt("number", i + 1))
            val strokesArray = frameObject.optJSONArray("strokes") ?: JSONArray()
            for (j in 0 until strokesArray.length()) {
                val strokeObject = strokesArray.getJSONObject(j)
                val tool = runCatching {
                    DrawingTool.valueOf(strokeObject.optString("tool", DrawingTool.BRUSH.name))
                }.getOrDefault(DrawingTool.BRUSH)
                val pointsArray = strokeObject.optJSONArray("points") ?: JSONArray()
                val points = mutableListOf<Offset>()
                for (k in 0 until pointsArray.length()) {
                    val point = pointsArray.getJSONObject(k)
                    points.add(Offset(point.optDouble("x", 0.0).toFloat(), point.optDouble("y", 0.0).toFloat()))
                }
                frame.strokes.add(
                    InkStroke(
                        points = points,
                        color = strokeObject.optInt("color", AndroidColor.BLACK),
                        width = strokeObject.optDouble("width", 8.0).toFloat().coerceAtLeast(1f),
                        tool = tool
                    )
                )
            }
            loadedFrames.add(frame)
        }
        if (loadedFrames.isEmpty()) return null
        SavedProject(
            frames = loadedFrames,
            currentFrame = root.optInt("currentFrame", 0),
            fps = root.optInt("fps", 12)
        )
    } catch (_: Exception) {
        null
    }
}

private fun renderFramePng(frame: AnimationFrame): Bitmap {
    val bitmap = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    canvas.drawColor(AndroidColor.WHITE)
    canvas.scale(2f, 2f)

    frame.strokes.forEach { stroke ->
        if (stroke.points.isEmpty()) return@forEach
        val color = if (stroke.tool == DrawingTool.ERASER) AndroidColor.WHITE else stroke.color
        val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            strokeWidth = stroke.width.coerceAtLeast(1f)
            strokeCap = AndroidPaint.Cap.ROUND
            strokeJoin = AndroidPaint.Join.ROUND
            style = if (stroke.tool == DrawingTool.RECTANGLE) AndroidPaint.Style.STROKE else AndroidPaint.Style.STROKE
        }
        val first = stroke.points.first()
        val last = stroke.points.last()
        when (stroke.tool) {
            DrawingTool.RECTANGLE -> {
                if (stroke.points.size == 1) {
                    canvas.drawCircle(first.x, first.y, stroke.width / 2f, paint)
                } else {
                    canvas.drawRect(
                        RectF(min(first.x, last.x), min(first.y, last.y), max(first.x, last.x), max(first.y, last.y)),
                        paint
                    )
                }
            }
            DrawingTool.LINE -> {
                if (stroke.points.size == 1) {
                    canvas.drawCircle(first.x, first.y, stroke.width / 2f, paint)
                } else {
                    canvas.drawLine(first.x, first.y, last.x, last.y, paint)
                }
            }
            DrawingTool.BRUSH, DrawingTool.ERASER -> {
                if (stroke.points.size == 1) {
                    paint.style = AndroidPaint.Style.FILL
                    canvas.drawCircle(first.x, first.y, stroke.width / 2f, paint)
                } else {
                    val path = AndroidPath().apply {
                        moveTo(first.x, first.y)
                        stroke.points.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    canvas.drawPath(path, paint)
                }
            }
        }
    }
    return bitmap
}
