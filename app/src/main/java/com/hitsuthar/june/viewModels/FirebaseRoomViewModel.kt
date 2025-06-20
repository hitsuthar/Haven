import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.hitsuthar.june.utils.SimpleIdGenerator
import com.hitsuthar.june.viewModels.VideoPlayerViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

class MovieSyncViewModel(videoPlayerViewModel: VideoPlayerViewModel) : ViewModel() {


  // Firestore instances
  private val firestore = Firebase.firestore
  private val usersCollection = firestore.collection("users")
  private val roomsCollection = firestore.collection("rooms")

  // Realtime Database instances
  private val realtimeDb = Firebase.database
  private val syncRef = realtimeDb.getReference("sync")

  // Current state
  private val _currentRoom = MutableStateFlow<Room?>(null)
  val currentRoom = _currentRoom.asStateFlow()

  private val _currentMovie = MutableStateFlow<Movie?>(null)
  val currentMovie = _currentMovie.asStateFlow()

  private val _syncState = MutableStateFlow<SyncState?>(null)
  val syncState = _syncState.asStateFlow()

  private val _userSyncState = MutableStateFlow<List<UserSyncState>>(emptyList())
  val userSyncState = _userSyncState.asStateFlow()

  private val _myRooms = MutableStateFlow(emptyList<Room>())
  val myRooms = _myRooms.asStateFlow()

  private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
  val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()


  private var roomListener: ListenerRegistration? = null
  private var syncListener: ValueEventListener? = null
  private var messagesListener: ListenerRegistration? = null
  private var userPresenceListener: ValueEventListener? = null


  fun checkAlreadyJoinedRoom(userID: String) {
    viewModelScope.launch {
      try {
        val querySnapshot =
          roomsCollection.whereEqualTo(FieldPath.of("participants", userID, "userID"), userID)
            .limit(1).get().await()
        val room = querySnapshot.documents[0].toObject(Room::class.java)
        _currentRoom.value = room
        if (room != null) {
          startSyncListener(room.id)
          listenToRoom(room.id)
          listenToUserPresence(room.id)
        }

      } catch (e: Exception) {
        Log.e("MovieSync", "Error checking rooms", e)
      }
    }
  }

  // Create a new room
  fun createRoom(roomName: String, userID: String, userName: String) {
    viewModelScope.launch {
      try {
        val user = Participant(
          userID = userID, userName = userName, isHost = true
        )

        // Create in Firestore
        val room = Room(
          name = roomName,
          hostId = userID,
          participants = mapOf(userID to user),
          roomCode = SimpleIdGenerator().generateRandomId()
        )

        val document = roomsCollection.add(room.toMap()).await()
        document.update("id", document.id).await()

        // Initialize sync in Realtime DB
        val syncData = hashMapOf(
          "currentTime" to 0L, "playbackState" to "paused", "lastUpdated" to ServerValue.TIMESTAMP
        )

        syncRef.child(document.id).updateChildren(syncData).await()

        // Set current room
        _currentRoom.value = room.copy(id = document.id)
        if (currentRoom.value != null) {
          listenToRoom(document.id)
          startSyncListener(document.id)
          listenToUserPresence(document.id)
        }


      } catch (e: Exception) {
        Log.e("MovieSync", "Error creating room", e)
      }
    }
  }

  // Join an existing room
  fun joinRoom(roomCode: String, userID: String, userName: String) {
    viewModelScope.launch {
      try {
        val querySnapshot =
          roomsCollection.whereEqualTo("roomCode", roomCode).limit(1).get().await()

        if (!querySnapshot.isEmpty) {
//          Log.d("FirebaseRoomViewModel", querySnapshot.documents.toString())
          val roomDoc = querySnapshot.documents[0]
          val room = roomDoc.toObject(Room::class.java)

          val newParticipant = Participant(
            userID = userID, userName = userName, isHost = false
          )

          if (room != null) {
            roomDoc.reference.update("participants", room.participants + (userID to newParticipant))
              .await()
            _currentRoom.value = room
            if (currentRoom.value != null) {
              listenToRoom(room.id)
              startSyncListener(room.id)
              listenToUserPresence(room.id)
            }
          }

        } else {
          Log.d("FirebaseRoomViewModel", "No matching room found")
        }
      } catch (e: Exception) {
        Log.e("MovieSync", "Error joining room", e)
      }
    }
  }

