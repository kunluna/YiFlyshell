package com.yishell.app.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yishell.app.presentation.about.AboutScreen
import com.yishell.app.presentation.connection.AddConnectionScreen
import com.yishell.app.presentation.home.HomeScreen
import com.yishell.app.presentation.sftp.SftpScreen
import com.yishell.app.presentation.settings.AppearanceSettingsScreen
import com.yishell.app.presentation.settings.ConnectionSettingsScreen
import com.yishell.app.presentation.settings.SettingsScreen
import com.yishell.app.presentation.settings.TerminalSettingsScreen
import com.yishell.app.presentation.terminal.TerminalScreen
@Composable
fun YiFeiNavHost(
    navController: NavHostController,
    startDestination: String = Screen.Home.route
) {
    var activeConnectionId by remember { mutableStateOf("") }
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize()
    ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onConnect = { connectionId ->
                        if (connectionId.isNotBlank()) {
                            activeConnectionId = connectionId
                            navController.navigate(Screen.Terminal.createRoute(connectionId))
                        }
                    },
                    onAddConnection = {
                        navController.navigate(Screen.AddConnection.route)
                    },
                    onEditConnection = { connectionId ->
                        if (connectionId.isNotBlank()) {
                            navController.navigate(Screen.EditConnection.createRoute(connectionId))
                        }
                    },
                    onSettings = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onAbout = {
                        navController.navigate(Screen.About.route)
                    },
                    onSearch = {
                    }
                )
            }
            composable(
                route = Screen.Terminal.route,
                arguments = listOf(
                    navArgument("connectionId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val connectionId = backStackEntry.arguments?.getString("connectionId") ?: ""
                if (connectionId.isNotBlank()) {
                    TerminalScreen(
                        connectionId = connectionId,
                        onBack = { navController.popBackStack() },
                        onSftp = {
                            navController.navigate(Screen.Sftp.createRoute(connectionId))
                        },
                        onMonitor = {
                            navController.navigate(Screen.Monitor.createRoute(connectionId))
                        },
                        onEditConnection = {
                            navController.navigate(Screen.EditConnection.createRoute(connectionId))
                        }
                    )
                } else {
                    navController.popBackStack()
                }
            }

            composable(
                route = Screen.Sftp.route,
                arguments = listOf(
                    navArgument("connectionId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val connectionId = backStackEntry.arguments?.getString("connectionId") ?: ""
                if (connectionId.isNotBlank()) {
                    SftpScreen(
                        connectionId = connectionId,
                        onBack = { navController.popBackStack() }
                    )
                } else {
                    navController.popBackStack()
                }
            }

            composable(
                route = Screen.Monitor.route,
                arguments = listOf(
                    navArgument("connectionId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val connectionId = backStackEntry.arguments?.getString("connectionId") ?: ""
                if (connectionId.isNotBlank()) {
                    com.yishell.app.presentation.monitor.MonitorScreen(
                        connectionId = connectionId,
                        onBack = { navController.popBackStack() }
                    )
                } else {
                    navController.popBackStack()
                }
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToAppearance = {
                        navController.navigate(Screen.SettingsAppearance.route)
                    },
                    onNavigateToTerminal = {
                        navController.navigate(Screen.SettingsTerminal.route)
                    },
                    onNavigateToConnection = {
                        navController.navigate(Screen.SettingsConnection.route)
                    },
                    onNavigateToAbout = {
                        navController.navigate(Screen.About.route)
                    }
                )
            }

            composable(Screen.SettingsAppearance.route) {
                AppearanceSettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.SettingsTerminal.route) {
                TerminalSettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.SettingsConnection.route) {
                ConnectionSettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.AddConnection.route) {
                AddConnectionScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.About.route) {
                AboutScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.EditConnection.route,
                arguments = listOf(
                    navArgument("connectionId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val connectionId = backStackEntry.arguments?.getString("connectionId") ?: ""
                if (connectionId.isNotBlank()) {
                    AddConnectionScreen(
                        connectionId = connectionId,
                        onBack = { navController.popBackStack() }
                    )
                } else {
                    navController.popBackStack()
                }
            }
        }
    }
