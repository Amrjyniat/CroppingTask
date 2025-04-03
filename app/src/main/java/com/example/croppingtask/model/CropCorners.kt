package com.example.croppingtask.model

import androidx.compose.ui.geometry.Offset

data class CropCorners(
    val topLeft: Offset,
    val topRight: Offset,
    val bottomLeft: Offset,
    val bottomRight: Offset,
)