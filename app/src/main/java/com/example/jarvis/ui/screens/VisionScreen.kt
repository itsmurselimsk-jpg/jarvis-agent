package com.example.jarvis.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvis.model.VisionScan
import com.example.jarvis.provider.AIProvider
import com.example.jarvis.ui.theme.JarvisAmber
import com.example.jarvis.ui.theme.JarvisBackground
import com.example.jarvis.ui.theme.JarvisBorder
import com.example.jarvis.ui.theme.JarvisBorderSubtle
import com.example.jarvis.ui.theme.JarvisCyan
import com.example.jarvis.ui.theme.JarvisCyanBright
import com.example.jarvis.ui.theme.JarvisElectricBlue
import com.example.jarvis.ui.theme.JarvisGreen
import com.example.jarvis.ui.theme.JarvisTextDim
import com.example.jarvis.ui.theme.JarvisTextPrimary
import com.example.jarvis.ui.theme.JarvisTextSecondary
import com.example.jarvis.vision.JarvisVisionEngine
import com.example.jarvis.vision.SensitiveDataFilter
import com.example.jarvis.vision.VisionActionType
import com.example.jarvis.vision.VisionDerivedAction
import com.example.jarvis.vision.VisionEngine
import com.example.jarvis.vision.VisionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class VisionViewMode {
    STRUCTURED,
    RAW_TEXT,
    ACTIONS
}

