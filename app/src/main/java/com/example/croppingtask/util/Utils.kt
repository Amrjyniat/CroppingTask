package com.example.croppingtask.util

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.pointer.PointerEvent
import com.example.croppingtask.CropCorners
import com.example.croppingtask.model.Corner
import kotlin.math.min
import kotlin.math.roundToInt

fun calculatePanAndZoom(event: PointerEvent): Pair<Offset, Float> {
    val zoom = event.calculateZoom()
    val pan = event.calculatePan()
    return Pair(pan, zoom)
}

fun calculateCropCorners(
    draggingCorner: Corner?,
    cropCorners: CropCorners,
    dragAmount: Offset,
    imageWidth: Int,
    imageHeight: Int,
    draggingCenter: Boolean,
    minCropSize: Float = 100f,
) = when (draggingCorner) {
    Corner.TopLeft -> {
        val newTopLeft = cropCorners.topLeft + dragAmount
        cropCorners.copy(
            topLeft = Offset(
                x = newTopLeft.x.coerceIn(
                    0f,
                    cropCorners.bottomRight.x - minCropSize
                ),
                y = newTopLeft.y.coerceIn(
                    0f,
                    cropCorners.bottomRight.y - minCropSize
                )
            ),
            topRight = cropCorners.topRight.copy(
                y = newTopLeft.y
            ),
            bottomLeft = cropCorners.bottomLeft.copy(
                x = newTopLeft.x
            )
        )
    }

    Corner.TopRight -> {
        val newTopRight = cropCorners.topRight + dragAmount
        cropCorners.copy(
            topRight = Offset(
                x = newTopRight.x.coerceIn(
                    cropCorners.topLeft.x + minCropSize,
                    imageWidth.toFloat()
                ),
                y = newTopRight.y.coerceIn(
                    0f,
                    cropCorners.bottomLeft.y - minCropSize
                )
            ),
            topLeft = cropCorners.topLeft.copy(y = newTopRight.y),
            bottomRight = cropCorners.bottomRight.copy(
                x = newTopRight.x
            )
        )
    }

    Corner.BottomLeft -> {
        val newBottomLeft = cropCorners.bottomLeft + dragAmount
        cropCorners.copy(
            bottomLeft = Offset(
                x = newBottomLeft.x.coerceIn(
                    0f,
                    cropCorners.bottomRight.x - minCropSize
                ),
                y = newBottomLeft.y.coerceIn(
                    cropCorners.topLeft.y + minCropSize,
                    imageHeight.toFloat()
                )
            ),
            topLeft = cropCorners.topLeft.copy(x = newBottomLeft.x),
            bottomRight = cropCorners.bottomRight.copy(
                y = newBottomLeft.y
            )
        )
    }

    Corner.BottomRight -> {
        val newBottomRight = cropCorners.bottomRight + dragAmount
        cropCorners.copy(
            bottomRight = Offset(
                x = newBottomRight.x.coerceIn(
                    cropCorners.bottomLeft.x + minCropSize,
                    imageWidth.toFloat()
                ),
                y = newBottomRight.y.coerceIn(
                    cropCorners.topRight.y + minCropSize,
                    imageHeight.toFloat()
                )
            ),
            topRight = cropCorners.topRight.copy(
                x = newBottomRight.x
            ),
            bottomLeft = cropCorners.bottomLeft.copy(
                y = newBottomRight.y
            )
        )
    }

    null -> if (draggingCenter) {
        val newTopLeft = cropCorners.topLeft + dragAmount
        val newBottomRight = cropCorners.bottomRight + dragAmount

        if (newTopLeft.x >= 0 && newBottomRight.x <= imageWidth &&
            newTopLeft.y >= 0 && newBottomRight.y <= imageHeight
        ) {
            cropCorners.copy(
                topLeft = newTopLeft,
                topRight = cropCorners.topRight + dragAmount,
                bottomLeft = cropCorners.bottomLeft + dragAmount,
                bottomRight = newBottomRight
            )
        } else {
            cropCorners
        }
    } else cropCorners
}

// Check if the touch is near a specific point
fun Offset.isNear(point: Offset, threshold: Float = 80f): Boolean {
    return (this - point).getDistance() <= threshold
}

fun getCroppedBitmap(
    imageBitmap: ImageBitmap,
    cropRect: Rect,
    canvasWidth: Float,
    canvasHeight: Float,
    scale: Float,
    offset: Offset,
): Bitmap {
    val bitmapWidth = imageBitmap.width.toFloat()
    val bitmapHeight = imageBitmap.height.toFloat()

    val widthRatio = (canvasWidth / bitmapWidth)
    val heightRatio = (canvasHeight / bitmapHeight)

    val scaleFactor = min(widthRatio, heightRatio)

    val cropLeft =
        ((cropRect.left) / scaleFactor).roundToInt().coerceIn(0, bitmapWidth.toInt())
    val cropTop =
        ((cropRect.top) / scaleFactor).roundToInt().coerceIn(0, bitmapHeight.toInt())
    Log.i("ImageCropper", "cropLeft: $cropLeft , cropTop: $cropTop")
    val cropRight =
        ((cropRect.right) / scaleFactor).roundToInt().coerceIn(0, bitmapWidth.toInt())
    val cropBottom =
        ((cropRect.bottom) / scaleFactor).roundToInt().coerceIn(0, bitmapHeight.toInt())

    val cropWidth = (cropRight - cropLeft).coerceAtLeast(1)
    val cropHeight = (cropBottom - cropTop).coerceAtLeast(1)

    return Bitmap.createBitmap(
        imageBitmap.asAndroidBitmap(), cropLeft, cropTop, cropWidth, cropHeight
    )
}