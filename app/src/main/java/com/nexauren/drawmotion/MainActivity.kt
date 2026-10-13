package com.nexauren.drawmotion

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.DashPathEffect
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import android.graphics.RectF
import android.media.Image
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.OutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import com.squareup.gifencoder.GifEncoder
import com.squareup.gifencoder.ImageOptions
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
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
    BRUSH("Pincéis", "✎"),
    ERASER("Borracha", "⌫"),
    LINE("Linha", "／"),
    RULER("Régua", "▤"),
    RECTANGLE("Retângulo", "□"),
    ELLIPSE("Elipse", "◯")
}

private enum class DrawingInputMode(val label: String) {
    BOTH("Dedo + caneta"),
    TOUCH("Só dedo"),
    STYLUS("Só caneta")
}

private enum class BrushPreset(
    val label: String,
    val description: String,
    val symbol: String,
    val widthScale: Float,
    val opacity: Float,
    val dashed: Boolean = false,
    val squareCaps: Boolean = false
) {
    INK("Tinta fina", "Contorno preciso e definido", "✒", 1f, 1f),
    PENCIL("Lápis", "Esboço leve e discreto", "✎", 0.72f, 0.68f),
    MARKER("Marcador", "Traço espesso e saturado", "▬", 1.55f, 0.72f),
    AIRBRUSH("Aerógrafo", "Traço largo e suave", "☁", 2.1f, 0.22f),
    CRAYON("Giz de cera", "Textura tracejada", "▧", 1.15f, 0.9f, dashed = true),
    HIGHLIGHTER("Marca-texto", "Realce translúcido", "▰", 2.25f, 0.25f),
    PASTEL("Pastel macio", "Cor suave e difusa", "●", 1.65f, 0.42f),
    PIXEL("Pixel", "Traço de arestas quadradas", "▪", 1.05f, 1f, squareCaps = true)
}

private enum class CanvasMaterial(
    val title: String,
    val description: String,
    val backgroundColor: Int,
    val gridColor: Int?
) {
    PAPER("Papel branco", "Superfície limpa", AndroidColor.WHITE, null),
    GRID("Papel quadriculado", "Grelha fina para desenhar", AndroidColor.WHITE, 0xFFE9EDF5.toInt()),
    CREAM("Papel creme", "Tom quente e suave", 0xFFFFF4DC.toInt(), 0xFFF0E3C6.toInt()),
    BLUEPRINT("Papel azul", "Grelha técnica azul", 0xFF123459.toInt(), 0xFF294A6D.toInt()),
    NIGHT("Noite", "Fundo escuro para cenas luminosas", 0xFF101522.toInt(), null)
}

private enum class DemoProject(
    val title: String,
    val description: String,
    val fps: Int,
    val material: CanvasMaterial,
    val frameCount: Int
) {
    COSMIC_BOUNCE(
        "Bola cósmica",
        "Uma esfera luminosa salta entre estrelas e deixa um rasto colorido.",
        12,
        CanvasMaterial.NIGHT,
        12
    ),
    SUNSET(
        "Sol de verão",
        "Sol, nuvens e colinas em movimento, com cores quentes.",
        12,
        CanvasMaterial.CREAM,
        12
    ),
    FLOWER_BLOOM(
        "Flor a florescer",
        "Pétalas abrem-se à volta do centro enquanto o caule permanece no lugar.",
        12,
        CanvasMaterial.PAPER,
        12
    )
}

private data class InkStroke(
    val points: List<Offset>,
    val color: Int,
    val width: Float,
    val tool: DrawingTool,
    val brush: BrushPreset = BrushPreset.INK
)

private class AnimationFrame(
    val number: Int,
    val strokes: MutableList<InkStroke> = mutableListOf()
)

private data class SavedProject(
    val frames: List<AnimationFrame>,
    val currentFrame: Int,
    val fps: Int,
    val material: CanvasMaterial = CanvasMaterial.GRID,
    val projectName: String = "Animação recente"
)

private data class ProjectInfo(
    val name: String,
    val frameCount: Int,
    val strokeCount: Int,
    val fps: Int,
    val material: CanvasMaterial,
    val updatedAt: Long
)

private fun demoStroke(
    points: List<Offset>,
    color: Int,
    width: Float,
    brush: BrushPreset = BrushPreset.INK,
    tool: DrawingTool = DrawingTool.BRUSH
): InkStroke = InkStroke(points, color, width, tool, brush)