@Composable
fun VisionScreen(
    scans: List<VisionScan>,
    aiProvider: AIProvider,
    onAddScan: (VisionScan) -> Unit,
    visionEngine: VisionEngine = remember { JarvisVisionEngine() },
    activeResult: VisionResult? = null,
    onSetActiveResult: (VisionResult, Uri?) -> Unit = { _, _ -> },
    onExecuteAction: (VisionDerivedAction) -> Unit = {},
    onAskJarvis: (String) -> Unit = {},
    onCopyToClipboard: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isAnalyzing by remember { mutableStateOf(false) }
    var currentUri by remember { mutableStateOf<Uri?>(null) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentVisionResult by remember { mutableStateOf<VisionResult?>(activeResult) }
    var derivedActions by remember { mutableStateOf<List<VisionDerivedAction>>(emptyList()) }
    var viewMode by remember { mutableStateOf(VisionViewMode.STRUCTURED) }
    var maskSensitive by remember { mutableStateOf(true) }
    var localSummary by remember { mutableStateOf<String?>(null) }
    var isSummarizing by remember { mutableStateOf(false) }
    var copyNotice by remember { mutableStateOf<String?>(null) }

    // Keep state in sync with external activeResult if set
    LaunchedEffect(activeResult) {
        if (activeResult != null && activeResult != currentVisionResult) {
            currentVisionResult = activeResult
            derivedActions = SensitiveDataFilter.extractActions(activeResult.extractedText)
        }
    }

    // Camera capture launcher for live photo analysis
    val cameraCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp: Bitmap? ->
        if (bmp != null) {
            currentBitmap = bmp
            currentUri = null
            isAnalyzing = true
            localSummary = null
            coroutineScope.launch {
                try {
                    val result = visionEngine.processBitmap(bmp)
                    currentVisionResult = result
                    onSetActiveResult(result, null)

                    val actions = if (result.success) {
                        SensitiveDataFilter.extractActions(result.extractedText)
                    } else emptyList()
                    derivedActions = actions

                    val scan = VisionScan(
                        uriString = "camera_${System.currentTimeMillis()}",
                        fileName = "LIVE_CAPTURE_${System.currentTimeMillis()}.jpg",
                        fileSizeFormatted = "${(bmp.byteCount / 1024).coerceAtLeast(1)} KB",
                        analysisResult = if (result.success) {
                            if (result.extractedText.isNotBlank()) {
                                "Live Optical Scan: ${result.extractedText.length} chars (${result.detectedLanguage ?: "Latin"})."
                            } else {
                                "Live camera frame captured. No text detected."
                            }
                        } else {
                            "Failed: ${result.errorMessage}"
                        }
                    )
                    onAddScan(scan)
                } catch (e: Exception) {
                    currentVisionResult = VisionResult.failure("Camera scan error: ${e.message}", null)
                } finally {
                    isAnalyzing = false
                }
            }
        }
    }

    // Photo picker launcher (complies with Android 13+ zero-permission photo picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            currentUri = uri
            isAnalyzing = true
            localSummary = null
            coroutineScope.launch {
                try {
                    // Safe downsampled bitmap load
                    val bmp = withContext(Dispatchers.IO) {
                        try {
                            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            var stream = context.contentResolver.openInputStream(uri)
                            BitmapFactory.decodeStream(stream, null, options)
                            stream?.close()

                            var sampleSize = 1
                            val maxDimension = 1280
                            val maxSide = maxOf(options.outWidth, options.outHeight)
                            while (maxSide / sampleSize > maxDimension) {
                                sampleSize *= 2
                            }

                            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                            stream = context.contentResolver.openInputStream(uri)
                            val decoded = BitmapFactory.decodeStream(stream, null, decodeOptions)
                            stream?.close()
                            decoded
                        } catch (_: Exception) {
                            null
                        }
                    }
                    currentBitmap = bmp

                    // Run On-Device OCR via VisionEngine
                    val result = visionEngine.processImage(context, uri)
                    currentVisionResult = result
                    onSetActiveResult(result, uri)

                    // Extract entities & actionable items
                    val actions = if (result.success) {
                        SensitiveDataFilter.extractActions(result.extractedText)
                    } else emptyList()
                    derivedActions = actions

                    // Save scan record in Room repository
                    val scan = VisionScan(
                        uriString = uri.toString(),
                        fileName = result.metadata?.fileName ?: "IMG_${System.currentTimeMillis()}.jpg",
                        fileSizeFormatted = result.metadata?.fileSizeFormatted ?: "Unknown size",
                        analysisResult = if (result.success) {
                            if (result.extractedText.isNotBlank()) {
                                "Extracted ${result.extractedText.length} characters on-device (${result.detectedLanguage ?: "Latin"})."
                            } else {
                                "Optical frame analyzed. No readable text characters detected."
                            }
                        } else {
                            result.errorMessage ?: "Analysis failed."
                        },
                        status = if (result.success) "Completed" else "Error"
                    )
                    onAddScan(scan)
                } catch (e: Exception) {
                    val errResult = VisionResult.failure("Execution error: ${e.localizedMessage}", null)
                    currentVisionResult = errResult
                } finally {
                    isAnalyzing = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // TOP HUD BAR
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "OPTICAL VISION & OCR HUD",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = JarvisCyan
                )
                Text(
                    text = "On-Device Multimodal & OCR Engine",
                    fontSize = 11.sp,
                    color = JarvisTextSecondary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { cameraCaptureLauncher.launch(null) },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisCyanBright, contentColor = Color.Black),
                    modifier = Modifier.testTag("camera_capture_button")
                ) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Camera", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("CAMERA", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = Color.Black),
                    modifier = Modifier.testTag("scan_image_button")
                ) {
                    Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("GALLERY", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ANALYZING PROGRESS BANNER
        if (isAnalyzing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0A1828))
                    .border(1.dp, JarvisCyan, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(color = JarvisCyan, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Column {
                        Text("EXECUTING ON-DEVICE OPTICAL RECOGNITION...", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = JarvisCyanBright)
                        Text("Privacy preserved: Optical pixels are evaluated entirely in local memory.", fontSize = 10.sp, color = JarvisTextSecondary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // ACTIVE INSPECTION VIEW
        val result = currentVisionResult
        if (result != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. ACTIVE IMAGE PREVIEW & METADATA CARD
                item {
                    ActiveImageCard(
                        bitmap = currentBitmap,
                        result = result,
                        maskSensitive = maskSensitive,
                        onToggleMask = { maskSensitive = !maskSensitive }
                    )
                }

                // 2. PRIVACY & SECURITY BANNER
                item {
                    PrivacyBanner(
                        result = result,
                        maskSensitive = maskSensitive,
                        onToggleMask = { maskSensitive = !maskSensitive }
                    )
                }

                // 3. QUICK ACTION BAR
                item {
                    QuickActionBar(
                        result = result,
                        derivedActions = derivedActions,
                        isSummarizing = isSummarizing,
                        onSummarize = {
                            if (result.extractedText.isNotBlank()) {
                                isSummarizing = true
                                coroutineScope.launch {
                                    localSummary = visionEngine.summarizeText(result.extractedText, aiProvider)
                                    isSummarizing = false
                                }
                            }
                        },
                        onAskJarvis = {
                            val prompt = "Based on this image text: \"${result.extractedText.take(150)}\", what can you tell me?"
                            onAskJarvis(prompt)
                        },
                        onCopy = {
                            val textToCopy = if (maskSensitive && result.containsSensitiveData) {
                                SensitiveDataFilter.redactSensitiveData(result.extractedText)
                            } else result.extractedText
                            onCopyToClipboard(textToCopy)
                            copyNotice = "Text copied to clipboard"
                        }
                    )
                }

                // Temporary copy toast notice
                if (copyNotice != null) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF0B2418))
                                .border(0.5.dp, JarvisGreen, RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = JarvisGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(copyNotice ?: "", color = JarvisGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }

                // 4. LOCAL SUMMARY (if generated)
                if (localSummary != null) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0D1C2E))
                                .border(0.5.dp, JarvisCyan, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Summarize, contentDescription = null, tint = JarvisCyan, modifier = Modifier.size(16.dp))
                                    Text("EXECUTIVE OPTICAL SUMMARY", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = JarvisCyan)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(localSummary ?: "", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = JarvisTextPrimary, lineHeight = 16.sp)
                            }
                        }
                    }
                }

                // 5. EXTRACTED ENTITIES CARD (Phone numbers, URLs, Emails, OTPs)
                if (derivedActions.isNotEmpty()) {
                    item {
                        EntitiesCard(
                            actions = derivedActions,
                            maskSensitive = maskSensitive,
                            onExecuteAction = onExecuteAction,
                            onAskJarvis = onAskJarvis
                        )
                    }
                }

                // 6. VIEW MODE SELECTOR (Structured vs Raw Text)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            VisionViewMode.STRUCTURED to "STRUCTURED OCR",
                            VisionViewMode.RAW_TEXT to "RAW TEXT",
                            VisionViewMode.ACTIONS to "DERIVED ACTIONS (${derivedActions.size})"
                        ).forEach { (mode, label) ->
                            val isSelected = viewMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) JarvisCyan else Color(0xFF0C1420))
                                    .border(0.5.dp, if (isSelected) JarvisCyan else JarvisBorderSubtle, RoundedCornerShape(6.dp))
                                    .clickable { viewMode = mode }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isSelected) Color.Black else JarvisTextSecondary
                                )
                            }
                        }
                    }
                }

                // 7. OCR VIEW CONTENT ACCORDING TO SELECTED TAB
                item {
                    when (viewMode) {
                        VisionViewMode.STRUCTURED -> {
                            StructuredOcrView(
                                result = result,
                                maskSensitive = maskSensitive
                            )
                        }
                        VisionViewMode.RAW_TEXT -> {
                            RawTextView(
                                result = result,
                                maskSensitive = maskSensitive,
                                onCopy = {
                                    val text = if (maskSensitive && result.containsSensitiveData) {
                                        SensitiveDataFilter.redactSensitiveData(result.extractedText)
                                    } else result.extractedText
                                    onCopyToClipboard(text)
                                    copyNotice = "Raw text copied to clipboard"
                                }
                            )
                        }
                        VisionViewMode.ACTIONS -> {
                            DerivedActionsFullView(
                                actions = derivedActions,
                                maskSensitive = maskSensitive,
                                onExecuteAction = onExecuteAction
                            )
                        }
                    }
                }

                // PAST SCANS SECTION HEADER
                if (scans.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "PREVIOUS OPTICAL SCANS (${scans.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = JarvisTextSecondary
                        )
                    }

                    items(scans.take(5), key = { it.id }) { scan ->
                        VisionScanCard(scan = scan)
                    }
                }
            }
        } else {
            // EMPTY STATE: No active image yet
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF090E1A))
                            .border(1.dp, JarvisBorderSubtle, RoundedCornerShape(10.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = JarvisCyan, modifier = Modifier.size(36.dp))
                            Text("NO IMAGE LOADED IN HUD", fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = JarvisTextPrimary)
                            Text(
                                text = "Select an image or document screenshot to perform high-precision on-device OCR, extract phone numbers/links, detect OTPs, and ask JARVIS questions.",
                                fontSize = 11.sp,
                                color = JarvisTextSecondary,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = Color.Black)
                            ) {
                                Text("SELECT FROM DEVICE", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }

                if (scans.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "PREVIOUS OPTICAL SCANS (${scans.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = JarvisTextSecondary
                        )
                    }

                    items(scans, key = { it.id }) { scan ->
                        VisionScanCard(scan = scan)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveImageCard(
    bitmap: Bitmap?,
    result: VisionResult,
    maskSensitive: Boolean,
    onToggleMask: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF090F1C))
            .border(0.5.dp, JarvisBorder, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (result.success) JarvisGreen else JarvisAmber)
                    )
                    Text(
                        text = result.metadata?.fileName ?: "ACTIVE_FRAME.JPG",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = JarvisTextPrimary
                    )
                }

                Text(
                    text = "${result.processingTimeMs}ms",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = JarvisCyan
                )
            }

            // Image Preview thumbnail if bitmap loaded
            if (bitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black)
                        .border(0.5.dp, JarvisBorderSubtle, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Active Image Frame",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Resolution: ${result.metadata?.width ?: 0}x${result.metadata?.height ?: 0} px",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = JarvisTextDim
                )
                Text(
                    text = "Size: ${result.metadata?.fileSizeFormatted ?: "N/A"}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = JarvisTextDim
                )
                Text(
                    text = "Script: ${result.detectedLanguage ?: "Latin"}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = JarvisCyan
                )
            }
        }
    }
}

