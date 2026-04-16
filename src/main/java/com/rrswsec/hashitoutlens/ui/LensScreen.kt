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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rrswsec.hashitoutlens.camera.CameraPreview
import com.rrswsec.hashitoutlens.core.model.Confidence
import com.rrswsec.hashitoutlens.core.model.DecodeFinding

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
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.78f)),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(uiState.modeLabel, style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = bestFinding?.resultText ?: "Point the camera at a cipher, QR, or hidden text source.",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        AssistChip(onClick = {}, label = { Text(bestFinding?.method ?: "Waiting") })
                        bestFinding?.let { AssistChip(onClick = {}, label = { Text(it.confidence.name) }) }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { showResults = true }, modifier = Modifier.weight(1f)) { Text("Results") }
                Button(onClick = { showTextPanel = true }, modifier = Modifier.weight(1f)) { Text("View text") }
                Button(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.weight(1f)
                ) { Text("Import") }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = Color.Black.copy(alpha = 0.65f),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("OCR", color = Color.White, style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = uiState.recognizedText.ifBlank { "No text recognized yet" },
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (uiState.barcodeTexts.isNotEmpty()) {
                        Text("Barcodes: ${uiState.barcodeTexts.size}", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
                    }
                    if (uiState.hiddenTexts.isNotEmpty()) {
                        Text("Hidden text hits: ${uiState.hiddenTexts.size}", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    if (showResults) {
        ModalBottomSheet(onDismissRequest = { showResults = false }, dragHandle = { BottomSheetDefaults.DragHandle() }) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Ranked results", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    uiState.findings.forEach { finding -> ResultRow(finding = finding, onClick = { expandedFinding = finding }) }
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
                Text("Detected text", style = MaterialTheme.typography.headlineSmall)
                SectionText("OCR", uiState.recognizedText.ifBlank { "None" })
                SectionText("Barcodes / QR", uiState.barcodeTexts.joinToString("

").ifBlank { "None" })
                SectionText("Hidden text", uiState.hiddenTexts.joinToString("

").ifBlank { "None" })
                TextButton(onClick = { showTextPanel = false }) { Text("Close") }
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
                AssistChip(onClick = {}, label = { Text(finding.confidence.name) })
                Text("Score: %.1f".format(finding.score), style = MaterialTheme.typography.labelLarge)
                HorizontalDivider()
                Text(finding.resultText, style = MaterialTheme.typography.bodyLarge)
                if (finding.note.isNotBlank()) {
                    Text("Note", fontWeight = FontWeight.Bold)
                    Text(finding.note)
                }
                if (finding.why.isNotBlank()) {
                    Text("Why", fontWeight = FontWeight.Bold)
                    Text(finding.why)
                }
                if (finding.chain.isNotEmpty()) {
                    Text("Chain", fontWeight = FontWeight.Bold)
                    Text(finding.chain.joinToString(" → "))
                }
                TextButton(onClick = { expandedFinding = null }) { Text("Close") }
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
            Box(modifier = Modifier.size(10.dp).background(confidenceColor, shape = MaterialTheme.shapes.small))
            Text(finding.method, fontWeight = FontWeight.SemiBold)
            Text("%.1f".format(finding.score), color = MaterialTheme.colorScheme.outline)
        }
        Text(text = finding.resultText, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
        HorizontalDivider()
    }
}
