package com.meshverse.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.meshverse.app.media.MediaTransferManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MediaTransferManagerViewModel @Inject constructor(
    val manager: MediaTransferManager
) : ViewModel()
