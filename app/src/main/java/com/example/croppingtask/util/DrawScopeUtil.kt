package com.example.croppingtask.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import com.example.croppingtask.model.Corner

fun DrawScope.drawCropRectangle(
    topLeft: Offset,
    topRight: Offset,
    bottomLeft: Offset,
    bottomRight: Offset,
    dimAnimatedColor: Color,
    isCustomRation: Boolean,
    isDragging: Boolean,
) {
    val cropPath = Path().apply {
        addRect(Rect(topLeft, bottomRight))
    }

    clipPath(cropPath, clipOp = ClipOp.Difference) {
        drawRect(dimAnimatedColor)
    }

    if (isCustomRation) {
        if (isDragging) {
            drawGrid(
                topLeft,
                topRight,
                bottomLeft,
                bottomRight,
            )
        }

        drawCorner(topLeft, Corner.TopLeft)
        drawCorner(topRight, Corner.TopRight)
        drawCorner(bottomLeft, Corner.BottomLeft)
        drawCorner(bottomRight, Corner.BottomRight)
    }
}

fun DrawScope.drawGrid(topLeft: Offset, topRight: Offset, bottomLeft: Offset, bottomRight: Offset) {
    val path = Path().apply {
        moveTo(topLeft.x, topLeft.y)
        lineTo(topRight.x, topRight.y)
        lineTo(bottomRight.x, bottomRight.y)
        lineTo(bottomLeft.x, bottomLeft.y)
        close()

        val vertical1 = Offset(
            x = (topLeft.x + bottomLeft.x) / 2,
            y = (topLeft.y + bottomLeft.y) / 2
        )
        val vertical2 = Offset(
            x = (topRight.x + bottomRight.x) / 2,
            y = (topRight.y + bottomRight.y) / 2
        )
        val horizontal1 = Offset(
            x = (topLeft.x + topRight.x) / 2,
            y = (topLeft.y + topRight.y) / 2
        )
        val horizontal2 = Offset(
            x = (bottomLeft.x + bottomRight.x) / 2,
            y = (bottomLeft.y + bottomRight.y) / 2
        )

        moveTo(vertical1.x, vertical1.y)
        lineTo(vertical2.x, vertical2.y)

        moveTo(horizontal1.x, horizontal1.y)
        lineTo(horizontal2.x, horizontal2.y)
    }

    drawPath(
        path = path,
        color = Color.White.copy(alpha = .8f),
        style = Stroke(width = 2f, cap = StrokeCap.Round)
    )
}

fun DrawScope.drawCorner(center: Offset, corner: Corner) {
    val lineLength = 75f
    val strokeWidth = 14f
    val path = Path()

    val (verticalLine, horizontalLine) = when (corner) {
        Corner.TopLeft -> {
            Pair(center.copy(y = center.y + lineLength), center.copy(x = center.x + lineLength))
        }

        Corner.TopRight -> {
            Pair(center.copy(y = center.y + lineLength), center.copy(x = center.x - lineLength))
        }

        Corner.BottomLeft -> {
            Pair(center.copy(y = center.y - lineLength), center.copy(x = center.x + lineLength))
        }

        Corner.BottomRight -> {
            Pair(center.copy(y = center.y - lineLength), center.copy(x = center.x - lineLength))
        }
    }

    path.apply {
        // Move to the starting point (center)
        moveTo(center.x, center.y)
        // Draw vertical line
        lineTo(verticalLine.x, verticalLine.y)
        // Move back to center
        moveTo(center.x, center.y)
        // Draw horizontal line
        lineTo(horizontalLine.x, horizontalLine.y)
    }

    // Draw the path
    drawPath(
        path = path,
        color = Color(0xFF999DE3),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )

}