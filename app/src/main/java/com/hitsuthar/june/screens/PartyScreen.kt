package com.hitsuthar.june.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun PartyScreen(innersPadding: PaddingValues) {
    Box(Modifier.padding(innersPadding)) {
        Text("PArty screen")
    }
}