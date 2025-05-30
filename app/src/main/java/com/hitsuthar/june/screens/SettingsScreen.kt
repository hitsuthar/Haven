package com.hitsuthar.june.screens

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.hitsuthar.june.SharedPreferencesManager

@Composable
fun SettingsScreen(
  innersPadding: PaddingValues, context: Context,
) {
  val userID = SharedPreferencesManager(context.applicationContext).getData("USER_ID", "")
  val userName = SharedPreferencesManager(context.applicationContext).getData("USER_NAME", "")
  var showChangeNameDialog by remember { mutableStateOf(false) }

  Box(modifier = Modifier.padding(innersPadding)) {
    Column {
      Text(text = "User ID: $userID")
      Text(text = "Name: $userName")
      Button(onClick = {showChangeNameDialog = true}) {
        Text("Change Name")
      }
      if (showChangeNameDialog) {
        ChangeNameDialog(onDismiss = { showChangeNameDialog = false }, onNameChange = { name ->
          SharedPreferencesManager(context.applicationContext).saveData("USER_NAME", name)
          showChangeNameDialog = false
        })
      }
    }
  }
}

@Composable
fun ChangeNameDialog(
  onDismiss: () -> Unit, onNameChange: (String) -> Unit
) {
  var changedName by remember { mutableStateOf("") }

  AlertDialog(onDismissRequest = onDismiss, title = { Text("Change name") }, text = {
    Column {
      OutlinedTextField(
        value = changedName,
        onValueChange = { changedName = it },
        label = { Text("Enter Name") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
      )
    }
  }, confirmButton = {
    TextButton(
      onClick = { onNameChange(changedName) }) {
      Text("Change Name")
    }
  }, dismissButton = {
    TextButton(onClick = onDismiss) {
      Text("Cancel")
    }
  })
}