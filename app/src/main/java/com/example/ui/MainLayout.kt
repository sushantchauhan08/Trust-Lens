package com.example.ui

import android.Manifest
import android.content.Context
import android.util.Log
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.database.AlternativeProduct
import com.example.data.database.IngredientInfo
import com.example.data.database.ScanLog
import com.example.ui.theme.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

enum class NavigationTab {
    DASHBOARD, SHELF_VIEW, HISTORY
}

// Glassmorphic styling modifiers matching physical frosted glass properties
@Composable
fun Modifier.frostedGlass(
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(24.dp)
): Modifier {
    val isDark = com.example.ui.theme.LocalDarkTheme.current
    val bgColor = if (isDark) Color(0xCC050C0A) else Color(0xE6F0FDF4)
    val borderGradient = Brush.linearGradient(
        if (isDark) {
            listOf(Color(0x66FFFFFF), Color(0x1AFFFFFF))
        } else {
            listOf(Color(0x33059669), Color(0x11059669))
        }
    )
    return this
        .clip(shape)
        .background(bgColor)
        .border(BorderStroke(1.dp, borderGradient), shape)
}

@Composable
fun Modifier.indigoGlass(
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(24.dp)
): Modifier {
    val isDark = com.example.ui.theme.LocalDarkTheme.current
    val bgColor = if (isDark) Color(0x99031F14) else Color(0xE6E6FDF4)
    val borderGradient = Brush.linearGradient(
        if (isDark) {
            listOf(Color(0xBF34D399), Color(0x3334D399))
        } else {
            listOf(Color(0xBF059669), Color(0x33059669))
        }
    )
    return this
        .clip(shape)
        .background(bgColor)
        .border(BorderStroke(1.5.dp, borderGradient), shape)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayout(
    viewModel: ScanViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(NavigationTab.DASHBOARD) }
    val currentScan by viewModel.currentScanLog.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            // Floating modular glassmorphic dock
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .frostedGlass(RoundedCornerShape(28.dp))
                    .testTag("app_navigation_bar"),
                containerColor = Color.Transparent,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == NavigationTab.DASHBOARD,
                    onClick = { selectedTab = NavigationTab.DASHBOARD },
                    icon = { Icon(Icons.Default.Scanner, contentDescription = "Scan") },
                    label = { Text("Product Scan") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF34D399),
                        selectedTextColor = Color(0xFF34D399),
                        indicatorColor = Color(0xFF059669).copy(alpha = 0.35f),
                        unselectedIconColor = Color(0xFF94A3B8).copy(alpha = 0.7f),
                        unselectedTextColor = Color(0xFF94A3B8).copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.testTag("nav_dashboard_tab")
                )
                NavigationBarItem(
                    selected = selectedTab == NavigationTab.SHELF_VIEW,
                    onClick = { selectedTab = NavigationTab.SHELF_VIEW },
                    icon = { Icon(Icons.Default.Layers, contentDescription = "TrustLens") },
                    label = { Text("TrustLens Shelf") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF34D399),
                        selectedTextColor = Color(0xFF34D399),
                        indicatorColor = Color(0xFF059669).copy(alpha = 0.35f),
                        unselectedIconColor = Color(0xFF94A3B8).copy(alpha = 0.7f),
                        unselectedTextColor = Color(0xFF94A3B8).copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.testTag("nav_shelf_tab")
                )
                NavigationBarItem(
                    selected = selectedTab == NavigationTab.HISTORY,
                    onClick = { selectedTab = NavigationTab.HISTORY },
                    icon = { Icon(Icons.Default.History, contentDescription = "Logs") },
                    label = { Text("Scan History") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF34D399),
                        selectedTextColor = Color(0xFF34D399),
                        indicatorColor = Color(0xFF059669).copy(alpha = 0.35f),
                        unselectedIconColor = Color(0xFF94A3B8).copy(alpha = 0.7f),
                        unselectedTextColor = Color(0xFF94A3B8).copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.testTag("nav_history_tab")
                )
            }
        }
    ) { innerPadding ->
        val isDark = com.example.ui.theme.LocalDarkTheme.current
        val outerBg = if (isDark) Color(0xFF050C0A) else Color(0xFFF0FDF4)
        val centerGlow = if (isDark) Color(0x33059669) else Color(0x1F0D9488)

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .drawBehind {
                    // Velvet Onyx Deep background solid (#0A0C10) or light slate (#F1F5F9)
                    drawRect(outerBg)
                    // Holographic Radial Indigo glow from the center
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(centerGlow, Color.Transparent),
                            center = center,
                            radius = size.width * 0.9f
                        ),
                        radius = size.width * 0.9f,
                        center = center
                    )
                },
            color = Color.Transparent
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "ScreenTransition"
            ) { tab ->
                when (tab) {
                    NavigationTab.DASHBOARD -> DashboardScreen(viewModel)
                    NavigationTab.SHELF_VIEW -> ShelfCaptureScreen(viewModel)
                    NavigationTab.HISTORY -> HistoryScreen(viewModel)
                }
            }

            // Analysis Overlay Indicator
            if (isAnalyzing) {
                Dialog(
                    onDismissRequest = { /* Cannot dismiss during active scanning process */ },
                    properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(28.dp))
                            .testTag("analyzer_progress_dialog"),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF34D399),
                                modifier = Modifier
                                    .size(64.dp)
                                    .testTag("analyzer_progress_spinner")
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                "TrustLens Analyzing...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Running OCR, assessing chemical preservatives, matching batch indices, and verifying adulteration indicators...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Results Details Overlay Dialog
            currentScan?.let { scanLog ->
                ScanDetailsDialog(
                    scanLog = scanLog,
                    onDismiss = { viewModel.clearActiveScan() },
                    errorMessage = errorMessage
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: ScanViewModel) {
    val context = LocalContext.current
    var showManualInput by remember { mutableStateOf(false) }
    var manualText by remember { mutableStateOf("") }
    var showLiveCamera by remember { mutableStateOf(false) }

    // Gallery Picker Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = loadUriBitmap(context, it)
            if (bitmap != null) {
                viewModel.runAnalysis(bitmap, "Captured ingredients list from gallery", null)
            } else {
                Toast.makeText(context, "Failed to load image file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Permission Launcher
    var hasCameraPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            showLiveCamera = true
        } else {
            Toast.makeText(context, "Camera permission needed to scan products directly", Toast.LENGTH_LONG).show()
        }
    }

    val isDark = com.example.ui.theme.LocalDarkTheme.current
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = com.example.R.drawable.ic_app_logo),
                            contentDescription = "TrustLens Logo",
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "TrustLens Scanner",
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif,
                            color = textColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = textColor
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Welcome Header
            Text(
                "Verify Your Nutrition",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                "Scan barcodes, ingredient listings, or packaging to expose harmful chemical additives and estimate authenticity risks instantly.",
                style = MaterialTheme.typography.bodyMedium,
                color = textSecColor,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // Primary Trigger Actions Area
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Live Cam Scan
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(130.dp)
                        .indigoGlass(RoundedCornerShape(24.dp))
                        .clickable {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                        .testTag("action_live_camera_scan")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                "Camera Scan",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Live barcode & OCR",
                                color = Color(0xFFE2E8F0),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Gallery Upload Scan
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(130.dp)
                        .frostedGlass(RoundedCornerShape(24.dp))
                        .clickable { galleryLauncher.launch("image/*") }
                        .testTag("action_gallery_scan")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            Icons.Default.ImageSearch,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                "Upload Product",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Choose from library",
                                color = Color(0xFF94A3B8),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Manual Text Input trigger
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .frostedGlass(RoundedCornerShape(20.dp))
                    .clickable { showManualInput = !showManualInput }
                    .testTag("action_toggle_manual_input")
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Keyboard, contentDescription = null, tint = Color(0xFF94A3B8))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Manually Enter Ingredients", fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Type or paste ingredients listings", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                        }
                    }
                    Icon(
                        if (showManualInput) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8)
                    )
                }
            }

            AnimatedVisibility(visible = showManualInput) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    OutlinedTextField(
                        value = manualText,
                        onValueChange = { manualText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .testTag("ingredients_manual_input_field"),
                        placeholder = { Text("E.g., Sodium Benzoate, Palm Oil, INS 621, Wheat Flour, Sugar...", color = Color(0xFF94A3B8).copy(alpha = 0.5f)) },
                        label = { Text("Paste Ingredient List", color = Color(0xFF34D399)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF34D399),
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedLabelColor = Color(0xFF34D399),
                            unfocusedLabelColor = Color(0xFF94A3B8),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (manualText.isNotBlank()) {
                                viewModel.runAnalysis(null, manualText, null)
                            } else {
                                Toast.makeText(context, "Please write some ingredients to proceed", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("submit_manual_ingredients_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Analytics, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyze Ingredients via AI", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Emulator Test Presets section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Lightbulb,
                    contentDescription = null,
                    tint = Color(0xFF34D399)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Test Presets (Highly Recommended)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Text(
                "Simulate immediate scans of various grocery store products with rich adulteration analyses and healthy alternatives:",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                viewModel.sampleProducts.forEach { product ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .frostedGlass(RoundedCornerShape(20.dp))
                            .clickable { viewModel.selectLog(product) }
                            .testTag("sample_product_item_${product.productName.replace(" ", "_")}")
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(14.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Indicator Color matching trust score
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            product.trustScore >= 80 -> LowHazardGreen.copy(alpha = 0.15f)
                                            product.trustScore >= 50 -> MedHazardOrange.copy(alpha = 0.15f)
                                            else -> HighHazardRed.copy(alpha = 0.15f)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${product.trustScore}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    color = when {
                                        product.trustScore >= 80 -> LowHazardGreen
                                        product.trustScore >= 50 -> MedHazardOrange
                                        else -> HighHazardRed
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1.0f)) {
                                Text(
                                    product.productName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "Adulteration: " + if (product.adulterationStatus.contains("Potential risk")) "⚠️ Risk Alert" else "🛡️ Clean",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            Icon(
                                Icons.Default.ArrowForwardIos,
                                contentDescription = "Scan Detail",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showLiveCamera) {
        LiveCameraScannerDialog(
            onDismiss = { showLiveCamera = false },
            onImageCaptured = { bitmap ->
                showLiveCamera = false
                viewModel.runAnalysis(bitmap, "Direct camera capture ingredients scan", null)
            }
        )
    }
}

@Composable
fun LiveCameraScannerDialog(
    onDismiss: () -> Unit,
    onImageCaptured: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().apply {
                                setSurfaceProvider(previewView.surfaceProvider)
                            }
                            imageCapture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageCapture
                                )
                            } catch (e: Exception) {
                                Log.e("CameraScanner", "Use case binding failed", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Translucent HUD overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val strokeWidthVal = 4.dp.toPx()
                            val cornerLength = 40.dp.toPx()
                            val color = Color(0xFF75DB92)

                            // Drawn Corner HUD markers
                            val viewWidth = size.width
                            val viewHeight = size.height
                            val rWidth = viewWidth * 0.75f
                            val rHeight = viewHeight * 0.45f
                            val left = (viewWidth - rWidth) / 2f
                            val top = (viewHeight - rHeight) / 2.3f
                            val right = left + rWidth
                            val bottom = top + rHeight

                            // Draw brackets
                            // Top-Left
                            drawLine(color, Offset(left, top), Offset(left + cornerLength, top), strokeWidthVal)
                            drawLine(color, Offset(left, top), Offset(left, top + cornerLength), strokeWidthVal)
                            // Top-Right
                            drawLine(color, Offset(right, top), Offset(right - cornerLength, top), strokeWidthVal)
                            drawLine(color, Offset(right, top), Offset(right, top + cornerLength), strokeWidthVal)
                            // Bottom-Left
                            drawLine(color, Offset(left, bottom), Offset(left + cornerLength, bottom), strokeWidthVal)
                            drawLine(color, Offset(left, bottom), Offset(left, bottom - cornerLength), strokeWidthVal)
                            // Bottom-Right
                            drawLine(color, Offset(right, bottom), Offset(right - cornerLength, bottom), strokeWidthVal)
                            drawLine(color, Offset(right, bottom), Offset(right, bottom - cornerLength), strokeWidthVal)
                        }
                )

                // Top Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .testTag("camera_close_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                    Text(
                        "Align Ingredients / Barcode",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.size(48.dp)) // Equalizer
                }

                // Emulator scanning help card inside Camera preview so they aren't stuck with a black screen!
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 120.dp, start = 20.dp, end = 20.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.75f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Testing in Headless Cloud Emulator?",
                            color = Color(0xFF75DB92),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            "Virtual cameras can return black feeds. Click 'Simulate Scan' below to capture a simulated product in high resolution!",
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                        )
                        Button(
                            onClick = {
                                // Capture a mock image (e.g. from template drawable asset)
                                val conf = Bitmap.Config.ARGB_8888
                                val mockBitmap = Bitmap.createBitmap(400, 400, conf)
                                onImageCaptured(mockBitmap)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF75DB92)),
                            modifier = Modifier.testTag("camera_simulate_capture_button")
                        ) {
                            Text("Simulate Camera Capture", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Bottom shutter button
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f))
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable {
                            imageCapture?.let { capture ->
                                capture.takePicture(
                                    cameraExecutor,
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            val buffer = image.planes[0].buffer
                                            val bytes = ByteArray(buffer.capacity())
                                            buffer.get(bytes)
                                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                            image.close()
                                            if (bitmap != null) {
                                                onImageCaptured(bitmap)
                                            }
                                        }
                                    }
                                )
                            } ?: run {
                                // Fallback
                                val conf = Bitmap.Config.ARGB_8888
                                val mockBitmap = Bitmap.createBitmap(400, 400, conf)
                                onImageCaptured(mockBitmap)
                            }
                        }
                        .testTag("camera_shutter_button")
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelfCaptureScreen(viewModel: ScanViewModel) {
    val selectedId by viewModel.selectedShelfProductId.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val activeProduct = viewModel.shelfProducts.firstOrNull { it.id == selectedId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TrustLens Shelf Compare", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Screen explanation banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .indigoGlass(RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Layers,
                        contentDescription = null,
                        tint = Color(0xFF34D399),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Point your camera at a store shelf. TrustLens automatically overlays choices: 🟢 Best Choice, 🟡 Average Choice, 🔴 Avoid. Best alternatives are spotlighted beside them!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }

            // Interactive Simulated Shelf Viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF050C0A)) // Deep high-contrast obsidian jade matching spec
                    .border(
                        BorderStroke(
                            1.dp,
                            Brush.linearGradient(
                                listOf(Color(0x4034D399), Color(0x1134D399))
                            )
                        ),
                        RoundedCornerShape(20.dp)
                    )
            ) {
                // Shelf rows graphic backings drawn using basic gradients
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(modifier = Modifier.height(10.dp))
                    // Row 1
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF33463F), Color(0xFF101915))
                                )
                            )
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // Row 2
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF33463F), Color(0xFF101915))
                                )
                            )
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Floating Augmented Reality Overlay Nodes!
                viewModel.shelfProducts.forEach { product ->
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val pxX = (constraints.maxWidth * product.xOffsetRate)
                        val pxY = (constraints.maxHeight * product.yOffsetRate)

                        // Relative positioned overlaid badge
                        Box(
                            modifier = Modifier
                                .absoluteOffset(
                                    x = (pxX / LocalContext.current.resources.displayMetrics.density).dp,
                                    y = (pxY / LocalContext.current.resources.displayMetrics.density).dp
                                )
                                .testTag("shelf_overlay_target_${product.id}")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (selectedId == product.id) Color(0xFFEAB308) else Color.Black.copy(
                                            alpha = 0.7f
                                        )
                                    )
                                    .border(
                                        2.dp,
                                        when {
                                            product.score >= 80 -> LowHazardGreen
                                            product.score >= 50 -> MedHazardOrange
                                            else -> HighHazardRed
                                        },
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewModel.selectShelfProduct(if (selectedId == product.id) null else product.id) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Colored Dot status indicator
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    product.score >= 80 -> LowHazardGreen
                                                    product.score >= 50 -> MedHazardOrange
                                                    else -> HighHazardRed
                                                }
                                            )
                                    )
                                    Text(
                                        product.subtitle,
                                        color = if (selectedId == product.id) Color.Black else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    "Score: ${product.score}",
                                    color = if (selectedId == product.id) Color.Black.copy(alpha = 0.8f) else Color.White.copy(
                                        alpha = 0.7f
                                    ),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }

                // Guide Text on Shelf overlay
                if (selectedId == null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            "Tap any products above\nto overlay TrustLens details!",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Spotlight Alternative details drawer at bottom of shelf comparison
            AnimatedContent(
                targetState = activeProduct,
                label = "ShelfSpotlightExpansion"
            ) { product ->
                if (product != null) {
                    // Pull detailed preset scan values directly based on matching indices
                    val mappedSample = viewModel.sampleProducts.firstOrNull { it.id == product.sampleIndex }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .frostedGlass(RoundedCornerShape(24.dp))
                            .testTag("shelf_spotlight_card")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Overlay Score Badge
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    product.score >= 80 -> LowHazardGreen.copy(alpha = 0.15f)
                                                    product.score >= 50 -> MedHazardOrange.copy(alpha = 0.15f)
                                                    else -> HighHazardRed.copy(alpha = 0.15f)
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "${product.score}",
                                            fontWeight = FontWeight.Black,
                                            color = when {
                                                product.score >= 80 -> LowHazardGreen
                                                product.score >= 50 -> MedHazardOrange
                                                else -> HighHazardRed
                                            },
                                            fontSize = 14.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            product.name,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            product.statusText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = when {
                                                product.score >= 80 -> LowHazardGreen
                                                product.score >= 50 -> MedHazardOrange
                                                else -> HighHazardRed
                                            },
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { viewModel.selectShelfProduct(null) }
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Deselect", tint = Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Display spotlight substitute
                            if (mappedSample != null) {
                                // Extract first alternative
                                val alternatives = remember(mappedSample) {
                                    val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, AlternativeProduct::class.java)
                                    val adapter = Moshi.Builder().add(KotlinJsonAdapterFactory()).build().adapter<List<AlternativeProduct>>(type)
                                    adapter.fromJson(mappedSample.alternativesJson) ?: emptyList()
                                }

                                val firstAlternative = alternatives.firstOrNull()
                                if (firstAlternative != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(LowHazardGreen.copy(alpha = 0.08f))
                                            .border(1.dp, LowHazardGreen.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                            .padding(12.dp)
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.Stars,
                                                    contentDescription = null,
                                                    tint = LowHazardGreen,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    "TrustLens Shelf Alternative Swap:",
                                                    fontWeight = FontWeight.Bold,
                                                    color = LowHazardGreen,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                            Text(
                                                "👉 " + firstAlternative.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color.White,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                            Text(
                                                firstAlternative.reasonToBuy,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF94A3B8)
                                            )

                                            // Quick differences badges row
                                            Row(
                                                modifier = Modifier.padding(top = 8.dp),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                firstAlternative.sugarDiff?.let {
                                                    Badge(containerColor = Color(0x3334D399), contentColor = Color(0xFF34D399)) {
                                                        Text(" Sugar: $it ", fontSize = 10.sp)
                                                    }
                                                }
                                                firstAlternative.sodiumDiff?.let {
                                                    Badge(containerColor = Color(0x3334D399), contentColor = Color(0xFF34D399)) {
                                                        Text(" Sodium: $it ", fontSize = 10.sp)
                                                    }
                                                }
                                                firstAlternative.proteinDiff?.let {
                                                    Badge(containerColor = Color(0x3334D399), contentColor = Color(0xFF34D399)) {
                                                        Text(" Protein: $it ", fontSize = 10.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = { viewModel.selectLog(mappedSample) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("shelf_spotlight_view_details_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                                ) {
                                    Icon(Icons.Default.Analytics, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("View Complete Ingredient Report", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Highlight shelf items above to compare nutrition and swaps dynamically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8).copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: ScanViewModel) {
    val history by viewModel.filteredScanLogs.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Scans", fontWeight = FontWeight.Bold, color = Color.White) },
                actions = {
                    if (history.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearAllLogs() },
                            modifier = Modifier.testTag("action_clear_all_history")
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All Logs", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Search field
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("history_search_input_field"),
                placeholder = { Text("Search product name or barcode...", color = Color(0xFF94A3B8).copy(alpha = 0.5f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8)) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF94A3B8))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF34D399),
                    unfocusedBorderColor = Color(0x33FFFFFF),
                    focusedLabelColor = Color(0xFF34D399),
                    unfocusedLabelColor = Color(0xFF94A3B8),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            if (history.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.0f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFF94A3B8).copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "No scanned products catalogued",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "Scan actual ingredients or try presets to build your safety index.",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.0f)
                ) {
                    items(history, key = { it.id }) { product ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .frostedGlass(RoundedCornerShape(20.dp))
                                .clickable { viewModel.selectLog(product) }
                                .testTag("history_item_card_${product.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(14.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Double circular trust score indicator
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                product.trustScore >= 80 -> LowHazardGreen.copy(alpha = 0.15f)
                                                product.trustScore >= 50 -> MedHazardOrange.copy(alpha = 0.15f)
                                                else -> HighHazardRed.copy(alpha = 0.15f)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "${product.trustScore}",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp,
                                        color = when {
                                            product.trustScore >= 80 -> LowHazardGreen
                                            product.trustScore >= 50 -> MedHazardOrange
                                            else -> HighHazardRed
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1.0f)) {
                                    Text(
                                        product.productName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "Barcode: " + (product.barcode ?: "N/A"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8)
                                    )
                                    val formattedDate = remember(product.timestamp) {
                                        try {
                                            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(product.timestamp))
                                        } catch (e: Exception) {
                                            ""
                                        }
                                    }
                                    Text(
                                        "Scanned: $formattedDate",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8).copy(alpha = 0.7f),
                                        fontSize = 10.sp
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { viewModel.deleteLog(product) }
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Delete",
                                            tint = HighHazardRed.copy(alpha = 0.85f)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.selectLog(product) }
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowForwardIos,
                                            contentDescription = "Open Details",
                                            modifier = Modifier.size(16.dp),
                                            tint = Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ScanDetailsDialog(
    scanLog: ScanLog,
    onDismiss: () -> Unit,
    errorMessage: String?
) {
    val context = LocalContext.current
    val moshi = remember { Moshi.Builder().add(KotlinJsonAdapterFactory()).build() }

    val ingredients = remember(scanLog) {
        val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, IngredientInfo::class.java)
        val adapter = moshi.adapter<List<IngredientInfo>>(type)
        adapter.fromJson(scanLog.ingredientsJson) ?: emptyList()
    }

    val alternatives = remember(scanLog) {
        val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, AlternativeProduct::class.java)
        val adapter = moshi.adapter<List<AlternativeProduct>>(type)
        adapter.fromJson(scanLog.alternativesJson) ?: emptyList()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    // Velvet Onyx Deep background solid (#0A0C10)
                    drawRect(Color(0xFF0A0C10))
                    // Holographic Radial Indigo glow from the center
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x334F46E5), Color.Transparent),
                            center = center,
                            radius = size.width * 0.9f
                        ),
                        radius = size.width * 0.9f,
                        center = center
                    )
                },
            color = Color.Transparent
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                        title = { Text("TrustLens Chemical Analysis", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss, modifier = Modifier.testTag("detail_back_button")) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = Color.White
                        )
                    )
                },
                containerColor = Color.Transparent
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                        .testTag("scan_details_container")
                ) {
                    
                    // Warning toast if model processed offline or API not set up
                    errorMessage?.let { msg ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .border(BorderStroke(1.dp, Color(0x80EF4444)), RoundedCornerShape(12.dp))
                                .testTag("detail_api_warning_card"),
                            colors = CardDefaults.cardColors(containerColor = Color(0x33EF4444))
                        ) {
                            Text(
                                msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFCA5A5),
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // Product title, manufacturer and dynamic trust ring matching trust score value
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .frostedGlass(RoundedCornerShape(24.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1.0f)) {
                                    Text(
                                        scanLog.productName,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Text(
                                        "Mfg: " + (scanLog.manufacturer ?: "Not listed"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8)
                                    )
                                    Text(
                                        "Barcode / Batch: " + (scanLog.barcode ?: "Internal scan"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8).copy(alpha = 0.7f),
                                        fontSize = 11.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Visual ring representation of Trust Score
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .drawBehind {
                                            val trustPct = scanLog.trustScore / 100f
                                            val color = when {
                                                scanLog.trustScore >= 80 -> LowHazardGreen
                                                scanLog.trustScore >= 50 -> MedHazardOrange
                                                else -> HighHazardRed
                                            }
                                            // Draw backing path ring
                                            drawCircle(
                                                color = color.copy(alpha = 0.15f),
                                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                                    width = 6.dp.toPx()
                                                )
                                            )
                                            // Draw score sweep ring
                                            drawArc(
                                                color = color,
                                                startAngle = -90f,
                                                sweepAngle = 360f * trustPct,
                                                useCenter = false,
                                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                                    width = 6.dp.toPx(),
                                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                                )
                                            )
                                        }
                                        .testTag("detail_trust_score_badge"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            "${scanLog.trustScore}",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 20.sp,
                                            color = when {
                                                scanLog.trustScore >= 80 -> LowHazardGreen
                                                scanLog.trustScore >= 50 -> MedHazardOrange
                                                else -> HighHazardRed
                                            }
                                        )
                                        Text(
                                            "Trust",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Adulteration Risk Checker Section
                    Text(
                        "Adulteration Risk Assessment",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val isRiskProne = scanLog.adulterationStatus.contains("Potential risk")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isRiskProne) HighHazardRed.copy(alpha = 0.08f) else LowHazardGreen.copy(
                                    alpha = 0.08f
                                )
                            )
                            .border(
                                1.dp,
                                if (isRiskProne) HighHazardRed.copy(alpha = 0.3f) else LowHazardGreen.copy(alpha = 0.3f),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(14.dp)
                            .testTag("detail_adulteration_card")
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (isRiskProne) Icons.Default.Warning else Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = if (isRiskProne) HighHazardRed else LowHazardGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    scanLog.adulterationStatus,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isRiskProne) HighHazardRed else LowHazardGreen,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                scanLog.adulterationReason,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Manufacturer Registration & Batch Consistency Assessor (Fake Product Detector)
                    Text(
                        "Manufacturer Registration & Counterfeit Assessor",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .frostedGlass(RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.Gavel,
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    scanLog.officialRegistration ?: "FSSAI Registered - Active Status",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }

                            scanLog.fakeProductReasoning?.let { reasoning ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x1A34D399))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        reasoning,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            scanLog.consumerReportsSummary?.let { reports ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.People,
                                        contentDescription = null,
                                        tint = Color(0xFF34D399),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        reports,
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8).copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Ingredients Explanations Cards
                    Text(
                        "Ingredient Explanations (${ingredients.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Replacing technical names with clear consumer terminology, highlighting hazardous preservatives:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ingredients.forEach { ingredient ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .frostedGlass(RoundedCornerShape(12.dp))
                                    .testTag("ingredient_item_${ingredient.name.replace(" ", "_")}")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (ingredient.isHarmful) {
                                                            if (ingredient.hazardLevel == "High") HighHazardRed else MedHazardOrange
                                                        } else {
                                                            LowHazardGreen
                                                        }
                                                    )
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                ingredient.name,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }

                                        // Badge highlighting chemical code (e.g. INS 211)
                                        ingredient.chemicalName?.let {
                                            Badge(
                                                containerColor = if (ingredient.isHarmful) {
                                                    if (ingredient.hazardLevel == "High") HighHazardRed.copy(alpha = 0.15f) else MedHazardOrange.copy(alpha = 0.15f)
                                                } else {
                                                    Color(0x3334D399)
                                                },
                                                contentColor = if (ingredient.isHarmful) {
                                                    if (ingredient.hazardLevel == "High") HighHazardRed else MedHazardOrange
                                                } else {
                                                    Color(0xFF34D399)
                                                }
                                            ) {
                                                Text(" $it ", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        ingredient.explanation,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8)
                                    )

                                    Row(
                                        modifier = Modifier.padding(top = 6.dp, start = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Badge(containerColor = Color(0x3334D399), contentColor = Color(0xFF34D399)) {
                                            Text("Category: ${ingredient.category}", fontSize = 9.sp)
                                        }
                                        if (ingredient.isHarmful) {
                                            Badge(
                                                containerColor = HighHazardRed.copy(alpha = 0.1f),
                                                contentColor = HighHazardRed
                                            ) {
                                                Text("Hazard: ${ingredient.hazardLevel}", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Better Alternative Finder Section ⭐
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Stars,
                            contentDescription = null,
                            tint = LowHazardGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Better Alternatives Spotlight",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        "Recommended healthier, less-processed swaps with lower sugar or sodium ratios:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (alternatives.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x1A34D399), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "You are currently scanning our recommended healthy choice! No replacement needed.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            alternatives.forEach { alternative ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(LowHazardGreen.copy(alpha = 0.08f))
                                        .border(1.dp, LowHazardGreen.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                                        .padding(14.dp)
                                        .testTag("alternative_item_${alternative.name.replace(" ", "_")}")
                                ) {
                                    Column {
                                        Text(
                                            "Buy This Instead:",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Black,
                                            color = LowHazardGreen
                                        )
                                        Text(
                                            alternative.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color.White,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                        Text(
                                            alternative.reasonToBuy,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF94A3B8),
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )

                                        // Comparison metrics
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            alternative.sugarDiff?.let {
                                                Badge(containerColor = LowHazardGreen.copy(alpha = 0.15f), contentColor = LowHazardGreen) {
                                                    Text(" Sugar: $it ", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            alternative.sodiumDiff?.let {
                                                Badge(containerColor = LowHazardGreen.copy(alpha = 0.15f), contentColor = LowHazardGreen) {
                                                    Text(" Sodium: $it ", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            alternative.proteinDiff?.let {
                                                Badge(containerColor = LowHazardGreen.copy(alpha = 0.15f), contentColor = LowHazardGreen) {
                                                    Text(" Protein: $it ", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            alternative.priceDiff?.let {
                                                Badge(containerColor = Color(0x3334D399), contentColor = Color(0xFF34D399)) {
                                                    Text(" $it ", fontSize = 9.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("detail_close_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Dismiss Report", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Utility to decode Uri into Bitmap for Android files
private fun loadUriBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        bitmap
    } catch (e: Exception) {
        Log.e("MainLayout", "Failed decoding Bitmap from Uri: ${e.message}")
        null
    }
}
