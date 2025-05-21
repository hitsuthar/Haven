package com.hitsuthar.june.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WatchPartyViewModel : ViewModel() {
    private val database = FirebaseDatabase.getInstance()

    private val _partyRefFlow = MutableStateFlow<DatabaseReference?>(null)
    val partyRefFlow: StateFlow<DatabaseReference?> = _partyRefFlow.asStateFlow()

    private val _currentTime = MutableStateFlow(0L)
    val currentTime: StateFlow<Long> = _currentTime.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var currentTimeListener: ValueEventListener? = null
    private var isPlayingListener: ValueEventListener? = null


    private fun startListeningForUpdates() {
        Log.d("WatchPartyViewModel", "startListeningForUpdates: called")
        val partyRef = _partyRefFlow.value ?: run {
            Log.d(
                "WatchPartyViewModel",
                "partyRef is null, skipping startListeningForUpdates"
            )
            return
        }

//        val partyRef = database.getReference("watch_parties").child(partyID)
        val currentTimeRef = partyRef.child("currentTime")
        val isPlayingRef = partyRef.child("isPlaying")


        currentTimeListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.getValue(Long::class.java)?.let { time ->
                    _currentTime.value = time
                }
            }

            override fun onCancelled(error: DatabaseError) {
                println("Error listening for currentTime updates: ${error.message}")
            }
        }.also { currentTimeRef.addValueEventListener(it) }

        isPlayingListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.getValue(Boolean::class.java)?.let { playing ->
                    _isPlaying.value = playing
                }
            }

            override fun onCancelled(error: DatabaseError) {
                println("Error listening for isPlaying updates: ${error.message}")
            }
        }.also { isPlayingRef.addValueEventListener(it) }

    }

    fun createParty(userID: String, partyID: String) {
        val newPartyRef = database.getReference("watch_parties").child(partyID)
        _partyRefFlow.value = newPartyRef
        val partyData = mapOf(
            "host" to userID,
            "timestamp" to ServerValue.TIMESTAMP,
            "currentTime" to 0L,
            "isPlaying" to false,
        )
        newPartyRef.setValue(partyData)
            .addOnSuccessListener { startListeningForUpdates() }
            .addOnFailureListener { e ->
                println("Error creating party: ${e.message}")
            }
    }

    fun updatePlayback(currentTime: Long, isPlaying: Boolean, partyID: String) {
        val partyRef = _partyRefFlow.value
        if (partyRef == null) {
            Log.d("WatchPartyViewModel", "partyRef is null, cannot update playback")
            return
        }
        partyRef.let { ref ->
            val updateData = mapOf(
                "currentTime" to currentTime,
                "isPlaying" to isPlaying,
                "timestamp" to ServerValue.TIMESTAMP
            )
            ref.updateChildren(updateData).addOnFailureListener { e ->
                println("Error updating playback: ${e.message}")
            }
        }
    }
    fun updateVideoUrl(videoUrl: String) {
        val partyRef = _partyRefFlow.value
        if (partyRef == null) {
            Log.d("WatchPartyViewModel", "partyRef is null, cannot update videoUrl")
            return
        }
        partyRef.let { ref ->
            val updateData = mapOf(
                "videoUrl" to videoUrl,
                "timestamp" to ServerValue.TIMESTAMP
            )
            ref.updateChildren(updateData).addOnFailureListener { e ->
                println("Error updating playback: ${e.message}")
            }
        }
    }
}