  fun leaveRoom(roomId: String, userID: String) {
    viewModelScope.launch {
      try {
        val roomDoc = roomsCollection.document(roomId).get().await()
        val room = roomDoc.toObject(Room::class.java)
        if (room != null) {
          val updatedParticipants = room.participants.filter { it.key != userID }
          roomDoc.reference.update("participants", updatedParticipants).await()
        }
        _currentRoom.value = null
        onCleared()
      } catch (e: Exception) {
        Log.e("MovieSync", "Error leaving room", e)
      }
    }
  }

  // Start listening to sync updates
  private fun startSyncListener(roomId: String) {
    syncListener?.let { syncRef.child(roomId).removeEventListener(it) }

    syncListener = object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        _syncState.value = snapshot.getValue(SyncState::class.java)
      }

      override fun onCancelled(error: DatabaseError) {
        Log.e("MovieSync", "Sync listener cancelled", error.toException())
      }
    }
    syncRef.child(roomId).addValueEventListener(syncListener!!)
  }

  private fun listenToRoom(roomId: String) {
    roomListener = roomsCollection.document(roomId).addSnapshotListener { snapshot, error ->
      if (error != null) {
        _currentRoom.value = null
        Log.e("MovieSync", "Error listening to room", error)
        return@addSnapshotListener
      }
      val room = snapshot?.toObject(Room::class.java)
      _currentRoom.value = room
      _currentMovie.value = room?.currentMovie
      Log.d("FirebaseROomViewmodel", "currentMovie: ${currentMovie.value}")
    }

    messagesListener?.remove()
    messagesListener = roomsCollection.document(roomId).collection("chat")
      .orderBy("timestamp", Query.Direction.ASCENDING).addSnapshotListener { snapshot, error ->
        error?.let {
          Log.e("Chat", "Listen failed", it)
          return@addSnapshotListener
        }
        snapshot?.let { querySnapshot ->
          val messages = querySnapshot.documents.mapNotNull { doc ->
            doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
          }
          _messages.value = messages.reversed()
        }
      }
  }

  fun updateCurrentMovie(movie: Movie) {
    viewModelScope.launch {
      val roomId = currentRoom.value?.id
      if (roomId != null) {
        val roomDoc = roomsCollection.document(roomId).get().await()
        val room = roomDoc.toObject(Room::class.java)
        if (room != null) {
          roomDoc.reference.update("currentMovie", movie.toMap()).await()
          listenToRoom(room.id)
          startSyncListener(room.id)
        }
      }
    }
  }


  // Update playback state (called from player)
  fun updatePlaybackState(currentTime: Long? = null, isPlaying: Boolean? = null) {
    val roomId = currentRoom.value?.id ?: return
    val updates = hashMapOf<String, Any>()
    // Only add playbackState if isPlaying is not null
    isPlaying?.let {
      updates["playbackState"] = if (it) "playing" else "paused"
    }
    currentTime?.let {
      updates["currentTime"] = it
    }
    syncRef.child(roomId).updateChildren(updates)
  }

  // Update user presence
  fun setBufferingState(isBuffering: Boolean, userID: String) {
    val roomId = currentRoom.value?.id ?: return

    val userPresence = hashMapOf<String, Any>(
      "buffering" to isBuffering, "lastSeen" to ServerValue.TIMESTAMP, "userID" to userID
    )

    syncRef.child("$roomId/users/$userID").updateChildren(userPresence)
  }


  private fun listenToUserPresence(roomId: String) {
    userPresenceListener?.let { syncRef.child("$roomId/users").removeEventListener(it) }
    userPresenceListener = object : ValueEventListener {
      override fun onDataChange(snapshot: DataSnapshot) {
        _userSyncState.value =
          snapshot.children.mapNotNull { it.getValue(UserSyncState::class.java) }
            .filter { it.buffering }
      }

      override fun onCancelled(error: DatabaseError) {
        Log.e("MovieSync", "User presence listener cancelled", error.toException())
      }
    }
    syncRef.child("$roomId/users").addValueEventListener(userPresenceListener!!)
  }


  fun getMyRooms(userID: String) {
    viewModelScope.launch {
      try {
        val querySnapshot = roomsCollection.whereEqualTo("hostId", userID).get().await()
        val rooms = querySnapshot.documents.mapNotNull { it.toObject(Room::class.java) }

//        Log.d("FirebaseRoomViewModel", "My Rooms: $rooms")
        _myRooms.value = rooms

      } catch (e: Exception) {
        Log.e("MovieSync", "Error checking rooms", e)
      }
    }
  }

  fun sendMessage(roomId: String, text: String, userID: String, userName: String) {
    val message = ChatMessage(
      text = text, senderId = userID, senderName = userName, timestamp = Date()
    )

    roomsCollection.document(roomId).collection("chat").add(message).addOnFailureListener { e ->
      Log.e("Chat", "Error sending message", e)
    }
  }