@Composable
private fun PrivacyBanner(
    result: VisionResult,
    maskSensitive: Boolean,
    onToggleMask: () -> Unit
) {
    val hasSensitive = result.containsSensitiveData
    val bgColor = if (hasSensitive) Color(0xFF231405) else Color(0xFF071911)
    val borderColor = if (hasSensitive) JarvisAmber else JarvisGreen
    val iconColor = if (hasSensitive) JarvisAmber else JarvisGreen

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(0.5.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (hasSensitive) Icons.Default.Warning else Icons.Default.Security,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = if (hasSensitive) {
                            "SENSITIVE DATA DETECTED (${result.sensitiveEntitiesDetected.joinToString()})"
                        } else {
                            "LOCAL ON-DEVICE OCR: 100% PRIVATE"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = iconColor
                    )
                    Text(
                        text = if (hasSensitive) {
                            "Authentication tokens & card numbers are kept local and shielded from long-term memory."
                        } else {
                            "Zero external transmission. Image was analyzed entirely on this device."
                        },
                        fontSize = 10.sp,
                        color = JarvisTextSecondary
                    )
                }
            }

            if (hasSensitive) {
                IconButton(
                    onClick = onToggleMask,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (maskSensitive) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (maskSensitive) "Unmask text" else "Mask text",
                        tint = JarvisAmber,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionBar(
    result: VisionResult,
    derivedActions: List<VisionDerivedAction>,
    isSummarizing: Boolean,
    onSummarize: () -> Unit,
    onAskJarvis: () -> Unit,
    onCopy: () -> Unit
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Copy button
        Button(
            onClick = onCopy,
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E1A29), contentColor = JarvisCyan),
            modifier = Modifier.height(34.dp)
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("COPY TEXT", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }

        // Summarize button
        Button(
            onClick = onSummarize,
            enabled = !isSummarizing && result.extractedText.isNotBlank(),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E1A29), contentColor = JarvisCyanBright),
            modifier = Modifier.height(34.dp)
        ) {
            if (isSummarizing) {
                CircularProgressIndicator(color = JarvisCyan, modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
            } else {
                Icon(Icons.Default.Summarize, contentDescription = null, modifier = Modifier.size(14.dp))
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text("SUMMARIZE", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }

        // Ask JARVIS button
        Button(
            onClick = onAskJarvis,
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan, contentColor = Color.Black),
            modifier = Modifier.height(34.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("ASK JARVIS", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EntitiesCard(
    actions: List<VisionDerivedAction>,
    maskSensitive: Boolean,
    onExecuteAction: (VisionDerivedAction) -> Unit,
    onAskJarvis: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF081220))
            .border(0.5.dp, JarvisCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = JarvisCyan, modifier = Modifier.size(15.dp))
                Text("DETECTED ENTITIES & ACTIONS", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = JarvisCyan)
            }

            actions.forEach { action ->
                EntityActionRow(
                    action = action,
                    maskSensitive = maskSensitive,
                    onExecute = { onExecuteAction(action) }
                )
            }
        }
    }
}

