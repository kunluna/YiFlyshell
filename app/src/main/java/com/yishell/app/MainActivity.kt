package com.yishell.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.yishell.app.data.local.AppSettings
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.yishell.app.data.local.SettingsDataStore
import com.yishell.app.presentation.navigation.YiFeiNavHost
import com.yishell.app.presentation.theme.YiShellTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsDataStore.settings.collectAsState(initial = AppSettings())
            YiShellTheme(isDarkTheme = settings.isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    YiFeiNavHost(navController = navController)
                }
            }
        }
    }
}
