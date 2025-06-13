package com.hitsuthar.june

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController


data class BottomNavItem(
  val name: String,
  val route: String,
  val icon: ImageVector
)


@Composable
fun BottomNavigationBar(
  navController: NavController,
  currentRoute: String?,
  isFullScreen: Boolean,
) {
  val items = listOf(
    BottomNavItem("Home", Screen.Home.route, Icons.Default.Home),
//        BottomNavItem("Search", Screen.Search.route, Icons.Default.Search),
    BottomNavItem("Party", Screen.Party.route, Icons.Default.PlayArrow),
    BottomNavItem("Settings", Screen.Settings.route, Icons.Default.Settings)
  )

  AnimatedVisibility(visible = !isFullScreen) {
    Box(modifier = Modifier.padding(horizontal = 16.dp).navigationBarsPadding()){
      NavigationBar(
        modifier = Modifier.clip(shape = RoundedCornerShape(24.dp)).height(72.dp)) {
        items.forEach { item ->
          NavigationBarItem(
            icon = { Icon(imageVector = item.icon, contentDescription = item.name) },
            label = { Text(text = item.name) },
            selected = currentRoute == item.route,
            onClick = {
              navController.navigate(item.route) {
                popUpTo(navController.graph.startDestinationId)
                launchSingleTop = true
              }
            }
          )
        }
      }
    }

  }
}
