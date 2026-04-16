package com.rrswsec.hashitoutlens.ui

import android.graphics.RectF
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rrswsec.hashitoutlens.camera.CameraPreview
import com.rrswsec.hashitoutlens.core.model.DecodeFinding
import kotlin.random.Random

private val banner = """
[ hashitout lens ]
[ skunk mode // point and stare ]
""".trimIndent()

private val PhosphorGreen = Color(0xFFA2D9A2) // Soft ambient green

@Composable
fun outlinedTextStyle(
    baseColor: Color = Color.White.copy(alpha = 0.98f),
    baseStyle: TextStyle = MaterialTheme.typography.titleMedium
): TextStyle {
    return baseStyle.copy(
        color = baseColor,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.8f),
            offset = Offset(1f, 1f),
            blurRadius = 1f
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LensScreen(viewModel: LensViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    
    var showResults by remember { mutableStateOf(false) }
    var expandedFinding by remember { mutableStateOf<DecodeFinding?>(null) }
    var showTextPanel by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) viewModel.analyzeImportedImage(context, uri)
    }

    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragEnd by remember { mutableStateOf<Offset?>(null) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    
    // Persistent Selection State
    var selectionRect by remember { mutableStateOf<RectF?>(null) }
    var isDraggingHandle by remember { mutableStateOf<Int?>(null) } // 0: TopLeft, 1: TopRight, 2: BottomLeft, 3: BottomRight, 4: Body

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
        .onGloballyPositioned { containerSize = it.size }
    ) {
        if (uiState.isFrozen && uiState.frozenBitmap != null) {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    bitmap = uiState.frozenBitmap!!.asImageBitmap(),
                    contentDescription = "captured frame",
                    modifier = Modifier.fillMaxSize()
                        .pointerInput(uiState.isFrozen) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val rect = selectionRect
                                    if (rect != null) {
                                        val x = offset.x / containerSize.width
                                        val y = offset.y / containerSize.height
                                        val threshold = 0.05f
                                        
                                        isDraggingHandle = when {
                                            Math.abs(x - rect.left) < threshold && Math.abs(y - rect.top) < threshold -> 0
                                            Math.abs(x - rect.right) < threshold && Math.abs(y - rect.top) < threshold -> 1
                                            Math.abs(x - rect.left) < threshold && Math.abs(y - rect.bottom) < threshold -> 2
                                            Math.abs(x - rect.right) < threshold && Math.abs(y - rect.bottom) < threshold -> 3
                                            rect.contains(x, y) -> 4
                                            else -> null
                                        }
                                    }
                                    
                                    if (isDraggingHandle == null) {
                                        dragStart = offset
                                        dragEnd = offset
                                        selectionRect = null
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    val handle = isDraggingHandle
                                    val rect = selectionRect
                                    if (handle != null && rect != null) {
                                        val dx = dragAmount.x / containerSize.width
                                        val dy = dragAmount.y / containerSize.height
                                        
                                        selectionRect = when (handle) {
                                            0 -> RectF(rect.left + dx, rect.top + dy, rect.right, rect.bottom)
                                            1 -> RectF(rect.left, rect.top + dy, rect.right + dx, rect.bottom)
                                            2 -> RectF(rect.left + dx, rect.top, rect.right, rect.bottom + dy)
                                            3 -> RectF(rect.left, rect.top, rect.right + dx, rect.bottom + dy)
                                            4 -> RectF(rect.left + dx, rect.top + dy, rect.right + dx, rect.bottom + dy)
                                            else -> rect
                                        }
                                    } else {
                                        dragEnd = change.position
                                    }
                                },
                                onDragEnd = {
                                    if (isDraggingHandle == null) {
                                        val start = dragStart
                                        val end = dragEnd
                                        if (start != null && end != null) {
                                            selectionRect = RectF(
                                                minOf(start.x, end.x) / containerSize.width,
                                                minOf(start.y, end.y) / containerSize.height,
                                                maxOf(start.x, end.x) / containerSize.width,
                                                maxOf(start.y, end.y) / containerSize.height
                                            )
                                        }
                                    }
                                    
                                    selectionRect?.let { viewModel.onSelectionMade(it) }
                                    dragStart = null
                                    dragEnd = null
                                    isDraggingHandle = null
                                }
                            )
                        },
                    contentScale = ContentScale.Fit
                )
                
                // Forensic Overlays (AR + Analysis Preview)
                ArOverlay(uiState = uiState)
                AnalysisWorkingOverlay(uiState = uiState)

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val rect = selectionRect
                    if (rect != null) {
                        val left = rect.left * size.width
                        val top = rect.top * size.height
                        val right = rect.right * size.width
                        val bottom = rect.bottom * size.height
                        
                        // Main Box
                        drawRect(
                            color = PhosphorGreen,
                            topLeft = Offset(left, top),
                            size = Size(right - left, bottom - top),
                            style = Stroke(width = 2.dp.toPx())
                        )
                        
                        // Corner Handles
                        val handleSize = 10.dp.toPx()
                        drawRect(Color.White, Offset(left - handleSize/2, top - handleSize/2), Size(handleSize, handleSize))
                        drawRect(Color.White, Offset(right - handleSize/2, top - handleSize/2), Size(handleSize, handleSize))
                        drawRect(Color.White, Offset(left - handleSize/2, bottom - handleSize/2), Size(handleSize, handleSize))
                        drawRect(Color.White, Offset(right - handleSize/2, bottom - handleSize/2), Size(handleSize, handleSize))
                    } else if (dragStart != null && dragEnd != null) {
                        val start = dragStart!!
                        val end = dragEnd!!
                        drawRect(
                            color = PhosphorGreen.copy(alpha = 0.5f),
                            topLeft = Offset(minOf(start.x, end.x), minOf(start.y, end.y)),
                            size = Size(Math.abs(end.x - start.x), Math.abs(end.y - start.y)),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    onFrameResult = viewModel::onLiveFrame,
                )
                // Also show AR on live feed if locked findings exist
                ArOverlay(uiState = uiState)
            }
        }

        // --- FORENSIC HUD LAYERS ---

        // Top Left: Banner & Mode
        Column(modifier = Modifier.padding(24.dp).align(Alignment.TopStart)) {
            Text(
                text = banner,
                style = outlinedTextStyle(baseStyle = MaterialTheme.typography.labelSmall),
                fontFamily = FontFamily.Monospace,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "[ mode: ${uiState.modeLabel} ]",
                style = outlinedTextStyle(baseColor = PhosphorGreen, baseStyle = MaterialTheme.typography.labelSmall),
                fontFamily = FontFamily.Monospace
            )
        }

        // Left Side: Locked Findings (Charger Side in Landscape)
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
                .width(180.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (uiState.lockedFindings.isNotEmpty()) {
                Text(
                    "LOCKED DATA",
                    style = outlinedTextStyle(baseColor = PhosphorGreen, baseStyle = MaterialTheme.typography.labelSmall),
                    fontFamily = FontFamily.Monospace
                )
                uiState.lockedFindings.take(5).forEach { finding ->
                    LockedFindingItem(finding = finding)
                }
                if (uiState.lockedFindings.size > 5) {
                    Text(
                        "...+${uiState.lockedFindings.size - 5} more",
                        style = outlinedTextStyle(baseStyle = MaterialTheme.typography.labelSmall),
                        modifier = Modifier.clickable { showResults = true }
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = viewModel::clearResults,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.4f)),
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Text("CLEAR", style = outlinedTextStyle(baseStyle = MaterialTheme.typography.labelSmall))
                }
            }
        }

        // Right Side: Quick Forensic Tools
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .width(100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ControlBtn(text = "CLEAR", highlighted = true, onClick = viewModel::clearResults)
            ControlBtn(text = "IMPORT", onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
            ControlBtn(text = "STEGO", onClick = viewModel::setIrlStegoMode)
            ControlBtn(text = "EXPORT", onClick = { viewModel.exportProduct(context) })
        }

        // Bottom Area: Controls & Results Toggle
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.isFrozen) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Resume Button
                    FloatingActionButton(
                        onClick = viewModel::toggleFreeze,
                        containerColor = PhosphorGreen,
                        contentColor = Color.Black,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("RESUME SCAN", modifier = Modifier.padding(horizontal = 20.dp), style = outlinedTextStyle(baseColor = Color.Black))
                    }
                    
                    // Results View Button
                    FloatingActionButton(
                        onClick = { showResults = true },
                        containerColor = PhosphorGreen.copy(alpha = 0.9f),
                        contentColor = Color.Black,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("VIEW RESULTS (${uiState.findings.size})", modifier = Modifier.padding(horizontal = 20.dp), style = outlinedTextStyle(baseColor = Color.Black))
                    }
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Lock/Confirm Focus Button (Redish)
                    FloatingActionButton(
                        onClick = { viewModel.toggleAutoFreeze() }, // Or a new lock function
                        modifier = Modifier.size(56.dp),
                        containerColor = if (uiState.isAutoFreezeEnabled) Color(0xFFCF6679) else Color(0xFFB00020).copy(alpha = 0.6f),
                        contentColor = Color.White
                    ) {
                        Text("LOCK", style = outlinedTextStyle(baseColor = Color.White, baseStyle = MaterialTheme.typography.labelMedium))
                    }

                    // Large CAP Button
                    FloatingActionButton(
                        onClick = viewModel::toggleFreeze,
                        modifier = Modifier.size(80.dp),
                        containerColor = PhosphorGreen.copy(alpha = 0.8f),
                        contentColor = Color.Black,
                        shape = RoundedCornerShape(40.dp)
                    ) {
                        Text("CAP", style = outlinedTextStyle(baseColor = Color.Black, baseStyle = MaterialTheme.typography.titleLarge))
                    }
                }
            }
        }

        // Analyzing Indicator Removed (Replaced by in-box "work")
        /*
        if (uiState.isAnalyzing) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center).size(48.dp),
                color = PhosphorGreen
            )
        }
        */
    }

    // Bottom Sheets (Results, Text, etc.)
    if (showResults) {
        ModalBottomSheet(
            onDismissRequest = { showResults = false },
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = Color(0xFF0A0C0A)
        ) {
            Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RANKED DISCOVERIES",
                        style = outlinedTextStyle(baseColor = PhosphorGreen, baseStyle = MaterialTheme.typography.titleLarge),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${uiState.findings.size} HITS",
                        style = outlinedTextStyle(baseStyle = MaterialTheme.typography.labelSmall),
                        fontFamily = FontFamily.Monospace
                    )
                }
                
                HorizontalDivider(color = PhosphorGreen.copy(alpha = 0.2f), thickness = 1.dp)

                val sortedFindings = remember(uiState.findings) {
                    uiState.findings.sortedByDescending { it.score }
                }

                if (sortedFindings.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("NO DECODABLE DATA FOUND", style = outlinedTextStyle(baseColor = Color.Gray))
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
                        sortedFindings.forEachIndexed { index, finding ->
                            DetailedResultCard(
                                finding = finding, 
                                isTop = index == 0,
                                isSelected = uiState.selectedFinding == finding,
                                onClick = { 
                                    viewModel.selectFinding(finding)
                                    showResults = false 
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTextPanel) {
        ModalBottomSheet(onDismissRequest = { showTextPanel = false }, dragHandle = { BottomSheetDefaults.DragHandle() }) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("detected text", style = outlinedTextStyle(baseColor = PhosphorGreen, baseStyle = MaterialTheme.typography.headlineSmall))
                SectionText("ocr", uiState.recognizedText.ifBlank { "none" })
                SectionText("barcodes", uiState.barcodeTexts.joinToString("\n").ifBlank { "none" })
                Button(
                    onClick = { showTextPanel = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PhosphorGreen),
                    modifier = Modifier.fillMaxWidth().alpha(0.98f)
                ) { 
                    Text("close", style = outlinedTextStyle(baseStyle = MaterialTheme.typography.labelLarge)) 
                }
            }
        }
    }
}

