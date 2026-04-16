package com.rrswsec.hashitoutlens.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.rrswsec.hashitoutlens.camera.CameraPreview
import com.rrswsec.hashitoutlens.core.model.Confidence
import com.rrswsec.hashitoutlens.core.model.DecodeFinding

private val banner = """
[ hashitout lens ]
[ skunk mode // point and stare ]
""".trimIndent()

@Composable
fun LensScreen(viewModel: LensViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val bestFinding = uiState.findings.firstOrNull()
    var showResults by remember { mutableStateOf(false) }
    var expandedFinding by remember { mutableStateOf<DecodeFinding?>(null) }
    var showTextPanel by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) viewModel.analyzeImportedImage(context, uri)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            onFrameResult = viewModel::onLiveFrame,
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Transparent, Color(0x99000000), Color(0xE6000000))
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xF0131713)),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = banner,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                    Text(
                        text = "mode: ${uiState.modeLabel}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        text = bestFinding?.resultText ?: "point the camera at a cipher, qr, or weird little code critter.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        AssistChip(onClick = {}, label = { Text(bestFinding?.method ?: "waiting") })
                        bestFinding?.let { AssistChip(onClick = {}, label = { Text(it.confidence.name.lowercase()) }) }
                        if (uiState.isAnalyzing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { showResults = true }, modifier = Modifier.weight(1f)) { Text("results") }
                Button(onClick = { showTextPanel = true }, modifier = Modifier.weight(1f)) { Text("view text") }
                Button(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.weight(1f)
                ) { Text("import") }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xD10F120F),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "raw feed",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        text = uiState.recognizedText.ifBlank { "no text recognized yet" },
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (uiState.barcodeTexts.isNotEmpty()) {
                        Text(
                            text = "barcodes: ${uiState.barcodeTexts.size}",
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                    if (uiState.hiddenTexts.isNotEmpty()) {
                        Text(
                            text = "hidden text hits: ${uiState.hiddenTexts.size}",
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }
        }
    }

    if (showResults) {
        ModalBottomSheet(onDismissRequest = { showResults = false }, dragHandle = { BottomSheetDefaults.DragHandle() }) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "ranked results",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    uiState.findings.forEach { finding ->
                        ResultRow(finding = finding, onClick = { expandedFinding = finding })
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
                Text("detected text", style = MaterialTheme.typography.headlineSmall)
                SectionText("ocr", uiState.recognizedText.ifBlank { "none" })
                SectionText("barcodes / qr", uiState.barcodeTexts.joinToString("\n\n").ifBlank { "none" })
                SectionText("hidden text", uiState.hiddenTexts.joinToString("\n\n").ifBlank { "none" })
                TextButton(onClick = { showTextPanel = false }) { Text("close") }
            }
        }
    }

    expandedFinding?.let { finding ->
        ModalBottomSheet(onDismissRequest = { expandedFinding = null }, dragHandle = { BottomSheetDefaults.DragHandle() }) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(finding.method, style = MaterialTheme.typography.headlineSmall)
                AssistChip(onClick = {}, label = { Text(finding.confidence.name.lowercase()) })
                Text("score: %.1f".format(finding.score), style = MaterialTheme.typography.labelLarge, fontFamily = FontFamily.Monospace)
                HorizontalDivider()
                Text(finding.resultText, style = MaterialTheme.typography.bodyLarge)
                if (finding.note.isNotBlank()) {
                    Text("note", fontWeight = FontWeight.Bold)
                    Text(finding.note)
                }
                if (finding.why.isNotBlank()) {
                    Text("why", fontWeight = FontWeight.Bold)
                    Text(finding.why)
                }
                if (finding.chain.isNotEmpty()) {
                    Text("chain", fontWeight = FontWeight.Bold)
                    Text(finding.chain.joinToString(" -> "), fontFamily = FontFamily.Monospace)
                }
                TextButton(onClick = { expandedFinding = null }) { Text("close") }
            }
        }
    }
}

@Composable
private fun SectionText(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontWeight = FontWeight.Bold)
        Text(value, style = MaterialTheme.typography.bodyMedium)
        HorizontalDivider()
    }
}

@Composable
private fun ResultRow(finding: DecodeFinding, onClick: () -> Unit) {
    val confidenceColor = when (finding.confidence) {
        Confidence.HIGH -> MaterialTheme.colorScheme.primary
        Confidence.MEDIUM -> MaterialTheme.colorScheme.tertiary
        Confidence.LOW -> MaterialTheme.colorScheme.outline
    }

    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.size(10.dp).background(confidenceColor, shape = RoundedCornerShape(99.dp)))
            Text(finding.method, fontWeight = FontWeight.SemiBold)
            Text("%.1f".format(finding.score), color = MaterialTheme.colorScheme.outline, fontFamily = FontFamily.Monospace)
        }
        Text(text = finding.resultText, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
        HorizontalDivider()
    }
}