//  fun listenForMessages(roomId: String) {
//    messagesListener?.remove()
//    messagesListener = roomsCollection.document(roomId).collection("chat")
//      .orderBy("timestamp", Query.Direction.ASCENDING)
//      .addSnapshotListener { snapshot, error ->
//        error?.let {
//          Log.e("Chat", "Listen failed", it)
//          return@addSnapshotListener
//        }
//        snapshot?.let { querySnapshot ->
//          val messages = querySnapshot.documents.mapNotNull { doc ->
//            doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
//          }
//          _messages.value = messages.reversed()
//        }
//      }
//  }

  fun sendSystemMessage(roomId: String, text: String) {
    val message = ChatMessage(
      text = text, senderId = "system", senderName = "System", timestamp = Date(), type = "system"
    )

    roomsCollection.document(roomId).collection("chat").add(message)
  }


  override fun onCleared() {
    roomListener?.remove()
    roomListener = null
    _currentRoom.value = null
    messagesListener?.remove()
    messagesListener = null
    _messages.value = emptyList()
    userPresenceListener?.let { syncRef.removeEventListener(it) }
    userPresenceListener = null
    syncListener?.let { syncRef.removeEventListener(it) }
    syncListener = null
  }

  // Data classes
  data class Room(
    val id: String = "",
    val name: String = "",
    val hostId: String = "",
    val roomCode: String = "",
    val currentMovie: Movie = Movie(),
    val participants: Map<String, Participant> = emptyMap(),
    val createdAt: Date = Date()
  ) {
    fun toMap(): Map<String, Any> {
      return mapOf(
        "id" to id,
        "name" to name,
        "hostId" to hostId,
        "currentMovie" to currentMovie.toMap(),
        "participants" to participants,
        "createdAt" to FieldValue.serverTimestamp(),
        "roomCode" to roomCode
      )
    }
  }

  data class Participant(
    val userID: String = "",
    val userName: String = "",
    val isHost: Boolean = false,
    val joinedAt: Long = System.currentTimeMillis(),
    val isOnline: Boolean = true,
    val lastSeen: Long = System.currentTimeMillis()
  )

  data class Movie(
    val title: String = "",
    val url: String = "",
  ) {
    fun toMap(): Map<String, Any> {
      return mapOf(
        "title" to title,
        "url" to url,
      )
    }
  }

  data class SyncState(
    val currentTime: Long = 0, val playbackState: String = "paused", val lastUpdated: Long = 0,
  )

  data class UserSyncState(
    val buffering: Boolean = false, val lastSeen: Long = 0, val userID: String = ""
  )

  data class ChatMessage(
    val id: String = "",
    val text: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val timestamp: Date = Date(),
    val type: String = "text" // "text" or "system"
  )
}