@Composable
fun ArOverlay(uiState: LensUiState) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        uiState.textBlocks.forEach { block ->
            val normRect = block.normalizedBoundingBox ?: return@forEach
            
            val displayFinding = if (uiState.selectedFinding != null && uiState.selectionRect != null) {
                if (uiState.selectionRect.contains(normRect.centerX(), normRect.centerY())) {
                    uiState.selectedFinding
                } else null
            } else {
                val findings = uiState.lockedFindings.filter { 
                    it.originalText?.trim() == block.text.trim() || 
                    block.text.contains(it.originalText ?: "____") 
                }
                findings.maxByOrNull { it.score }
            }

            if (displayFinding != null) {
                val left = screenWidth * normRect.left
                val top = screenHeight * normRect.top
                val width = screenWidth * (normRect.right - normRect.left)
                val height = screenHeight * (normRect.bottom - normRect.top)

                // Render strictly inside the selected box with case-matching
                Box(
                    modifier = Modifier
                        .offset(x = left, y = top)
                        .size(width, height)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.White.copy(alpha = 0.98f), Color.White.copy(alpha = 0.92f))
                            ),
                            shape = RoundedCornerShape(1.dp)
                        )
                        .padding(horizontal = 2.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val rawSize = height.value * 0.8f
                    val estimatedFontSize = when {
                        rawSize > 24f -> 24.sp
                        rawSize < 6f -> 6.sp
                        else -> rawSize.sp
                    }
                    
                    Text(
                        text = displayFinding.resultText,
                        style = TextStyle(
                            color = Color.Black,
                            fontWeight = FontWeight.Normal,
                            fontSize = estimatedFontSize,
                            fontFamily = FontFamily.Monospace, // Clean, technical, non-all-caps font
                            lineHeight = estimatedFontSize * 1.1f
                        ),
                        modifier = Modifier.fillMaxSize(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun AnalysisWorkingOverlay(uiState: LensUiState) {
    if (!uiState.isAnalyzing) return
    
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        
        uiState.selectionRect?.let { rect ->
            val left = screenWidth * rect.left
            val top = screenHeight * rect.top
            val width = screenWidth * (rect.right - rect.left)
            val height = screenHeight * (rect.bottom - rect.top)
            
            Box(
                modifier = Modifier
                    .offset(x = left, y = top)
                    .size(width, height)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(8.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Column {
                    Text(
                        text = "DECODING...",
                        style = TextStyle(
                            color = PhosphorGreen,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    uiState.decodingLog.forEach { logLine ->
                        Text(
                            text = logLine,
                            style = TextStyle(
                                color = Color.White.copy(alpha = 0.7f),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.sp,
                                lineHeight = 10.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ControlBtn(text: String, highlighted: Boolean = false, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(44.dp).alpha(0.98f),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PhosphorGreen
        )
    ) {
        Text(
            text = if (highlighted) "> $text <" else text, 
            style = outlinedTextStyle(
                baseColor = Color.White,
                baseStyle = MaterialTheme.typography.labelMedium
            ), 
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun DetailedResultCard(finding: DecodeFinding, isTop: Boolean, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() }
            .alpha(0.98f),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> Color(0xFF2E3D2E)
                isTop -> Color(0xFF1E2B1E)
                else -> Color(0xFF121412)
            },
            contentColor = Color.White
        ),
        border = if (isTop || isSelected) androidx.compose.foundation.BorderStroke(1.dp, PhosphorGreen.copy(alpha = 0.5f)) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isTop) {
                            Text(
                                "BEST MATCH",
                                style = TextStyle(
                                    color = PhosphorGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                        Text(
                            finding.method.uppercase(),
                            style = outlinedTextStyle(baseColor = PhosphorGreen, baseStyle = MaterialTheme.typography.labelSmall),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        finding.resultText,
                        style = outlinedTextStyle(baseStyle = MaterialTheme.typography.bodyLarge),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Box(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .background(
                            if (finding.score >= 80) Color(0xFF1B5E20).copy(alpha = 0.3f) else Color.DarkGray.copy(alpha = 0.3f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "${finding.score.toInt()}%",
                        style = outlinedTextStyle(
                            baseColor = if (finding.score >= 80) Color.Green else Color.White,
                            baseStyle = MaterialTheme.typography.titleMedium
                        ),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            
            if (finding.why.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    finding.why,
                    style = TextStyle(color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun LockedFindingItem(finding: DecodeFinding) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
            .padding(8.dp)
    ) {
        Column {
            Text(
                finding.method.uppercase(),
                style = outlinedTextStyle(baseColor = PhosphorGreen, baseStyle = MaterialTheme.typography.labelSmall),
                fontFamily = FontFamily.Monospace
            )
            Text(
                finding.resultText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = outlinedTextStyle(baseStyle = MaterialTheme.typography.bodySmall),
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun SectionText(title: String, content: String) {
    Column {
        Text(title, style = outlinedTextStyle(baseColor = PhosphorGreen, baseStyle = MaterialTheme.typography.labelSmall))
        Text(content, style = outlinedTextStyle(baseStyle = MaterialTheme.typography.bodyMedium))
    }
}

fun formatToPercentage(score: Double): Int {
    return score.toInt().coerceIn(0, 100)
}
