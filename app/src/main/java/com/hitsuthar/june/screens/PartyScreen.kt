package com.hitsuthar.june.screens

import MovieSyncViewModel
import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Typeface
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import com.hitsuthar.june.SharedPreferencesManager
import com.hitsuthar.june.utils.EmojiDetector
import com.hitsuthar.june.viewModels.ContentDetailViewModel
import com.hitsuthar.june.viewModels.SelectedVideoViewModel
import com.hitsuthar.june.viewModels.VideoPlayerViewModel
import com.hitsuthar.june.viewModels.WatchPartyViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun PartyScreen(
  innersPadding: PaddingValues,
  context: Context,
  movieSyncViewModel: MovieSyncViewModel,
  navController: NavController,
  window: WindowInsetsControllerCompat,
  selectedVideo: SelectedVideoViewModel,
  watchPartyViewModel: WatchPartyViewModel,
  videoPlayerViewModel: VideoPlayerViewModel,
  contentDetailViewModel: ContentDetailViewModel
) {

  val userID = SharedPreferencesManager(context.applicationContext).getData("USER_ID", "")
  val userName = SharedPreferencesManager(context.applicationContext).getData("USER_NAME", "")

  val currentRoom by movieSyncViewModel.currentRoom.collectAsState()
  val messages by movieSyncViewModel.messages.collectAsState()
  var messageText by remember { mutableStateOf("") }

  if (currentRoom != null) {
    // Start listening for messages
    LaunchedEffect(currentRoom!!.id) {
      movieSyncViewModel.listenForMessages(currentRoom!!.id)
    }

    Column(modifier = Modifier.padding(bottom = innersPadding.calculateBottomPadding())) {
      VideoPlayerScreen(
        modifier = Modifier,
        navController = navController,
        window = window,
        selectedVideo = selectedVideo,
        watchPartyViewModel = watchPartyViewModel,
        context = context,
        innersPadding = innersPadding,
        videoPlayerViewModel = videoPlayerViewModel,
        contentDetailViewModel = contentDetailViewModel,
        movieSyncViewModel = movieSyncViewModel
      )
      CurrentRoomCard(room = currentRoom!!, currentUserId = userID, onLeaveRoom = {
        movieSyncViewModel.leaveRoom(
          currentRoom!!.id, userID
        )
      })

      LazyColumn(
        modifier = Modifier.weight(1f)
//          .padding(horizontal = 8.dp)
        , reverseLayout = true
      ) {
        itemsIndexed(messages) { index, chatMessage ->
          ChatMessageBubble(
            message = chatMessage,
            isCurrentUser = chatMessage.senderId == userID,
            isLastCurrentUser = index < messages.lastIndex && messages[index + 1].senderId == chatMessage.senderId,
            isBeforeUserIsCurrentUser = index > 0 && messages[index - 1].senderId == chatMessage.senderId
          )
        }

      }

      OutlinedTextField(
        value = messageText,
        onValueChange = { messageText = it },
        modifier = Modifier
          .height(48.dp).fillMaxWidth().padding(8.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        placeholder = { Text("Type a message...") },
        singleLine = true,
        trailingIcon = {
          IconButton(
            onClick = {
              if (messageText.isNotBlank()) {
                movieSyncViewModel.sendMessage(currentRoom!!.id, messageText, userID, userName)
                messageText = ""
              }
            }, enabled = messageText.isNotBlank()
          ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
          }
        },
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = MaterialTheme.colorScheme.primary, // Primary color when focused
          unfocusedBorderColor = MaterialTheme.colorScheme.outline, // Default outline color
          errorBorderColor = MaterialTheme.colorScheme.error, // Error outline color
          focusedLabelColor = MaterialTheme.colorScheme.primary, // Label color when focused
          unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant, // Label color when unfocused
          errorLabelColor = MaterialTheme.colorScheme.error, // Label color in error state
          cursorColor = MaterialTheme.colorScheme.primary, // Cursor color
          focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), // Subtle background when focused
          unfocusedContainerColor = MaterialTheme.colorScheme.surface, // Default background
          errorContainerColor = MaterialTheme.colorScheme.errorContainer // Background color in error state
        ),
        shape = CircleShape,

      )


    }
  } else {
    movieSyncViewModel.getMyRooms(userID)
    Column(
      Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      JoinAndCreateRoom(
        innersPadding = innersPadding,
        movieSyncViewModel = movieSyncViewModel,
        userID = userID,
        userName
      )
      Column {
        Text(text = "My Rooms")
        movieSyncViewModel.myRooms.value.forEach { room ->
          Text(room.name + " #" + room.roomCode)
        }
      }
    }
  }
}


@Composable
fun CurrentRoomCard(
  room: MovieSyncViewModel.Room, currentUserId: String, onLeaveRoom: () -> Unit
//  onUpdateStatus: (RoomStatus) -> Unit
) {
  val isHost = room.hostId == currentUserId

  Column(
//    modifier = Modifier.padding(innersPadding)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "Current Room",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Row(
          horizontalArrangement = Arrangement.Absolute.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = room.name,
            style = MaterialTheme.typography.titleMedium,
          )
          Text(
            text = " #${room.roomCode}",
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily(typeface = Typeface(android.graphics.Typeface.MONOSPACE)),
          )
        }
      }

      Row {
        if (isHost) {
          Icon(
            Icons.Default.Star,
            contentDescription = "Host",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
        }

        IconButton(onClick = onLeaveRoom) {
          Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Leave Room")
        }
      }
    }

    // Participants list
    if (room.participants.isNotEmpty()) {
      Spacer(modifier = Modifier.height(8.dp))
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        item { }

        items(room.participants.values.toList()) { participant ->
//            Text(participant.toString())
          ParticipantChip(participant)
        }
      }
    }

  }
}


