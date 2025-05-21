package com.hitsuthar.june

sealed class Screen(val route: String) {
    object Home : Screen(route = "Home_Screen")
    object Search : Screen(route = "Search_Screen")
    object Detail : Screen(route = "Detail_Screen")
    object Torrent : Screen(route = "Torrent_Screen")
    object DDL : Screen(route = "DDL_Screen")
    object VideoPlayer : Screen(route = "VideoPlayer_Screen")
    object Settings : Screen(route = "Settings_Screen")
    object Party : Screen(route = "Party_Screen")

}