private fun createDemoFrames(demo: DemoProject): List<AnimationFrame> {
    return (0 until demo.frameCount).map { frameIndex ->
        val strokes = mutableListOf<InkStroke>()
        val phase = frameIndex.toFloat() / demo.frameCount * 6.283185f
        when (demo) {
            DemoProject.COSMIC_BOUNCE -> {
                for (n in 0..34) {
                    val x = ((n * 149 + 37) % 930 + 15).toFloat()
                    val y = ((n * 83 + 19) % 500 + 15).toFloat()
                    val color = when (n % 4) {
                        0 -> 0xFFFFD166.toInt()
                        1 -> 0xFFBBAFFF.toInt()
                        2 -> 0xFF70D8F4.toInt()
                        else -> 0xFF7F8CAB.toInt()
                    }
                    strokes.add(demoStroke(listOf(Offset(x, y)), color, if (n % 5 == 0) 7f else 3.5f))
                }
                strokes.add(
                    demoStroke(
                        listOf(Offset(20f, 445f), Offset(940f, 445f)),
                        0xFF525978.toInt(),
                        3f
                    )
                )
                val x = 95f + frameIndex * 69f
                val y = 328f - abs(sin((phase * 1.5f).toDouble()).toFloat()) * 128f
                for (tail in 4 downTo 1) {
                    strokes.add(
                        demoStroke(
                            listOf(Offset(x - tail * 20f, y + tail * 4f)),
                            0x2D8D7CFF,
                            7f + tail * 3f,
                            BrushPreset.AIRBRUSH
                        )
                    )
                }
                strokes.add(
                    demoStroke(
                        listOf(Offset(x - 54f, y - 17f), Offset(x + 54f, y + 17f)),
                        0x887C6AFF.toInt(),
                        3f,
                        BrushPreset.PASTEL,
                        DrawingTool.ELLIPSE
                    )
                )
                strokes.add(demoStroke(listOf(Offset(x, y)), 0xFF21A0F6.toInt(), 78f))
                strokes.add(demoStroke(listOf(Offset(x + 3f, y + 2f)), 0xFF8D7CFF.toInt(), 54f))
                strokes.add(demoStroke(listOf(Offset(x + 7f, y + 3f)), 0xFFFF7DA7.toInt(), 28f))
                strokes.add(demoStroke(listOf(Offset(x - 10f, y - 11f)), AndroidColor.WHITE, 12f))
            }
            DemoProject.SUNSET -> {
                val sunX = 480f + sin(phase.toDouble()).toFloat() * 36f
                val sunY = 222f + sin((phase * 1.3f).toDouble()).toFloat() * 18f
                strokes.add(demoStroke(listOf(Offset(sunX, sunY)), 0x2DFF9B54, 164f, BrushPreset.AIRBRUSH))
                strokes.add(demoStroke(listOf(Offset(sunX, sunY)), 0xFFFF9B54.toInt(), 122f))
                strokes.add(demoStroke(listOf(Offset(sunX, sunY + 2f)), 0xFFFFD166.toInt(), 96f))
                strokes.add(demoStroke(listOf(Offset(sunX, sunY + 4f)), 0xFFFFF0B8.toInt(), 64f))

                val cloudX = 125f + (frameIndex * 28f) % 760f
                listOf(
                    Offset(cloudX - 30f, 115f),
                    Offset(cloudX, 98f),
                    Offset(cloudX + 28f, 111f),
                    Offset(cloudX + 45f, 116f)
                ).forEach { center ->
                    strokes.add(demoStroke(listOf(center), 0xFFE7EDF6.toInt(), 35f, BrushPreset.PASTEL))
                }

                strokes.add(
                    demoStroke(
                        listOf(
                            Offset(-20f, 405f), Offset(175f, 354f),
                            Offset(342f, 407f), Offset(515f, 320f),
                            Offset(715f, 391f), Offset(980f, 335f)
                        ),
                        0xFF7B6BB2.toInt(),
                        44f,
                        BrushPreset.PASTEL
                    )
                )
                strokes.add(
                    demoStroke(
                        listOf(
                            Offset(-20f, 470f), Offset(160f, 438f),
                            Offset(365f, 474f), Offset(540f, 410f),
                            Offset(750f, 460f), Offset(980f, 425f)
                        ),
                        0xFF528A9A.toInt(),
                        50f,
                        BrushPreset.PASTEL
                    )
                )
                strokes.add(
                    demoStroke(
                        listOf(
                            Offset(-15f, 518f), Offset(190f, 490f),
                            Offset(410f, 512f), Offset(650f, 465f),
                            Offset(820f, 504f), Offset(980f, 480f)
                        ),
                        0xFF397B71.toInt(),
                        48f,
                        BrushPreset.PASTEL
                    )
                )
                for (bird in 0..2) {
                    val bx = 300f + bird * 52f + sin(phase.toDouble()).toFloat() * 10f
                    val by = 160f + bird * 20f
                    strokes.add(
                        demoStroke(
                            listOf(Offset(bx - 10f, by), Offset(bx, by + 6f), Offset(bx + 10f, by)),
                            0xFF856E8E.toInt(),
                            3f,
                            BrushPreset.PENCIL
                        )
                    )
                }
            }
            DemoProject.FLOWER_BLOOM -> {
                val cx = 480f
                val cy = 240f
                strokes.add(
                    demoStroke(
                        listOf(
                            Offset(cx, cy + 16f), Offset(cx + 3f, 310f),
                            Offset(cx + 13f, 390f), Offset(cx + 8f, 493f)
                        ),
                        0xFF4B9B67.toInt(),
                        12f,
                        BrushPreset.PASTEL
                    )
                )
                strokes.add(
                    demoStroke(
                        listOf(Offset(cx + 4f, 405f), Offset(cx - 24f, 376f), Offset(cx - 80f, 362f), Offset(cx - 52f, 405f)),
                        0xFF60AD75.toInt(),
                        15f,
                        BrushPreset.PASTEL
                    )
                )
                strokes.add(
                    demoStroke(
                        listOf(Offset(cx + 5f, 424f), Offset(cx + 34f, 389f), Offset(cx + 94f, 366f), Offset(cx + 62f, 414f)),
                        0xFF367E59.toInt(),
                        15f,
                        BrushPreset.PASTEL
                    )
                )
                val petalRadius = 27f + frameIndex * 4.4f
                val petalColors = listOf(
                    0xFFFF86B7.toInt(), 0xFFC5A2FF.toInt(),
                    0xFFFFB1CF.toInt(), 0xFFFF86B7.toInt(),
                    0xFFBBAFFF.toInt(), 0xFFFFB1CF.toInt(),
                    0xFFFF86B7.toInt(), 0xFFC5A2FF.toInt()
                )
                for (petal in 0..7) {
                    val angle = petal * (Math.PI / 4.0) + phase * 0.035
                    val end = Offset(
                        cx + cos(angle).toFloat() * petalRadius,
                        cy + sin(angle).toFloat() * petalRadius
                    )
                    strokes.add(
                        demoStroke(
                            listOf(Offset(cx, cy), end),
                            petalColors[petal],
                            27f,
                            BrushPreset.PASTEL
                        )
                    )
                }
                strokes.add(demoStroke(listOf(Offset(cx, cy)), 0xFFFFD166.toInt(), 46f))
                strokes.add(demoStroke(listOf(Offset(cx - 4f, cy - 4f)), 0xFFFFF0B8.toInt(), 19f))
            }
        }
        AnimationFrame(frameIndex + 1, strokes)
    }
}

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
    val activity = context.findActivity()
    val settingsPrefs = remember { context.getSharedPreferences("drawmotion-settings", Context.MODE_PRIVATE) }
    var page by remember { mutableStateOf(AppPage.HOME) }
    var selectedBrush by remember { mutableStateOf(BrushPreset.INK) }
    var canvasMaterial by remember { mutableStateOf(CanvasMaterial.GRID) }
    var projectName by remember { mutableStateOf("Animação recente") }
    var inputMode by remember { mutableStateOf(DrawingInputMode.BOTH) }
    var projectCatalog = remember { mutableStateListOf<ProjectInfo>() }
    var defaultFps by remember { mutableIntStateOf(settingsPrefs.getInt("default-fps", 12).coerceIn(1, 30)) }
    var defaultOnionSkin by remember { mutableStateOf(settingsPrefs.getBoolean("default-onion-skin", true)) }
    var immersiveEditor by remember { mutableStateOf(settingsPrefs.getBoolean("immersive-editor", true)) }

    val frames = remember { mutableStateListOf(AnimationFrame(1)) }
    var currentFrameIndex by remember { mutableIntStateOf(0) }
    var fps by remember { mutableIntStateOf(defaultFps) }
    var selectedTool by remember { mutableStateOf(DrawingTool.BRUSH) }
    var brushColor by remember { mutableIntStateOf(Color(0xFF272A35).toArgb()) }
    var brushSize by remember { mutableStateOf(8f) }
    var onionSkin by remember { mutableStateOf(defaultOnionSkin) }
    var zoom by remember { mutableStateOf(1f) }
    var rotation by remember { mutableStateOf(0f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var revision by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var showEffects by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showBrushesDialog by remember { mutableStateOf(false) }
    var showMaterialsDialog by remember { mutableStateOf(false) }
    val exportScope = rememberCoroutineScope()
    var notice by remember { mutableStateOf("") }
    val undoStack = remember { mutableStateListOf<List<InkStroke>>() }
    val redoStack = remember { mutableStateListOf<List<InkStroke>>() }
    val refreshProjectCatalog: () -> Unit = {
        projectCatalog.clear()
        projectCatalog.addAll(listProjectInfos(context))
    }

    val activeFrame = frames.getOrNull(currentFrameIndex) ?: frames.first()

    LaunchedEffect(page, immersiveEditor) {
        if (activity != null) {
            activity.requestedOrientation = if (page == AppPage.EDITOR) {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
            val bars = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
            if (page == AppPage.EDITOR && immersiveEditor) {
                bars.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                bars.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                bars.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    BackHandler(enabled = page != AppPage.HOME) {
        isPlaying = false
        showEffects = false
        showExportDialog = false
        showBrushesDialog = false
        showMaterialsDialog = false
        page = AppPage.HOME
    }

    LaunchedEffect(Unit) {
        val loaded = loadProject(context)
        if (loaded != null && loaded.frames.isNotEmpty()) {
            frames.clear()
            frames.addAll(loaded.frames)
            currentFrameIndex = loaded.currentFrame.coerceIn(0, frames.lastIndex)
            fps = loaded.fps.coerceIn(1, 30)
            canvasMaterial = loaded.material
            projectName = loaded.projectName
            revision++
            notice = "Projeto local carregado"
        }
        refreshProjectCatalog()
    }

    LaunchedEffect(page, revision, projectName) {
        if (page == AppPage.PROJECTS) refreshProjectCatalog()
    }

    LaunchedEffect(isPlaying, fps, frames.size) {
        while (isPlaying && frames.isNotEmpty()) {
            delay((1000L / fps.coerceAtLeast(1)).coerceAtLeast(1L))
            currentFrameIndex = (currentFrameIndex + 1) % frames.size
        }
    }

    // Autosave after edits, but never serialize every frame while preview playback is running.
    LaunchedEffect(revision, fps, frames.size, currentFrameIndex, isPlaying, projectName, canvasMaterial) {
        if (!isPlaying) {
            delay(650)
            saveProject(context, frames, currentFrameIndex, fps, canvasMaterial, projectName)
        }
    }

    LaunchedEffect(notice) {
        if (notice.isNotEmpty()) {
            delay(2200)
            notice = ""
        }
    }

    val pngExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri: Uri? ->
        val frame = frames.getOrNull(currentFrameIndex)
        if (uri != null && frame != null) {
            exportScope.launch {
                val ok = withContext(Dispatchers.IO) {
                    try {
                        val output = context.contentResolver.openOutputStream(uri)
                            ?: throw IOException("Não foi possível abrir o ficheiro de destino")
                        output.use { stream ->
                            val bitmap = renderFrameBitmap(frame, 1920, 1080, canvasMaterial)
                            try {
                                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                                    throw IOException("A imagem PNG não foi codificada")
                                }
                            } finally {
                                bitmap.recycle()
                            }
                        }
                        true
                    } catch (_: Exception) {
                        false
                    }
                }
                notice = if (ok) "Frame exportado em PNG" else "Falha ao exportar PNG"
            }
        }
    }

    val jpegExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/jpeg")
    ) { uri: Uri? ->
        val frame = frames.getOrNull(currentFrameIndex)
        if (uri != null && frame != null) {
            exportScope.launch {
                val ok = withContext(Dispatchers.IO) {
                    try {
                        val output = context.contentResolver.openOutputStream(uri)
                            ?: throw IOException("Não foi possível abrir o ficheiro de destino")
                        output.use { stream ->
                            val bitmap = renderFrameBitmap(frame, 1920, 1080, canvasMaterial)
                            try {
                                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                                    throw IOException("A imagem JPEG não foi codificada")
                                }
                            } finally {
                                bitmap.recycle()
                            }
                        }
                        true
                    } catch (_: Exception) {
                        false
                    }
                }
                notice = if (ok) "Frame exportado em JPEG" else "Falha ao exportar JPEG"
            }
        }
    }

    val gifExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/gif")
    ) { uri: Uri? ->
        val frameSnapshot = frames.toList()
        val fpsSnapshot = fps
        val materialSnapshot = canvasMaterial
        if (uri != null && frameSnapshot.isNotEmpty()) {
            exportScope.launch {
                val ok = withContext(Dispatchers.IO) {
                    try {
                        val output = context.contentResolver.openOutputStream(uri)
                            ?: throw IOException("Não foi possível abrir o ficheiro de destino")
                        output.use { stream -> exportAnimatedGif(stream, frameSnapshot, fpsSnapshot, materialSnapshot) }
                        true
                    } catch (_: Exception) {
                        false
                    }
                }
                notice = if (ok) "Animação exportada em GIF" else "Falha ao exportar GIF"
            }
        }
    }

    val pngSequenceExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        val frameSnapshot = frames.toList()
        val fpsSnapshot = fps
        val materialSnapshot = canvasMaterial
        if (uri != null && frameSnapshot.isNotEmpty()) {
            exportScope.launch {
                val ok = withContext(Dispatchers.IO) {
                    try {
                        val output = context.contentResolver.openOutputStream(uri)
                            ?: throw IOException("Não foi possível abrir o ficheiro de destino")
                        output.use { stream -> exportPngSequenceZip(stream, frameSnapshot, fpsSnapshot, materialSnapshot) }
                        true
                    } catch (_: Exception) {
                        false
                    }
                }
                notice = if (ok) "Sequência PNG exportada em ZIP" else "Falha ao exportar sequência"
            }
        }
    }

    val mp4ExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("video/mp4")
    ) { uri: Uri? ->
        val frameSnapshot = frames.map { AnimationFrame(it.number, it.strokes.toMutableList()) }
        val fpsSnapshot = fps
        val materialSnapshot = canvasMaterial
        if (uri != null && frameSnapshot.isNotEmpty()) {
            isPlaying = false
            exportScope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        exportAnimationMp4(context, frameSnapshot, fpsSnapshot, materialSnapshot, uri)
                    }.getOrDefault(false)
                }
                notice = if (ok) "Animação exportada em MP4" else "Falha ao exportar MP4; verifica o suporte de vídeo do dispositivo"
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
        val ok = saveProject(context, frames, currentFrameIndex, fps, canvasMaterial, projectName)
        refreshProjectCatalog()
        notice = if (ok) "Projeto guardado neste dispositivo" else "Falha ao guardar o projeto"
    }

    val createNamedProject: (String) -> Unit = { requestedName ->
        val name = requestedName.trim()
        if (name.isNotBlank()) {
            saveProject(context, frames, currentFrameIndex, fps, canvasMaterial, projectName)
            frames.clear()
            frames.add(AnimationFrame(1))
            currentFrameIndex = 0
            fps = defaultFps
            selectedTool = DrawingTool.BRUSH
            selectedBrush = BrushPreset.INK
            canvasMaterial = CanvasMaterial.GRID
            brushColor = Color(0xFF272A35).toArgb()
            brushSize = 8f
            onionSkin = defaultOnionSkin
            zoom = 1f
            rotation = 0f
            pan = Offset.Zero
            undoStack.clear()
            redoStack.clear()
            isPlaying = false
            projectName = name
            revision++
            saveProject(context, frames, currentFrameIndex, fps, canvasMaterial, projectName)
            refreshProjectCatalog()
            notice = "Novo projeto criado"
            page = AppPage.EDITOR
        }
    }

    val saveAsProject: (String) -> Unit = { requestedName ->
        val name = requestedName.trim()
        if (name.isNotBlank()) {
            val saved = saveProject(context, frames, currentFrameIndex, fps, canvasMaterial, name)
            if (saved) {
                projectName = name
                revision++
                refreshProjectCatalog()
                notice = "Cópia guardada como $name"
                page = AppPage.EDITOR
            } else {
                notice = "Falha ao guardar a cópia"
            }
        }
    }

    val openNamedProject: (String) -> Unit = { name ->
        saveProject(context, frames, currentFrameIndex, fps, canvasMaterial, projectName)
        val loaded = loadProject(context, name)
        if (loaded != null && loaded.frames.isNotEmpty()) {
            frames.clear()
            frames.addAll(loaded.frames)
            currentFrameIndex = loaded.currentFrame.coerceIn(0, frames.lastIndex)
            fps = loaded.fps.coerceIn(1, 30)
            canvasMaterial = loaded.material
            projectName = loaded.projectName
            selectedTool = DrawingTool.BRUSH
            selectedBrush = BrushPreset.INK
            undoStack.clear()
            redoStack.clear()
            isPlaying = false
            revision++
            notice = "Projeto aberto: $name"
            page = AppPage.EDITOR
        } else {
            notice = "Não foi possível abrir este projeto"
        }
    }

    val startNewProject: () -> Unit = {
        saveProject(context, frames, currentFrameIndex, fps, canvasMaterial, projectName)
        val existing = listProjectInfos(context).map { it.name }
        val name = generateUniqueProjectName(existing, "Nova animação")
        createNamedProject(name)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = AppBackground) {
        when (page) {
            AppPage.HOME -> HomeScreen(
                onContinue = { page = AppPage.EDITOR },
                onNewProject = startNewProject,
                onProjects = {
                    saveAction()
                    refreshProjectCatalog()
                    page = AppPage.PROJECTS
                },
                onTemplates = { page = AppPage.TEMPLATES },
                onSettings = { page = AppPage.SETTINGS },
                onAbout = { page = AppPage.ABOUT }
            )
            AppPage.PROJECTS -> ProjectsScreen(
                projects = projectCatalog.toList(),
                currentProjectName = projectName,
                onBack = { page = AppPage.HOME },
                onOpenProject = openNamedProject,
                onTemplates = { page = AppPage.TEMPLATES },
                onNewProject = createNamedProject,
                onSaveAs = saveAsProject,
                onDeleteProject = { name ->
                    if (name.equals(projectName, ignoreCase = true)) {
                        notice = "Guarda ou abre outro projeto antes de eliminar este"
                    } else {
                        deleteProjectFile(context, name)
                        refreshProjectCatalog()
                        notice = "Projeto eliminado"
                    }
                }
            )
            AppPage.TEMPLATES -> TemplatesScreen(
                onBack = { page = AppPage.HOME },
                onOpenTemplate = { demo ->
                    frames.clear()
                    frames.addAll(createDemoFrames(demo))
                    currentFrameIndex = 0
                    fps = demo.fps
                    canvasMaterial = demo.material
                    selectedTool = DrawingTool.BRUSH
                    selectedBrush = when (demo) {
                        DemoProject.COSMIC_BOUNCE -> BrushPreset.AIRBRUSH
                        DemoProject.SUNSET -> BrushPreset.PASTEL
                        DemoProject.FLOWER_BLOOM -> BrushPreset.PENCIL
                    }
                    brushColor = 0xFF332A55.toInt()
                    brushSize = 8f
                    onionSkin = false
                    zoom = 1f
                    rotation = 0f
                    pan = Offset.Zero
                    undoStack.clear()
                    redoStack.clear()
                    isPlaying = true
                    projectName = demo.title
                    revision++
                    notice = "Modelo pronto • " + demo.frameCount + " frames"
                    page = AppPage.EDITOR
                }
            )
            AppPage.SETTINGS -> SettingsScreen(
                defaultFps = defaultFps,
                onionSkin = defaultOnionSkin,
                immersiveEditor = immersiveEditor,
                onBack = { page = AppPage.HOME },
                onDefaultFpsChange = {
                    defaultFps = it.coerceIn(1, 30)
                    settingsPrefs.edit().putInt("default-fps", defaultFps).apply()
                },
                onOnionSkinChange = {
                    defaultOnionSkin = it
                    settingsPrefs.edit().putBoolean("default-onion-skin", it).apply()
                },
                onImmersiveEditorChange = {
                    immersiveEditor = it
                    settingsPrefs.edit().putBoolean("immersive-editor", it).apply()
                }
            )
            AppPage.ABOUT -> AboutScreen(onBack = { page = AppPage.HOME })
            AppPage.EDITOR -> EditorScreen(
                projectName = projectName,
                notice = notice,
                frames = frames,
                currentFrameIndex = currentFrameIndex,
                fps = fps,
                selectedTool = selectedTool,
                brushColor = brushColor,
                brushSize = brushSize,
                selectedBrush = selectedBrush,
                canvasMaterial = canvasMaterial,
                onionSkin = onionSkin,
                zoom = zoom,
                rotation = rotation,
                pan = pan,
                revision = revision,
                isPlaying = isPlaying,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                onHome = {
                    isPlaying = false
                    saveProject(context, frames, currentFrameIndex, fps, canvasMaterial, projectName)
                    refreshProjectCatalog()
                    page = AppPage.HOME
                },
                onToolSelected = { selectedTool = it },
                onBrushSelected = { selectedBrush = it; selectedTool = DrawingTool.BRUSH },
                onMaterialSelected = { canvasMaterial = it; revision++ },
                onShowBrushes = { showBrushesDialog = true },
                onShowMaterials = { showMaterialsDialog = true },
                onColorSelected = { brushColor = it },
                onBrushSizeChange = { brushSize = it },
                onZoomChange = { zoom = it.coerceIn(0.35f, 5f) },
                onRotationChange = { rotation = it },
                onResetViewport = {
                    zoom = 1f
                    rotation = 0f
                    pan = Offset.Zero
                },
                onSave = saveAction,
                onShowExport = { showExportDialog = true },
                inputMode = inputMode,
                onInputModeChange = { inputMode = it },
                onCommit = commitStroke,
                onPanChange = { pan = it },
                onFrameSelected = {
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
                onFpsChange = { fps = it.coerceIn(1, 30) },
                onPlay = { isPlaying = !isPlaying },
                onOnionSkin = { onionSkin = !onionSkin },
                onUndo = undoAction,
                onRedo = redoAction,
                onEffects = { showEffects = true }
            )
        }
    }

    if (showBrushesDialog) {
        AlertDialog(
            onDismissRequest = { showBrushesDialog = false },
            containerColor = PanelBackground,
            title = { Text("Biblioteca de pincéis", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 350.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    BrushPreset.values().forEach { preset ->
                        BrushPresetOption(
                            preset = preset,
                            selected = selectedBrush == preset,
                            onClick = {
                                selectedBrush = preset
                                selectedTool = DrawingTool.BRUSH
                                showBrushesDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBrushesDialog = false }) {
                    Text("Fechar", color = Accent)
                }
            }
        )
    }

    if (showMaterialsDialog) {
        AlertDialog(
            onDismissRequest = { showMaterialsDialog = false },
            containerColor = PanelBackground,
            title = { Text("Materiais do palco", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 350.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    CanvasMaterial.values().forEach { material ->
                        MaterialPresetOption(
                            material = material,
                            selected = canvasMaterial == material,
                            onClick = {
                                canvasMaterial = material
                                revision++
                                showMaterialsDialog = false
                            }
                        )
                    }
                    Text(
                        "O material escolhido também é usado na exportação de imagens e animações.",
                        color = TextMuted,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showMaterialsDialog = false }) {
                    Text("Fechar", color = Accent)
                }
            }
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            containerColor = PanelBackground,
            title = { Text("Exportar", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ExportOption("PNG — frame atual", "Imagem de alta resolução") {
                        showExportDialog = false
                        pngExportLauncher.launch("drawmotion-frame-" + (currentFrameIndex + 1) + ".png")
                    }
                    ExportOption("JPEG — frame atual", "Imagem comprimida, sem transparência") {
                        showExportDialog = false
                        jpegExportLauncher.launch("drawmotion-frame-" + (currentFrameIndex + 1) + ".jpg")
                    }
                    ExportOption("GIF — animação completa", "Todos os frames, sem áudio e com até 256 cores por frame") {
                        showExportDialog = false
                        gifExportLauncher.launch("drawmotion-animation.gif")
                    }
                    ExportOption("Sequência PNG — ZIP", "Um PNG por frame e um ficheiro com o FPS") {
                        showExportDialog = false
                        pngSequenceExportLauncher.launch("drawmotion-frames.zip")
                    }
                    ExportOption("MP4 — animação completa", "Vídeo H.264 com o FPS do projeto") {
                        showExportDialog = false
                        mp4ExportLauncher.launch("drawmotion-${safeProjectSlug(projectName)}.mp4")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cancelar", color = Accent)
                }
            }
        )
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
                        redoStack.clear()
                        activeFrame.strokes.replaceAll { stroke ->
                            stroke.copy(points = stroke.points.map { Offset(BOARD_WIDTH - it.x, it.y) })
                        }
                        revision++
                        showEffects = false
                    }
                    EffectOption("Inverter verticalmente", "Inverte o desenho de cima para baixo") {
                        undoStack.add(activeFrame.strokes.toList())
                        redoStack.clear()
                        activeFrame.strokes.replaceAll { stroke ->
                            stroke.copy(points = stroke.points.map { Offset(it.x, BOARD_HEIGHT - it.y) })
                        }
                        revision++
                        showEffects = false
                    }
                    EffectOption("Monocromático", "Converte os traços para tons de cinzento") {
                        undoStack.add(activeFrame.strokes.toList())
                        redoStack.clear()
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
                        redoStack.clear()
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

private enum class AppPage { HOME, PROJECTS, TEMPLATES, SETTINGS, ABOUT, EDITOR }

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is android.content.ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}

@Composable
private fun HomeScreen(
    onContinue: () -> Unit,
    onNewProject: () -> Unit,
    onProjects: () -> Unit,
    onTemplates: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(Accent),
                contentAlignment = Alignment.Center
            ) {
                Text("D", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
            }
            Column {
                Text("Drawmotion", color = TextPrimary, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text("O teu estúdio de animação 2D", color = TextMuted, fontSize = 12.sp)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = PanelBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier.padding(19.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("DESENHA • ANIMA • REPRODUZ", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("Dá vida aos teus desenhos.", color = TextPrimary, fontSize = 26.sp, lineHeight = 31.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Entra no estúdio horizontal, usa uma área de desenho maior e organiza os frames numa timeline.",
                    color = TextMuted,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
                Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                    Text("Continuar animação")
                }
                OutlinedButton(onClick = onNewProject, modifier = Modifier.fillMaxWidth()) {
                    Text("＋  Novo projeto")
                }
            }
        }

        Text("O TEU ESPAÇO", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HomeNavigationTile(
                modifier = Modifier.weight(1f),
                symbol = "▤",
                title = "Projetos",
                description = "Abrir o rascunho local",
                onClick = onProjects
            )
            HomeNavigationTile(
                modifier = Modifier.weight(1f),
                symbol = "⚙",
                title = "Definições",
                description = "Ajustar o estúdio",
                onClick = onSettings
            )
        }
        HomeNavigationTile(
            modifier = Modifier.fillMaxWidth(),
            symbol = "▶",
            title = "Animações prontas",
            description = "Explorar modelos em movimento e usá-los como ponto de partida",
            onClick = onTemplates
        )
        HomeNavigationTile(
            modifier = Modifier.fillMaxWidth(),
            symbol = "ⓘ",
            title = "Sobre o Drawmotion",
            description = "Funções disponíveis e próximas melhorias",
            onClick = onAbout
        )
        Text(
            "Os desenhos são guardados localmente neste dispositivo.",
            color = TextMuted,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun HomeNavigationTile(
    modifier: Modifier = Modifier,
    symbol: String,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PanelBackground)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(symbol, color = Accent, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(description, color = TextMuted, fontSize = 10.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun PageHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)) {
            Text("← Voltar")
        }
        Text(title, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ProjectsScreen(
    projects: List<ProjectInfo>,
    currentProjectName: String,
    onBack: () -> Unit,
    onOpenProject: (String) -> Unit,
    onTemplates: () -> Unit,
    onNewProject: (String) -> Unit,
    onSaveAs: (String) -> Unit,
    onDeleteProject: (String) -> Unit
) {
    var showNameDialog by remember { mutableStateOf(false) }
    var nameDialogMode by remember { mutableStateOf("new") }
    var requestedName by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PageHeader("Projetos", onBack)
        Text(
            "BIBLIOTECA LOCAL · ${projects.size} PROJETO(S)",
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )

        if (projects.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PanelBackground)
            ) {
                Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ainda não há projetos guardados", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Cria um projeto ou entra no estúdio para começar a desenhar. Cada projeto fica guardado separadamente neste dispositivo.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        } else {
            projects.forEach { project ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PanelBackground)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    project.name,
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    if (project.name.equals(currentProjectName, ignoreCase = true)) "Projeto atual" else "Guardado neste dispositivo",
                                    color = if (project.name.equals(currentProjectName, ignoreCase = true)) Accent else TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                            Text(
                                project.material.title,
                                color = TextMuted,
                                fontSize = 9.sp,
                                maxLines = 1
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ProjectDetail("FRAMES", project.frameCount.toString(), Modifier.weight(1f))
                            ProjectDetail("TRAÇOS", project.strokeCount.toString(), Modifier.weight(1f))
                            ProjectDetail("FPS", project.fps.toString(), Modifier.weight(1f))
                        }
                        Text(
                            "Última alteração: ${formatProjectTimestamp(project.updatedAt)}",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { onOpenProject(project.name) },
                                modifier = Modifier.weight(1f)
                            ) { Text("Abrir projeto", fontSize = 11.sp) }
                            if (!project.name.equals(currentProjectName, ignoreCase = true)) {
                                OutlinedButton(
                                    onClick = { onDeleteProject(project.name) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                                ) { Text("Eliminar", fontSize = 10.sp) }
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = {
                requestedName = generateUniqueProjectName(projects.map { it.name }, "Nova animação")
                nameDialogMode = "new"
                nameError = ""
                showNameDialog = true
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("＋  Criar projeto") }

        OutlinedButton(
            onClick = {
                requestedName = generateUniqueProjectName(projects.map { it.name }, "$currentProjectName - cópia")
                nameDialogMode = "copy"
                nameError = ""
                showNameDialog = true
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Guardar projeto atual como…") }

        OutlinedButton(onClick = onTemplates, modifier = Modifier.fillMaxWidth()) {
            Text("▶  Explorar animações prontas")
        }
        Text(
            "Os detalhes do projeto incluem a contagem de frames e traços, a velocidade e a última alteração.",
            color = TextMuted,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            containerColor = PanelBackground,
            title = {
                Text(
                    if (nameDialogMode == "copy") "Guardar cópia do projeto" else "Criar projeto",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (nameDialogMode == "copy")
                            "O desenho, a timeline, os pincéis e o material do palco serão mantidos na cópia."
                        else
                            "O projeto novo começa com um palco vazio. O trabalho atual fica guardado.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                    OutlinedTextField(
                        value = requestedName,
                        onValueChange = {
                            requestedName = it.take(60)
                            nameError = ""
                        },
                        singleLine = true,
                        label = { Text("Nome do projeto") },
                        isError = nameError.isNotBlank()
                    )
                    if (nameError.isNotBlank()) {
                        Text(nameError, color = Color(0xFFFF8492), fontSize = 10.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cleanName = requestedName.trim()
                        when {
                            cleanName.isBlank() -> nameError = "Escreve um nome para o projeto."
                            projects.any { it.name.equals(cleanName, ignoreCase = true) } ->
                                nameError = "Já existe um projeto com esse nome."
                            nameDialogMode == "copy" -> {
                                onSaveAs(cleanName)
                                showNameDialog = false
                            }
                            else -> {
                                onNewProject(cleanName)
                                showNameDialog = false
                            }
                        }
                    }
                ) { Text("Confirmar", color = Accent) }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text("Cancelar", color = TextMuted)
                }
            }
        )
    }
}

@Composable
private fun ProjectDetail(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF242834))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(label, color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TemplatesScreen(
    onBack: () -> Unit,
    onOpenTemplate: (DemoProject) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PageHeader("Animações prontas", onBack)
        Text("MODELOS EM MOVIMENTO", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(
            "Cada cartão é uma animação real composta por frames. Vê a pré-visualização a mexer e abre o modelo no editor para explorar, modificar e exportar.",
            color = TextMuted,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
        DemoProject.values().forEach { demo ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = PanelBackground)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    DemoPreview(demo, Modifier.fillMaxWidth().height(132.dp))
                    Text(demo.title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(demo.description, color = TextMuted, fontSize = 11.sp, lineHeight = 15.sp)
                    Text(
                        demo.frameCount.toString() + " frames  •  " + demo.fps + " FPS  •  " + demo.material.title,
                        color = Accent,
                        fontSize = 9.sp
                    )
                    Button(
                        onClick = { onOpenTemplate(demo) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Usar modelo e reproduzir  ▶")
                    }
                }
            }
        }
        Text(
            "Os modelos são criados localmente e funcionam sem descarregar vídeos da internet.",
            color = TextMuted,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun DemoPreview(demo: DemoProject, modifier: Modifier = Modifier) {
    var previewFrame by remember(demo) { mutableIntStateOf(0) }
    LaunchedEffect(demo) {
        while (true) {
            delay((1000L / demo.fps.coerceAtLeast(1)).coerceAtLeast(40L))
            previewFrame = (previewFrame + 1) % demo.frameCount
        }
    }
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(demo.material.backgroundColor))
    ) {
        drawRect(Color(demo.material.backgroundColor), size = size)
        withTransform({
            scale(size.width / BOARD_WIDTH, size.height / BOARD_HEIGHT, pivot = Offset.Zero)
        }) {
            demo.material.gridColor?.let { grid ->
                for (x in 0..960 step 48) {
                    drawLine(Color(grid), Offset(x.toFloat(), 0f), Offset(x.toFloat(), BOARD_HEIGHT), 1f)
                }
                for (y in 0..540 step 48) {
                    drawLine(Color(grid), Offset(0f, y.toFloat()), Offset(BOARD_WIDTH, y.toFloat()), 1f)
                }
            }
            val phase = previewFrame.toFloat() / demo.frameCount * 6.283185f
            when (demo) {
                DemoProject.COSMIC_BOUNCE -> {
                    for (n in 0..34) {
                        val x = ((n * 149 + 37) % 930 + 15).toFloat()
                        val y = ((n * 83 + 19) % 500 + 15).toFloat()
                        drawCircle(
                            color = if (n % 3 == 0) Color(0xFFFFD166) else Color(0xFFBBAFFF),
                            radius = if (n % 5 == 0) 3.5f else 2f,
                            center = Offset(x, y)
                        )
                    }
                    drawLine(Color(0xFF525978), Offset(20f, 445f), Offset(940f, 445f), 3f)
                    val x = 95f + previewFrame * 69f
                    val y = 328f - abs(sin((phase * 1.5f).toDouble()).toFloat()) * 128f
                    for (tail in 4 downTo 1) {
                        drawCircle(
                            color = Color(0xFF8D7CFF).copy(alpha = 0.08f * (5 - tail)),
                            radius = 9f + tail * 5f,
                            center = Offset(x - tail * 22f, y + tail * 4f)
                        )
                    }
                    drawOval(
                        color = Color(0xFF8D7CFF).copy(alpha = 0.55f),
                        topLeft = Offset(x - 54f, y - 17f),
                        size = Size(108f, 34f),
                        style = Stroke(width = 3f)
                    )
                    drawCircle(Color(0xFF21A0F6), 36f, Offset(x, y))
                    drawCircle(Color(0xFF8D7CFF), 26f, Offset(x + 3f, y + 2f))
                    drawCircle(Color(0xFFFF7DA7), 15f, Offset(x + 7f, y + 3f))
                    drawCircle(Color.White, 7f, Offset(x - 10f, y - 11f))
                }
                DemoProject.SUNSET -> {
                    val sunX = 480f + sin(phase.toDouble()).toFloat() * 36f
                    val sunY = 222f + sin((phase * 1.3f).toDouble()).toFloat() * 18f
                    drawCircle(Color(0xFFFF9B54).copy(alpha = 0.17f), 82f, Offset(sunX, sunY))
                    drawCircle(Color(0xFFFF9B54), 61f, Offset(sunX, sunY))
                    drawCircle(Color(0xFFFFD166), 48f, Offset(sunX, sunY + 2f))
                    drawCircle(Color(0xFFFFF0B8), 32f, Offset(sunX, sunY + 4f))
                    val cloudX = 165f + (previewFrame * 27f) % 820f
                    listOf(
                        Offset(cloudX - 25f, 115f),
                        Offset(cloudX, 99f),
                        Offset(cloudX + 27f, 113f),
                        Offset(cloudX + 45f, 117f)
                    ).forEach { drawCircle(Color.White.copy(alpha = 0.8f), 19f, it) }
                    drawLine(Color(0xFF7B6BB2), Offset(-30f, 405f), Offset(175f, 354f), 76f, StrokeCap.Round)
                    drawLine(Color(0xFF7B6BB2), Offset(175f, 354f), Offset(342f, 407f), 76f, StrokeCap.Round)
                    drawLine(Color(0xFF528A9A), Offset(300f, 431f), Offset(515f, 360f), 80f, StrokeCap.Round)
                    drawLine(Color(0xFF528A9A), Offset(515f, 360f), Offset(725f, 415f), 80f, StrokeCap.Round)
                    drawLine(Color(0xFF397B71), Offset(600f, 470f), Offset(790f, 425f), 82f, StrokeCap.Round)
                    drawLine(Color(0xFF397B71), Offset(790f, 425f), Offset(990f, 478f), 82f, StrokeCap.Round)
                }
                DemoProject.FLOWER_BLOOM -> {
                    val cx = 480f
                    val cy = 240f
                    drawLine(Color(0xFF4B9B67), Offset(cx, cy + 12f), Offset(cx + 8f, 493f), 12f, StrokeCap.Round)
                    drawLine(Color(0xFF4B9B67), Offset(cx + 4f, 405f), Offset(cx - 80f, 362f), 18f, StrokeCap.Round)
                    drawLine(Color(0xFF4B9B67), Offset(cx + 4f, 405f), Offset(cx + 94f, 366f), 18f, StrokeCap.Round)
                    val petalRadius = 28f + previewFrame * 4.2f
                    val petalColors = listOf(
                        Color(0xFFFF86B7), Color(0xFFC5A2FF), Color(0xFFFFB1CF),
                        Color(0xFFFF86B7), Color(0xFFBBAFFF), Color(0xFFFFB1CF),
                        Color(0xFFFF86B7), Color(0xFFC5A2FF)
                    )
                    for (petal in 0..7) {
                        val angle = petal * (Math.PI / 4.0) + phase * 0.035
                        val end = Offset(
                            cx + cos(angle).toFloat() * petalRadius,
                            cy + sin(angle).toFloat() * petalRadius
                        )
                        drawLine(
                            petalColors[petal],
                            Offset(cx, cy),
                            end,
                            strokeWidth = 24f,
                            cap = StrokeCap.Round
                        )
                    }
                    drawCircle(Color(0xFFFFD166), 24f, Offset(cx, cy))
                    drawCircle(Color(0xFFFFF0B8), 12f, Offset(cx - 4f, cy - 5f))
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    defaultFps: Int,
    onionSkin: Boolean,
    immersiveEditor: Boolean,
    onBack: () -> Unit,
    onDefaultFpsChange: (Int) -> Unit,
    onOnionSkinChange: (Boolean) -> Unit,
    onImmersiveEditorChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PageHeader("Definições", onBack)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PanelBackground)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Velocidade inicial da animação", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text("$defaultFps FPS", color = Accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = defaultFps.toFloat(),
                    onValueChange = { onDefaultFpsChange(it.roundToInt()) },
                    valueRange = 1f..30f,
                    steps = 28
                )
                Text("Aplica-se aos novos projetos. Podes mudar os FPS no editor.", color = TextMuted, fontSize = 11.sp)
            }
        }
        SettingsOption(
            title = "Onion skin por defeito",
            description = "Mostra os frames vizinhos ao desenhar.",
            enabled = onionSkin,
            onToggle = { onOnionSkinChange(!onionSkin) }
        )
        SettingsOption(
            title = "Editor imersivo",
            description = "Esconde a barra do sistema durante o desenho; desliza a partir da borda para a mostrar.",
            enabled = immersiveEditor,
            onToggle = { onImmersiveEditorChange(!immersiveEditor) }
        )
        Text(
            "O editor abre sempre na horizontal para dar mais espaço à tela de desenho.",
            color = TextMuted,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}

@Composable
private fun SettingsOption(
    title: String,
    description: String,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PanelBackground)
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(description, color = TextMuted, fontSize = 11.sp, lineHeight = 15.sp)
            }
            Text(
                if (enabled) "Ligado" else "Desligado",
                color = if (enabled) Accent else TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AboutScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        PageHeader("Sobre o Drawmotion", onBack)
        Text("UM ESTÚDIO 2D PARA ANDROID", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(
            "O Drawmotion é um editor nativo para desenhar frame a frame, com ferramentas simples e uma área de trabalho horizontal.",
            color = TextPrimary,
            fontSize = 16.sp,
            lineHeight = 23.sp
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PanelBackground)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Já disponível", color = TextPrimary, fontWeight = FontWeight.Bold)
                Text("• Pincel, borracha, linha, retângulo e elipse", color = TextMuted, fontSize = 12.sp)
                Text("• Frames, duplicação e remoção", color = TextMuted, fontSize = 12.sp)
                Text("• Onion skin e pré-visualização", color = TextMuted, fontSize = 12.sp)
                Text("• Desfazer/refazer e autosave local", color = TextMuted, fontSize = 12.sp)
                Text("• Exportação do frame atual para PNG", color = TextMuted, fontSize = 12.sp)
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PanelBackground)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Próximas melhorias", color = TextPrimary, fontWeight = FontWeight.Bold)
                Text("Biblioteca com vários projetos, camadas independentes, importação de imagens, áudio e exportação de animações para GIF/MP4.", color = TextMuted, fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun BrushPresetOption(
    preset: BrushPreset,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color(0xFF332E56) else Color(0xFF252936))
            .border(1.dp, if (selected) Accent else PanelBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(
            modifier = Modifier.size(50.dp, 38.dp).clip(RoundedCornerShape(6.dp)).background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val p = Path().apply {
                    moveTo(5f, size.height * 0.73f)
                    quadraticTo(size.width * 0.42f, size.height * 0.1f, size.width - 5f, size.height * 0.4f)
                }
                drawPath(
                    p,
                    color = Color(0xFF252238).copy(alpha = preset.opacity),
                    style = Stroke(
                        width = 2.8f * preset.widthScale,
                        cap = if (preset.squareCaps) StrokeCap.Square else StrokeCap.Round,
                        pathEffect = if (preset.dashed) PathEffect.dashPathEffect(floatArrayOf(5f, 3f)) else null
                    )
                )
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(preset.symbol + "  " + preset.label, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(preset.description, color = TextMuted, fontSize = 9.sp, lineHeight = 12.sp)
        }
        if (selected) Text("✓", color = Accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MaterialPresetOption(
    material: CanvasMaterial,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color(0xFF332E56) else Color(0xFF252936))
            .border(1.dp, if (selected) Accent else PanelBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Canvas(Modifier.size(54.dp, 38.dp).clip(RoundedCornerShape(6.dp))) {
            drawRect(Color(material.backgroundColor), size = size)
            material.gridColor?.let { grid ->
                for (x in 0..size.width.toInt() step 8) {
                    drawLine(Color(grid), Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), 1f)
                }
                for (y in 0..size.height.toInt() step 8) {
                    drawLine(Color(grid), Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), 1f)
                }
            }
            drawCircle(Color(0xFF8D7CFF), radius = 5f, center = Offset(size.width * 0.68f, size.height * 0.42f))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(material.title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(material.description, color = TextMuted, fontSize = 9.sp, lineHeight = 12.sp)
        }
        if (selected) Text("✓", color = Accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ExportOption(title: String, description: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(Color(0xFF252936))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text(description, color = TextMuted, fontSize = 9.sp, lineHeight = 12.sp)
    }
}

@Composable
private fun EditorScreen(
    projectName: String,
    notice: String,
    frames: List<AnimationFrame>,
    currentFrameIndex: Int,
    fps: Int,
    selectedTool: DrawingTool,
    brushColor: Int,
    brushSize: Float,
    selectedBrush: BrushPreset,
    canvasMaterial: CanvasMaterial,
    onionSkin: Boolean,
    zoom: Float,
    rotation: Float,
    pan: Offset,
    revision: Int,
    isPlaying: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onHome: () -> Unit,
    onToolSelected: (DrawingTool) -> Unit,
    onBrushSelected: (BrushPreset) -> Unit,
    onMaterialSelected: (CanvasMaterial) -> Unit,
    onShowBrushes: () -> Unit,
    onShowMaterials: () -> Unit,
    onColorSelected: (Int) -> Unit,
    onBrushSizeChange: (Float) -> Unit,
    onZoomChange: (Float) -> Unit,
    onRotationChange: (Float) -> Unit,
    onResetViewport: () -> Unit,
    onSave: () -> Unit,
    onShowExport: () -> Unit,
    onCommit: (InkStroke) -> Unit,
    onPanChange: (Offset) -> Unit,
    onFrameSelected: (Int) -> Unit,
    onAddFrame: () -> Unit,
    onDuplicateFrame: () -> Unit,
    onDeleteFrame: () -> Unit,
    onFpsChange: (Int) -> Unit,
    onPlay: () -> Unit,
    onOnionSkin: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onEffects: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 7.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Column(
            modifier = Modifier.width(59.dp).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            RailButton("⌂", "Início", onClick = onHome)
            RailButton(
                selectedBrush.symbol,
                "Pincéis",
                selected = selectedTool == DrawingTool.BRUSH,
                onClick = onShowBrushes
            )
            DrawingTool.values().filter { it != DrawingTool.BRUSH }.forEach { tool ->
                RailButton(
                    symbol = tool.symbol,
                    label = when (tool) {
                        DrawingTool.BRUSH -> "Pincéis"
                        DrawingTool.ERASER -> "Apagar"
                        DrawingTool.LINE -> "Linha"
                        DrawingTool.RECTANGLE -> "Rect."
                        DrawingTool.ELLIPSE -> "Elipse"
                    },
                    selected = selectedTool == tool,
                    onClick = { onToolSelected(tool) }
                )
            }
            Spacer(Modifier.weight(1f))
            RailButton("↶", "Desfazer", enabled = canUndo, onClick = onUndo)
            RailButton("↷", "Refazer", enabled = canRedo, onClick = onRedo)
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(32.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Column(modifier = Modifier.width(92.dp)) {
                    Text(projectName, color = TextPrimary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                    Text(
                        if (notice.isNotBlank()) notice else "Frame " + (currentFrameIndex + 1) + "/" + frames.size,
                        color = if (notice.isNotBlank()) Accent else TextMuted,
                        fontSize = 8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.weight(1f))
                TinyActionButton("−", onClick = { onZoomChange(zoom - 0.15f) })
                Text((zoom * 100f).roundToInt().toString() + "%", color = TextPrimary, fontSize = 9.sp)
                TinyActionButton("+", onClick = { onZoomChange(zoom + 0.15f) })
                TinyActionButton("↶", onClick = { onRotationChange(rotation - 15f) })
                TinyActionButton("↷", onClick = { onRotationChange(rotation + 15f) })
                TinyActionButton("⟲", onClick = onResetViewport)
                Slider(
                    value = brushSize,
                    onValueChange = onBrushSizeChange,
                    valueRange = 2f..28f,
                    modifier = Modifier.width(82.dp).height(26.dp)
                )
                TinyActionButton("Guardar", onClick = onSave)
                TinyActionButton("Exportar", onClick = onShowExport)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF090B10)),
                contentAlignment = Alignment.Center
            ) {
                StageCanvas(
                    modifier = Modifier,
                    frames = frames,
                    currentFrameIndex = currentFrameIndex,
                    tool = selectedTool,
                    brushColor = brushColor,
                    brushSize = brushSize,
                    selectedBrush = selectedBrush,
                    canvasMaterial = canvasMaterial,
                    onionSkin = onionSkin,
                    zoom = zoom,
                    rotation = rotation,
                    pan = pan,
                    revision = revision,
                    onCommit = onCommit,
                    onZoomChange = onZoomChange,
                    onRotationChange = onRotationChange,
                    onPanChange = onPanChange
                )
            }

            TimelinePanel(
                frames = frames,
                currentFrameIndex = currentFrameIndex,
                fps = fps,
                onSelectFrame = onFrameSelected,
                onAddFrame = onAddFrame,
                onDuplicateFrame = onDuplicateFrame,
                onDeleteFrame = onDeleteFrame,
                onFpsChange = onFpsChange
            )
        }

        Column(
            modifier = Modifier.width(58.dp).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text("CORES", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            EditorColorGrid(selectedColor = brushColor, onSelect = onColorSelected)
            RailButton(
                "▤",
                "Materiais",
                selected = canvasMaterial != CanvasMaterial.GRID,
                onClick = onShowMaterials
            )
            Spacer(Modifier.weight(1f))
            RailButton(if (isPlaying) "Ⅱ" else "▶", if (isPlaying) "Pausa" else "Reproduzir", onClick = onPlay)
            RailButton(if (onionSkin) "◉" else "○", "Onion", selected = onionSkin, onClick = onOnionSkin)
            RailButton("✦", "Efeitos", onClick = onEffects)
        }
    }
}

@Composable
private fun RailButton(
    symbol: String,
    label: String,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(
                when {
                    !enabled -> Color(0xFF171A22)
                    selected -> Color(0xFF332E56)
                    else -> Color(0xFF1C202B)
                }
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(symbol, color = if (selected) Color(0xFFC7BDFF) else if (enabled) TextPrimary else TextMuted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text(label, color = if (enabled) TextMuted else Color(0xFF636A7A), fontSize = 7.sp, maxLines = 1, overflow = TextOverflow.Clip)
    }
}

@Composable
private fun TinyActionButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Box(
        modifier = Modifier
            .height(27.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(if (enabled) Color(0xFF262A37) else Color(0xFF181B23))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (enabled) TextPrimary else TextMuted, fontSize = 9.sp, maxLines = 1)
    }
}

@Composable
private fun EditorColorGrid(selectedColor: Int, onSelect: (Int) -> Unit) {
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
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        colors.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                pair.forEach { colorInt ->
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color(colorInt))
                            .border(
                                width = if (selectedColor == colorInt) 2.dp else 1.dp,
                                color = if (selectedColor == colorInt) Accent else Color(0xFF4A4F60),
                                shape = CircleShape
                            )
                            .clickable { onSelect(colorInt) }
                    )
                }
            }
        }
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
    selectedBrush: BrushPreset,
    canvasMaterial: CanvasMaterial,
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
    val latestBrush = rememberUpdatedState(selectedBrush)
    val latestTool = rememberUpdatedState(tool)
    val latestOnCommit = rememberUpdatedState(onCommit)
    val latestZoomChange = rememberUpdatedState(onZoomChange)
    val latestRotationChange = rememberUpdatedState(onRotationChange)
    val latestPanChange = rememberUpdatedState(onPanChange)

    Canvas(
        modifier = modifier
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(canvasMaterial.backgroundColor))
            .pointerInput(tool, currentFrameIndex) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startTool = latestTool.value
                    val points = mutableListOf<Offset>()
                    points.add(pointerToBoard(down.position, size, latestZoom.value, latestRotation.value, latestPan.value))
                    var drawing = true
                    previewState.value = InkStroke(points.toList(), latestColor.value, latestWidth.value, startTool, latestBrush.value)

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.isEmpty()) {
                            event.changes.forEach { it.consume() }
                            break
                        }

                        if (pressed.size >= 2) {
                            // A gesture that becomes a two-finger canvas move/rotate/zoom
                            // must not leave an accidental dot or short mark behind.
                            if (drawing) {
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
                                    startTool,
                                    latestBrush.value
                                )
                            }
                        }

                        event.changes.forEach { it.consume() }
                    }

                    if (drawing && points.isNotEmpty()) {
                        latestOnCommit.value(
                            InkStroke(points.toList(), latestColor.value, latestWidth.value, startTool, latestBrush.value)
                        )
                    }
                    previewState.value = null
                }
            }
    ) {
        val ignoredRevision = revision
        drawRect(Color(canvasMaterial.backgroundColor), size = size)

        withTransform({
            scale(size.width / BOARD_WIDTH, size.height / BOARD_HEIGHT, pivot = Offset.Zero)
        }) {
            canvasMaterial.gridColor?.let { grid ->
                for (x in 0..960 step 48) {
                    drawLine(
                        color = Color(grid),
                        start = Offset(x.toFloat(), 0f),
                        end = Offset(x.toFloat(), BOARD_HEIGHT),
                        strokeWidth = 1f
                    )
                }
                for (y in 0..540 step 48) {
                    drawLine(
                        color = Color(grid),
                        start = Offset(0f, y.toFloat()),
                        end = Offset(BOARD_WIDTH, y.toFloat()),
                        strokeWidth = 1f
                    )
                }
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
        DrawingTool.RECTANGLE, DrawingTool.ELLIPSE -> {
            if (stroke.points.size == 1) {
                drawCircle(finalColor, radius = width / 2f, center = first)
            } else {
                val left = min(first.x, last.x)
                val top = min(first.y, last.y)
                val right = max(first.x, last.x)
                val bottom = max(first.y, last.y)
                val shapeSize = Size(right - left, bottom - top)
                if (stroke.tool == DrawingTool.RECTANGLE) {
                    drawRect(
                        color = finalColor,
                        topLeft = Offset(left, top),
                        size = shapeSize,
                        style = Stroke(width = width)
                    )
                } else {
                    drawOval(
                        color = finalColor,
                        topLeft = Offset(left, top),
                        size = shapeSize,
                        style = Stroke(width = width)
                    )
                }
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
            .clip(RoundedCornerShape(11.dp))
            .background(PanelBackground)
            .padding(horizontal = 7.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("TIMELINE", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(7.dp))
            Text(frames.size.toString() + " frames", color = TextMuted, fontSize = 8.sp)
            Spacer(Modifier.weight(1f))
            TinyActionButton("−", onClick = { onFpsChange(fps - 1) })
            Text(fps.toString() + " FPS", color = TextPrimary, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
            TinyActionButton("+", onClick = { onFpsChange(fps + 1) })
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
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
                    .size(width = 54.dp, height = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, PanelBorder, RoundedCornerShape(8.dp))
                    .background(Color(0xFF20232E))
                    .clickable(onClick = onAddFrame),
                contentAlignment = Alignment.Center
            ) {
                Text("+", color = Accent, fontSize = 22.sp)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(25.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TinyActionButton("+ Novo", onClick = onAddFrame)
            TinyActionButton("Duplicar", onClick = onDuplicateFrame)
            TinyActionButton("Apagar", onClick = onDeleteFrame)
            Spacer(Modifier.weight(1f))
            Text((currentFrameIndex + 1).toString() + "/" + frames.size, color = TextMuted, fontSize = 8.sp)
        }
    }
}

@Composable
private fun FrameTile(number: Int, selected: Boolean, strokeCount: Int, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .size(width = 54.dp, height = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color(0xFF332E56) else Color(0xFF222631))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Accent else PanelBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 5.dp, vertical = 3.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(number.toString().padStart(2, '0'), color = if (selected) Color(0xFFC7BDFF) else TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(5.dp).clip(CircleShape).background(if (strokeCount > 0) Color(0xFF67DDB5) else TextMuted))
            Text(" " + strokeCount, color = TextMuted, fontSize = 7.sp, maxLines = 1)
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

private fun projectDirectory(context: Context): File =
    File(context.filesDir, "projects").apply { mkdirs() }

private fun safeProjectSlug(name: String): String {
    val normalized = java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.ROOT)
    return normalized.replace(Regex("[^a-z0-9_-]+"), "-")
        .trim('-')
        .take(48)
        .ifBlank { "projeto" }
}

private fun projectFile(context: Context, name: String): File {
    val normalized = name.trim().lowercase(Locale.ROOT)
    val hash = Integer.toHexString(normalized.hashCode())
    return File(projectDirectory(context), "${safeProjectSlug(name)}-$hash.drawmotion")
}

private fun generateUniqueProjectName(existingNames: List<String>, preferredName: String): String {
    val existing = existingNames.map { it.trim().lowercase(Locale.ROOT) }.toSet()
    if (preferredName.trim().lowercase(Locale.ROOT) !in existing) return preferredName
    var suffix = 2
    while ((preferredName + " " + suffix).lowercase(Locale.ROOT) in existing) suffix++
    return preferredName + " " + suffix
}

private fun saveProject(
    context: Context,
    frames: List<AnimationFrame>,
    currentFrameIndex: Int,
    fps: Int,
    material: CanvasMaterial = CanvasMaterial.GRID,
    projectName: String = "Animação recente"
): Boolean {
    return try {
        val cleanName = projectName.trim().ifBlank { "Animação recente" }
        val root = JSONObject()
            .put("formatVersion", 3)
            .put("name", cleanName)
            .put("updatedAt", System.currentTimeMillis())
            .put("currentFrame", currentFrameIndex.coerceIn(0, (frames.size - 1).coerceAtLeast(0)))
            .put("fps", fps.coerceIn(1, 30))
            .put("material", material.name)
        val frameArray = JSONArray()
        frames.forEach { frame ->
            val frameObject = JSONObject().put("number", frame.number)
            val strokesArray = JSONArray()
            frame.strokes.forEach { stroke ->
                val strokeObject = JSONObject()
                    .put("color", stroke.color)
                    .put("width", stroke.width.toDouble())
                    .put("tool", stroke.tool.name)
                    .put("brush", stroke.brush.name)
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
        projectFile(context, cleanName).writeText(root.toString())
        true
    } catch (_: Exception) {
        false
    }
}

private fun decodeProject(data: String, fallbackName: String = "Animação recente"): SavedProject? {
    return try {
        val root = JSONObject(data)
        val material = runCatching {
            CanvasMaterial.valueOf(root.optString("material", CanvasMaterial.GRID.name))
        }.getOrDefault(CanvasMaterial.GRID)
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
                val brush = runCatching {
                    BrushPreset.valueOf(strokeObject.optString("brush", BrushPreset.INK.name))
                }.getOrDefault(BrushPreset.INK)
                val pointsArray = strokeObject.optJSONArray("points") ?: JSONArray()
                val points = mutableListOf<Offset>()
                for (k in 0 until pointsArray.length()) {
                    val point = pointsArray.getJSONObject(k)
                    points.add(
                        Offset(
                            point.optDouble("x", 0.0).toFloat(),
                            point.optDouble("y", 0.0).toFloat()
                        )
                    )
                }
                frame.strokes.add(
                    InkStroke(
                        points = points,
                        color = strokeObject.optInt("color", AndroidColor.BLACK),
                        width = strokeObject.optDouble("width", 8.0).toFloat().coerceAtLeast(1f),
                        tool = tool,
                        brush = brush
                    )
                )
            }
            loadedFrames.add(frame)
        }
        if (loadedFrames.isEmpty()) return null
        SavedProject(
            frames = loadedFrames,
            currentFrame = root.optInt("currentFrame", 0),
            fps = root.optInt("fps", 12),
            material = material,
            projectName = root.optString("name", fallbackName).ifBlank { fallbackName }
        )
    } catch (_: Exception) {
        null
    }
}

private fun loadProject(context: Context, name: String? = null): SavedProject? {
    return try {
        val directory = projectDirectory(context)
        val file = if (name != null) {
            projectFile(context, name).takeIf { it.isFile }
        } else {
            directory.listFiles()
                ?.filter { it.isFile && it.extension == "drawmotion" }
                ?.maxByOrNull { it.lastModified() }
        }
        if (file != null) return decodeProject(file.readText(), name ?: file.nameWithoutExtension)
        if (name != null) return null

        // Migra automaticamente o autosave único usado pelas versões anteriores.
        val legacyData = context.getSharedPreferences("drawmotion-project", Context.MODE_PRIVATE)
            .getString("project-json", null) ?: return null
        val migrated = decodeProject(legacyData, "Animação recente") ?: return null
        if (saveProject(
                context,
                migrated.frames,
                migrated.currentFrame,
                migrated.fps,
                migrated.material,
                migrated.projectName
            )
        ) {
            context.getSharedPreferences("drawmotion-project", Context.MODE_PRIVATE)
                .edit().remove("project-json").apply()
        }
        migrated
    } catch (_: Exception) {
        null
    }
}

private fun listProjectInfos(context: Context): List<ProjectInfo> {
    val files = projectDirectory(context).listFiles()
        ?.filter { it.isFile && it.extension == "drawmotion" }
        .orEmpty()
    return files.mapNotNull { file ->
        runCatching {
            val root = JSONObject(file.readText())
            val frames = root.optJSONArray("frames") ?: JSONArray()
            var strokeCount = 0
            for (i in 0 until frames.length()) {
                strokeCount += frames.optJSONObject(i)?.optJSONArray("strokes")?.length() ?: 0
            }
            val material = runCatching {
                CanvasMaterial.valueOf(root.optString("material", CanvasMaterial.GRID.name))
            }.getOrDefault(CanvasMaterial.GRID)
            ProjectInfo(
                name = root.optString("name", file.nameWithoutExtension),
                frameCount = frames.length(),
                strokeCount = strokeCount,
                fps = root.optInt("fps", 12),
                material = material,
                updatedAt = root.optLong("updatedAt", file.lastModified())
            )
        }.getOrNull()
    }.sortedByDescending { it.updatedAt }
}

private fun deleteProjectFile(context: Context, name: String): Boolean =
    projectFile(context, name).delete()

private fun formatProjectTimestamp(timestamp: Long): String =
    java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(java.util.Date(timestamp))

private fun renderFrameBitmap(
    frame: AnimationFrame,
    outputWidth: Int,
    outputHeight: Int,
    material: CanvasMaterial = CanvasMaterial.GRID
): Bitmap {
    require(outputWidth > 0 && outputHeight > 0) { "Dimensões da imagem inválidas" }
    val bitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    canvas.drawColor(material.backgroundColor)
    canvas.scale(outputWidth / BOARD_WIDTH, outputHeight / BOARD_HEIGHT)
    material.gridColor?.let { gridColor ->
        val gridPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            color = gridColor
            strokeWidth = 1f
        }
        for (x in 0..960 step 48) canvas.drawLine(x.toFloat(), 0f, x.toFloat(), BOARD_HEIGHT, gridPaint)
        for (y in 0..540 step 48) canvas.drawLine(0f, y.toFloat(), BOARD_WIDTH, y.toFloat(), gridPaint)
    }

    frame.strokes.forEach { stroke ->
        if (stroke.points.isEmpty()) return@forEach
        val preset = if (stroke.tool == DrawingTool.ERASER) BrushPreset.INK else stroke.brush
        val color = if (stroke.tool == DrawingTool.ERASER) material.backgroundColor else stroke.color
        val styledWidth = (stroke.width * preset.widthScale).coerceAtLeast(1f)
        val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            alpha = if (stroke.tool == DrawingTool.ERASER) 255 else
                (AndroidColor.alpha(color) * preset.opacity).roundToInt().coerceIn(0, 255)
            strokeWidth = styledWidth
            strokeCap = if (preset.squareCaps) AndroidPaint.Cap.SQUARE else AndroidPaint.Cap.ROUND
            strokeJoin = AndroidPaint.Join.ROUND
            pathEffect = if (preset.dashed) {
                DashPathEffect(floatArrayOf(max(3f, styledWidth * 0.45f), max(2f, styledWidth * 0.32f)), 0f)
            } else null
            maskFilter = if (preset == BrushPreset.AIRBRUSH) {
                BlurMaskFilter(max(2f, styledWidth * 0.28f), BlurMaskFilter.Blur.NORMAL)
            } else null
            style = AndroidPaint.Style.STROKE
        }
        val first = stroke.points.first()
        val last = stroke.points.last()
        when (stroke.tool) {
            DrawingTool.RECTANGLE, DrawingTool.ELLIPSE -> {
                if (stroke.points.size == 1) {
                    paint.style = AndroidPaint.Style.FILL
                    canvas.drawCircle(first.x, first.y, styledWidth / 2f, paint)
                } else {
                    val rect = RectF(
                        min(first.x, last.x),
                        min(first.y, last.y),
                        max(first.x, last.x),
                        max(first.y, last.y)
                    )
                    if (stroke.tool == DrawingTool.RECTANGLE) canvas.drawRect(rect, paint) else canvas.drawOval(rect, paint)
                }
            }
            DrawingTool.LINE -> {
                if (stroke.points.size == 1) {
                    paint.style = AndroidPaint.Style.FILL
                    canvas.drawCircle(first.x, first.y, styledWidth / 2f, paint)
                } else {
                    canvas.drawLine(first.x, first.y, last.x, last.y, paint)
                }
            }
            DrawingTool.BRUSH, DrawingTool.ERASER -> {
                if (stroke.points.size == 1) {
                    paint.style = AndroidPaint.Style.FILL
                    canvas.drawCircle(first.x, first.y, styledWidth / 2f, paint)
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

private fun exportAnimatedGif(
    output: OutputStream,
    frames: List<AnimationFrame>,
    fps: Int,
    material: CanvasMaterial = CanvasMaterial.GRID
) {
    require(frames.isNotEmpty()) { "Não existem frames para exportar" }
    val width = 480
    val height = 270
    val delayMillis = (1000L / fps.coerceIn(1, 30)).coerceAtLeast(10L)
    val encoder = GifEncoder(output, width, height, 0)
    val options = ImageOptions().setDelay(delayMillis, TimeUnit.MILLISECONDS)

    frames.forEach { frame ->
        val bitmap = renderFrameBitmap(frame, width, height, material)
        try {
            val argb = IntArray(width * height)
            bitmap.getPixels(argb, 0, width, 0, 0, width, height)
            val rgb = IntArray(argb.size) { index -> argb[index] and 0x00FFFFFF }
            encoder.addImage(rgb, width, options)
        } finally {
            bitmap.recycle()
        }
    }
    encoder.finishEncoding()
}

private fun exportPngSequenceZip(
    output: OutputStream,
    frames: List<AnimationFrame>,
    fps: Int,
    material: CanvasMaterial = CanvasMaterial.GRID
) {
    require(frames.isNotEmpty()) { "Não existem frames para exportar" }
    ZipOutputStream(output).use { zip ->
        frames.forEachIndexed { index, frame ->
            val fileName = String.format(Locale.US, "frame-%04d.png", index + 1)
            zip.putNextEntry(ZipEntry(fileName))
            val bitmap = renderFrameBitmap(frame, 1920, 1080, material)
            try {
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, zip)) {
                    throw IOException("Não foi possível codificar o frame $fileName")
                }
            } finally {
                bitmap.recycle()
            }
            zip.closeEntry()
        }
        zip.putNextEntry(ZipEntry("animation-info.txt"))
        zip.write(
            ("FPS=$fps\nFRAMES=" + frames.size + "\nSIZE=1920x1080\nBACKGROUND=white\n").toByteArray(Charsets.UTF_8)
        )
        zip.closeEntry()
    }
}
