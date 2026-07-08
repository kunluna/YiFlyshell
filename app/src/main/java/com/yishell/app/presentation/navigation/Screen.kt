package com.yishell.app.presentation.navigation

import java.net.URLEncoder

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Terminal : Screen("terminal/{connectionId}") {
        fun createRoute(connectionId: String) = "terminal/${URLEncoder.encode(connectionId, "UTF-8")}"
    }
    data object Sftp : Screen("sftp/{connectionId}") {
        fun createRoute(connectionId: String) = "sftp/${URLEncoder.encode(connectionId, "UTF-8")}"
    }
    data object Monitor : Screen("monitor/{connectionId}") {
        fun createRoute(connectionId: String) = "monitor/${URLEncoder.encode(connectionId, "UTF-8")}"
    }
    data object Settings : Screen("settings")
    data object SettingsAppearance : Screen("settings_appearance")
    data object SettingsTerminal : Screen("settings_terminal")
    data object SettingsConnection : Screen("settings_connection")
    data object AddConnection : Screen("add_connection")
    data object EditConnection : Screen("edit_connection/{connectionId}") {
        fun createRoute(connectionId: String) = "edit_connection/${URLEncoder.encode(connectionId, "UTF-8")}"
    }
    data object About : Screen("about")
    data object TerminalLogs : Screen("terminal_logs")

}
