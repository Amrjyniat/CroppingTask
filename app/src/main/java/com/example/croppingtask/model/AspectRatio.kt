package com.example.croppingtask.model

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import com.example.croppingtask.CropCorners

enum class AspectRatio(val label: String, val cropCorners: (Size) -> CropCorners) {
    Custom("Custom", { size ->
        val padding = 50f
        CropCorners(
            Offset(padding, padding),
            Offset(size.width - padding, padding),
            Offset(padding, size.height - padding),
            Offset(size.width - padding, size.height - padding)
        )
    }),
    OneToOne("1:1", { size ->
        val side = minOf(size.width, size.height) * 0.8f
        val left = (size.width - side) / 2
        val top = (size.height - side) / 2
        CropCorners(
            Offset(left, top),
            Offset(left + side, top),
            Offset(left, top + side),
            Offset(left + side, top + side)
        )
    }),
    ThreeToTwo("3:2", { size ->
        val width = size.width * 0.8f
        val height = width * 2 / 3
        val left = (size.width - width) / 2
        val top = (size.height - height) / 2
        CropCorners(
            Offset(left, top),
            Offset(left + width, top),
            Offset(left, top + height),
            Offset(left + width, top + height)
        )
    }),
    TowToThree("2:3", { size ->
        val height = size.height * 0.8f
        val width = height * 2 / 3
        val left = (size.width - width) / 2
        val top = (size.height - height) / 2
        CropCorners(
            Offset(left, top),
            Offset(left + width, top),
            Offset(left, top + height),
            Offset(left + width, top + height)
        )
    })
}