@Composable
fun ChatMessageBubble(
  message: MovieSyncViewModel.ChatMessage,
  isCurrentUser: Boolean,
  isLastCurrentUser: Boolean,
  isBeforeUserIsCurrentUser: Boolean
) {
//  val bubbleColor = if (isCurrentUser) {
//    MaterialTheme.colorScheme.surfaceVariant
//  } else {
//    MaterialTheme.colorScheme.surfaceVariant
//  }

  val baseStyle = MaterialTheme.typography.bodyMedium
  val textStyle = remember(message.text) {
    if (EmojiDetector.isPureEmoji(message.text)) {
      baseStyle.copy(
        fontSize = baseStyle.fontSize * 1.5f,  // Bigger for pure emoji
//        lineHeight = 1.sp   // Adjust line height
      )
    } else {
      baseStyle  // Normal style for mixed content
    }
  }


  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 8.dp)
    //    horizontalAlignment = alignment
  ) {
    if (message.type != "system") {
      if (!isLastCurrentUser) Spacer(modifier = Modifier.height(8.dp))
      Column {
        if (!isLastCurrentUser) {
          Row(
            horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier
          ) {
            Text(
              text = message.senderName.uppercase(),
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(4.dp))
            Text(
              text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(message.timestamp),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Light,
              color = MaterialTheme.colorScheme.primary
            )
          }
        }
        Text(
          text = message.text,
          color = when {
//            message.type == "system" -> MaterialTheme.colorScheme.onSurfaceVariant
            isCurrentUser -> MaterialTheme.colorScheme.onSurfaceVariant
            else -> MaterialTheme.colorScheme.onSurfaceVariant
          },
          style = textStyle
        )
      }
    }


  }
}


@Composable
fun ParticipantChip(participant: MovieSyncViewModel.Participant) {
  AssistChip(
    onClick = { }, label = {
      Text(
        text = participant.userName, style = MaterialTheme.typography.bodySmall
      )
    }, leadingIcon = if (participant.isHost) {
      { Icon(Icons.Default.Star, contentDescription = "Host", modifier = Modifier.size(16.dp)) }
    } else null, trailingIcon = {
      Box(
        modifier = Modifier
          .size(8.dp)
          .background(
            color = if (participant.isOnline) Color.Green else Color.Gray,
            shape = androidx.compose.foundation.shape.CircleShape
          )
      )
    })
}


@Composable
fun JoinAndCreateRoom(
  innersPadding: PaddingValues,
  movieSyncViewModel: MovieSyncViewModel,
  userID: String,
  userName: String
) {
  var showCreateDialog by remember { mutableStateOf(false) }
  var showJoinDialog by remember { mutableStateOf(false) }

  Box(
    Modifier.padding(innersPadding), contentAlignment = Alignment.Center
  ) {
    Column(
      verticalArrangement = Arrangement.spacedBy(4.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Button(
        onClick = { showCreateDialog = true },
      ) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Create Room")
      }

      OutlinedButton(
        onClick = { showJoinDialog = true },
//        enabled = !uiState.isLoading
      ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Join Room")
      }
    }
  }
  if (showCreateDialog) {
    CreateRoomDialog(onDismiss = { showCreateDialog = false }, onCreateRoom = { roomName ->
      movieSyncViewModel.createRoom(roomName, userID, userName)
      showCreateDialog = false
    })
  }

  if (showJoinDialog) {
    JoinRoomDialog(onDismiss = { showJoinDialog = false }, onJoinRoom = { roomCode ->
      movieSyncViewModel.joinRoom(roomCode, userID, userName)
      showJoinDialog = false
    })
  }

}

@Composable
fun CreateRoomDialog(
  onDismiss: () -> Unit, onCreateRoom: (String) -> Unit
) {
  var roomName by remember { mutableStateOf("") }

  AlertDialog(onDismissRequest = onDismiss, title = { Text("Create Room") }, text = {
    Column {
      OutlinedTextField(
        value = roomName,
        onValueChange = { roomName = it },
        label = { Text("Room Name") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
      )
    }
  }, confirmButton = {
    TextButton(
      onClick = { onCreateRoom(roomName) }) {
      Text("Create Room")
    }
  }, dismissButton = {
    TextButton(onClick = onDismiss) {
      Text("Cancel")
    }
  })
}

@Composable
fun JoinRoomDialog(
  onDismiss: () -> Unit, onJoinRoom: (String) -> Unit
) {
  var roomCode by remember { mutableStateOf("") }

  AlertDialog(onDismissRequest = onDismiss, title = { Text("Join Room") }, text = {
    Column {
      Text(
        text = "Enter the 6-character room code to join a movie room.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(16.dp))

      OutlinedTextField(
        value = roomCode,
        onValueChange = {
          if (it.length <= 6) {
            roomCode = it.uppercase().filter { char ->
              char.isLetterOrDigit()
            }
          }
        },
        label = { Text("Room Code") },
        placeholder = { Text("ABC123") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        supportingText = {
          Text("${roomCode.length}/6 characters")
        })
    }
  }, confirmButton = {
    TextButton(
      onClick = {
        if (roomCode.length == 6) {
          onJoinRoom(roomCode)
        }
      }, enabled = roomCode.length == 6
    ) {
      Text("Join Room")
    }
  }, dismissButton = {
    TextButton(onClick = onDismiss) {
      Text("Cancel")
    }
  })
}