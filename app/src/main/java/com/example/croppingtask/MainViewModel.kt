package com.example.croppingtask

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainViewModel : ViewModel() {

    private val _originalImageUri = MutableStateFlow<Uri?>(null)
    val originalImageUri: StateFlow<Uri?> = _originalImageUri
    fun setImageUri(uri: Uri) {
        _originalImageUri.value = uri
    }

    private val _isCropping = MutableStateFlow(false)
    val isCropping: StateFlow<Boolean> = _isCropping
    fun startCropping() {
        _isCropping.value = true
    }

    fun stopCropping() {
        _isCropping.value = false
    }


    private val _finishCropping = MutableStateFlow(false)
    val finishCropping: StateFlow<Boolean> = _finishCropping
    fun finishCropping(state: Boolean) {
        _finishCropping.value = state
    }

}