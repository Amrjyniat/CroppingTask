package com.example.croppingtask

import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.example.croppingtask.model.AspectRatio
import com.example.croppingtask.model.Corner
import com.example.croppingtask.util.calculateCropCorners
import com.example.croppingtask.util.calculatePanAndZoom
import com.example.croppingtask.util.drawCropRectangle
import com.example.croppingtask.util.getCroppedBitmap
import com.example.croppingtask.util.isNear
import com.example.croppingtask.widget.CropActions
import com.example.croppingtask.widget.ImageActionButtons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageCroppingScreen(mainViewModel: MainViewModel, paddingValues: PaddingValues) {

    val ctx = LocalContext.current
    val density = LocalDensity.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            mainViewModel.setImageUri(uri)
        } ?: Toast.makeText(ctx, "No image selected", Toast.LENGTH_SHORT).show()
    }
    val sheetState = remember {
        SheetState(
            skipPartiallyExpanded = true,
            initialValue = SheetValue.Expanded,
            density = density,
            confirmValueChange = {
                it == SheetValue.Expanded
            }
        )
    }

    val imageUri by mainViewModel.originalImageUri.collectAsState()
    val isImagePicked by remember { derivedStateOf { imageUri != null } }
    val isCropping by mainViewModel.isCropping.collectAsState()
    val finishCropping by mainViewModel.finishCropping.collectAsState()
    var selectedRatio by remember { mutableStateOf(AspectRatio.Custom) }

    val configuration = LocalConfiguration.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.roundToPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
    ) {
        BottomSheetScaffold(
            scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState),
            sheetPeekHeight = BottomSheetDefaults.SheetPeekHeight,
            content = {
                imageUri?.let { uri ->
                    var imageSize by remember { mutableStateOf(Size.Zero) }
                    var canvasSize by remember { mutableStateOf(Size.Zero) }
                    val imageBitmap by remember(imageUri) {
                        mutableStateOf(
                            if (Build.VERSION.SDK_INT < 28) {
                                MediaStore.Images.Media.getBitmap(ctx.contentResolver, uri)
                                    .asImageBitmap()
                            } else {
                                val source = ImageDecoder.createSource(ctx.contentResolver, uri)
                                ImageDecoder.decodeBitmap(source).asImageBitmap()
                            }
                        )
                    }

                    val corners by remember(selectedRatio) {
                        derivedStateOf {
                            selectedRatio.cropCorners(imageSize)
                        }
                    }
                    var draggingCorner by remember { mutableStateOf<Corner?>(null) }
                    var draggingCenter by remember { mutableStateOf(false) }
                    var isDragging by remember { mutableStateOf(false) }
                    LaunchedEffect(isDragging) {
                        if (isDragging && imageSize.height >= (screenHeightPx * 0.8)) {
                            sheetState.hide()
                        } else {
                            sheetState.show()
                        }
                    }
                    var cropCorners by remember { mutableStateOf(corners) }
                    LaunchedEffect(corners) {
                        cropCorners = corners
                    }

                    var scale by remember { mutableFloatStateOf(1f) }
                    var offset by remember { mutableStateOf(Offset.Zero) }

                    val dimAnimatedColor by animateColorAsState(
                        targetValue = if (isDragging) Color.Transparent else Color.Black.copy(alpha = 0.8f),
                        animationSpec = tween(durationMillis = 100)
                    )

                    Box(
                        modifier = Modifier
                            .pointerInput(Unit) {
                                var isTransforming = false
                                awaitEachGesture {
                                    var firstEvent = true

                                    val firstDown = awaitFirstDown(requireUnconsumed = false)
                                    val startPosition = firstDown.position

                                    // Check if we're interacting with crop controls before deciding gesture type
                                    val isCropShapeDragging =
                                        if (isCropping && selectedRatio == AspectRatio.Custom) {
                                            startPosition.isNear(cropCorners.topLeft) ||
                                                    startPosition.isNear(cropCorners.topRight) ||
                                                    startPosition.isNear(cropCorners.bottomLeft) ||
                                                    startPosition.isNear(cropCorners.bottomRight) ||
                                                    Rect(
                                                        cropCorners.topLeft,
                                                        cropCorners.bottomRight
                                                    ).contains(
                                                        startPosition
                                                    )
                                        } else false

                                    try {
                                        while (currentEvent.changes.any { it.pressed }) {
                                            val event = awaitPointerEvent()

                                            if (firstEvent) {
                                                firstEvent = false

                                                // Multi-touch -> transform gesture
                                                if (event.changes.size > 1 || (scale > 1f && !isCropShapeDragging)) {
                                                    isTransforming = true
                                                }
                                                // Crop control -> crop gesture
                                                else if (isCropShapeDragging) {
                                                    draggingCorner = when {
                                                        startPosition.isNear(cropCorners.topLeft) -> Corner.TopLeft
                                                        startPosition.isNear(cropCorners.topRight) -> Corner.TopRight
                                                        startPosition.isNear(cropCorners.bottomLeft) -> Corner.BottomLeft
                                                        startPosition.isNear(cropCorners.bottomRight) -> Corner.BottomRight
                                                        else -> null
                                                    }
                                                    draggingCenter = draggingCorner == null &&
                                                            Rect(
                                                                cropCorners.topLeft,
                                                                cropCorners.bottomRight
                                                            ).contains(startPosition)
                                                }
                                                continue
                                            }

                                            // Handle multi-touch transformation (zoom & pan)
                                            if (isTransforming) {
                                                val (panChange, zoomChange) = calculatePanAndZoom(
                                                    event
                                                )

                                                // Apply zoom and pan changes
                                                scale = (scale * zoomChange).coerceIn(1f, 5f)
                                                val extraWidth = (scale - 1) * imageSize.width
                                                val extraHeight = (scale - 1) * imageSize.height

                                                val maxX = extraWidth / 2
                                                val maxY = extraHeight / 2

                                                offset = Offset(
                                                    x = (offset.x + scale * panChange.x).coerceIn(
                                                        -maxX,
                                                        maxX
                                                    ),
                                                    y = (offset.y + scale * panChange.y).coerceIn(
                                                        -maxY,
                                                        maxY
                                                    ),
                                                )
                                            }
                                            // Handle crop adjustment
                                            else if (isCropping && selectedRatio == AspectRatio.Custom) {
                                                isDragging = true

                                                val change = event.changes.first()
                                                val dragAmount = change.positionChange()

                                                cropCorners = calculateCropCorners(
                                                    draggingCorner = draggingCorner,
                                                    cropCorners = cropCorners,
                                                    dragAmount = dragAmount,
                                                    imageWidth = size.width,
                                                    imageHeight = size.height,
                                                    draggingCenter = draggingCenter
                                                )
                                            }
                                        }
                                    } finally {
                                        if (isDragging) {
                                            isDragging = false
                                            draggingCorner = null
                                            draggingCenter = false
                                        }
                                        isTransforming = false
                                    }
                                }
                            }
                    ) {
                        if (!finishCropping) {
                            Image(
                                bitmap = imageBitmap,
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onSizeChanged {
                                        imageSize = it.toSize()
                                    }
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offset.x,
                                        translationY = offset.y
                                    )
                            )

                            if (isCropping) {
                                Canvas(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .onSizeChanged {
                                            canvasSize = it.toSize()
                                        }
                                ) {
                                    drawCropRectangle(
                                        topLeft = cropCorners.topLeft,
                                        topRight = cropCorners.topRight,
                                        bottomLeft = cropCorners.bottomLeft,
                                        bottomRight = cropCorners.bottomRight,
                                        dimAnimatedColor = dimAnimatedColor,
                                        isCustomRation = selectedRatio == AspectRatio.Custom,
                                        isDragging = isDragging
                                    )
                                }
                            }
                        } else {
                            val croppedBitmap = getCroppedBitmap(
                                imageBitmap = imageBitmap,
                                cropRect = Rect(
                                    cropCorners.topLeft,
                                    cropCorners.bottomRight
                                ),
                                canvasWidth = canvasSize.width,
                                canvasHeight = canvasSize.height,
                                scale = scale,
                                offset = offset
                            )

                            Image(
                                bitmap = croppedBitmap.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.aspectRatio(1f)
                            )
                        }
                    }
                }
            },
            sheetContent = {
                if (isCropping) {
                    CropActions(
                        selectedRatio = selectedRatio,
                        onCrop = {
                            mainViewModel.finishCropping(true)
                        },
                        onChangeRatio = { newRatio ->
                            selectedRatio = newRatio
                        },
                        onCloseCropping = {
                            if (finishCropping) {
                                mainViewModel.finishCropping(false)
                            } else {
                                mainViewModel.stopCropping()
                            }
                        }
                    )
                } else {
                    ImageActionButtons(
                        isImagePicked = isImagePicked,
                        onPickImage = {
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(
                                    mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        onStartCrop = {
                            mainViewModel.startCropping()
                        }
                    )
                }
            }
        )
    }
}