@Composable
private fun EntityActionRow(
    action: VisionDerivedAction,
    maskSensitive: Boolean,
    onExecute: () -> Unit
) {
    val displayPayload = if (action.isSensitive && maskSensitive) {
        SensitiveDataFilter.redactSensitiveData(action.payload)
    } else action.payload

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0D1726))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            val icon = when (action.type) {
                VisionActionType.DIAL_PHONE -> Icons.Default.Call
                VisionActionType.OPEN_URL -> Icons.Default.OpenInBrowser
                VisionActionType.SEND_EMAIL -> Icons.Default.Email
                VisionActionType.SEARCH_PHONE -> Icons.Default.Search
                VisionActionType.COPY_TEXT -> if (action.isSensitive) Icons.Default.Lock else Icons.Default.ContentCopy
            }
            val tint = if (action.isSensitive) JarvisAmber else JarvisCyan

            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Column {
                Text(action.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = JarvisTextPrimary)
                Text(displayPayload, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = JarvisTextSecondary)
            }
        }

        Button(
            onClick = onExecute,
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (action.isSensitive) JarvisAmber else JarvisCyan, contentColor = Color.Black),
            modifier = Modifier.height(28.dp)
        ) {
            Text(
                text = when (action.type) {
                    VisionActionType.DIAL_PHONE -> "DIAL"
                    VisionActionType.OPEN_URL -> "OPEN"
                    VisionActionType.SEND_EMAIL -> "COMPOSE"
                    VisionActionType.SEARCH_PHONE -> "SEARCH"
                    VisionActionType.COPY_TEXT -> "COPY"
                },
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun StructuredOcrView(
    result: VisionResult,
    maskSensitive: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF090E1A))
            .border(0.5.dp, JarvisBorderSubtle, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("PARSED TEXT BLOCKS (${result.blocks.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = JarvisCyan)
                Text("Total Lines: ${result.blocks.sumOf { it.lines.size }}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = JarvisTextDim)
            }

            if (result.blocks.isEmpty()) {
                Text(
                    text = result.errorMessage ?: "No text blocks were extracted from the optical scan.",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = JarvisTextDim
                )
            } else {
                result.blocks.forEachIndexed { idx, block ->
                    val blockText = if (maskSensitive && result.containsSensitiveData) {
                        SensitiveDataFilter.redactSensitiveData(block.text)
                    } else block.text

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0D1624))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Block #${idx + 1} (${block.lines.size} lines)",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = JarvisCyan
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = blockText,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = JarvisTextPrimary,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RawTextView(
    result: VisionResult,
    maskSensitive: Boolean,
    onCopy: () -> Unit
) {
    val displayText = if (maskSensitive && result.containsSensitiveData) {
        SensitiveDataFilter.redactSensitiveData(result.extractedText)
    } else result.extractedText

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF090E1A))
            .border(0.5.dp, JarvisBorderSubtle, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("RAW EXTRACTED TEXT STREAM", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = JarvisCyan)
                IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = JarvisCyan, modifier = Modifier.size(14.dp))
                }
            }

            Text(
                text = displayText.ifBlank { "No text content available." },
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = JarvisTextPrimary,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun DerivedActionsFullView(
    actions: List<VisionDerivedAction>,
    maskSensitive: Boolean,
    onExecuteAction: (VisionDerivedAction) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (actions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF090E1A))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No actionable entities detected in this frame.", color = JarvisTextDim, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        } else {
            actions.forEach { action ->
                EntityActionRow(
                    action = action,
                    maskSensitive = maskSensitive,
                    onExecute = { onExecuteAction(action) }
                )
            }
        }
    }
}

@Composable
private fun VisionScanCard(scan: VisionScan) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF090E1A))
            .border(0.5.dp, JarvisBorderSubtle, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = JarvisCyan, modifier = Modifier.size(16.dp))
                    Text(text = scan.fileName, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = JarvisTextPrimary)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF0D233A))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = scan.fileSizeFormatted, fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = JarvisCyan)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = scan.analysisResult,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = JarvisCyanBright,
                lineHeight = 15.sp
            )
        }
    }
}
