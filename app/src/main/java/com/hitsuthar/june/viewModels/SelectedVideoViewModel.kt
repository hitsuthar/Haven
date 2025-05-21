package com.hitsuthar.june.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hitsuthar.june.screens.DDLStream
import com.hitsuthar.june.utils.torrentProviders.TorrentStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class Stream {
    data class Torrent(val torrentStream: TorrentStream) : Stream()
    data class DDL(val ddlStream: DDLStream) : Stream()
    object Error : Stream()
}

class SelectedVideoViewModel : ViewModel() {

    private val _selectedVideo = MutableStateFlow<Stream?>(null)

    val selectedVideo: StateFlow<Stream?> = _selectedVideo.asStateFlow()

    fun setSelectedVideo(video: Stream) {
        viewModelScope.launch {
            _selectedVideo.update {
                when (video) {
                    is Stream.Torrent -> Stream.Torrent(video.torrentStream)
                    is Stream.DDL -> Stream.DDL(video.ddlStream)
                    else -> Stream.Error
                }
            }
        }
    }
}