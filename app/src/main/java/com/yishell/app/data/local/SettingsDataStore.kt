package com.yishell.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class TerminalColorScheme {
    AUTO,
    DEFAULT,
    SOLARIZED_DARK,
    SOLARIZED_LIGHT,
    MONOKAI,
    DRACULA
}

data class AppSettings(
    val isDarkTheme: Boolean = false,
    val defaultPort: Int = 22,
    val fontSize: Int = 14,
    val terminalColorScheme: TerminalColorScheme = TerminalColorScheme.AUTO,
    val keyboardLayout: String = "[\"⏎ Enter\",\"␣ Space\",\"←\",\"↑\",\"→\",\"↓\",\"Esc\",\"Tab\",\"Ctrl\",\"Alt\",\";\",\"/\",\"|\",\"-\",\"_\",\"~\",\".\",\"历史↑\",\"历史↓\",\"PgUp\",\"PgDn\"]",
    val glassEffect: Boolean = true,
    val sshTimeout: Int = 30,
    val autoReconnect: Boolean = true,
    val keepAliveInterval: Int = 60
)

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore by lazy { context.dataStore }

    private object Keys {
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val DEFAULT_PORT = intPreferencesKey("default_port")
        val FONT_SIZE = intPreferencesKey("font_size")
        val TERMINAL_COLOR_SCHEME = stringPreferencesKey("terminal_color_scheme")
        val KEYBOARD_LAYOUT = stringPreferencesKey("keyboard_layout")
        val GLASS_EFFECT = booleanPreferencesKey("glass_effect")
        val SSH_TIMEOUT = intPreferencesKey("ssh_timeout")
        val AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
        val KEEP_ALIVE_INTERVAL = intPreferencesKey("keep_alive_interval")
    }

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            isDarkTheme = prefs[Keys.IS_DARK_THEME] ?: false,
            defaultPort = prefs[Keys.DEFAULT_PORT] ?: 22,
            fontSize = prefs[Keys.FONT_SIZE] ?: 14,
            terminalColorScheme = try {
                TerminalColorScheme.valueOf(prefs[Keys.TERMINAL_COLOR_SCHEME] ?: "AUTO")
            } catch (e: Exception) {
                Log.w("SettingsDataStore", "Invalid terminal color scheme: ${prefs[Keys.TERMINAL_COLOR_SCHEME]}", e)
                TerminalColorScheme.AUTO
            },
            keyboardLayout = prefs[Keys.KEYBOARD_LAYOUT] ?: "[\"⏎ Enter\",\"␣ Space\",\"←\",\"↑\",\"→\",\"↓\",\"Esc\",\"Tab\",\"Ctrl\",\"Alt\",\";\",\"/\",\"|\",\"-\",\"_\",\"~\",\".\",\"历史↑\",\"历史↓\",\"PgUp\",\"PgDn\"]",
            glassEffect = prefs[Keys.GLASS_EFFECT] ?: true,
            sshTimeout = prefs[Keys.SSH_TIMEOUT] ?: 30,
            autoReconnect = prefs[Keys.AUTO_RECONNECT] ?: true,
            keepAliveInterval = prefs[Keys.KEEP_ALIVE_INTERVAL] ?: 60
        )
    }

    suspend fun updateDarkTheme(isDark: Boolean) {
        dataStore.edit { it[Keys.IS_DARK_THEME] = isDark }
    }

    suspend fun updateDefaultPort(port: Int) {
        dataStore.edit { it[Keys.DEFAULT_PORT] = port }
    }

    suspend fun updateFontSize(size: Int) {
        dataStore.edit { it[Keys.FONT_SIZE] = size }
    }

    suspend fun updateTerminalColorScheme(scheme: TerminalColorScheme) {
        dataStore.edit { it[Keys.TERMINAL_COLOR_SCHEME] = scheme.name }
    }

    suspend fun updateKeyboardLayout(layout: String) {
        dataStore.edit { it[Keys.KEYBOARD_LAYOUT] = layout }
    }

    suspend fun updateGlassEffect(value: Boolean) {
        dataStore.edit { it[Keys.GLASS_EFFECT] = value }
    }

    suspend fun updateSshTimeout(value: Int) {
        dataStore.edit { it[Keys.SSH_TIMEOUT] = value }
    }

    suspend fun updateAutoReconnect(value: Boolean) {
        dataStore.edit { it[Keys.AUTO_RECONNECT] = value }
    }

    suspend fun updateKeepAliveInterval(value: Int) {
        dataStore.edit { it[Keys.KEEP_ALIVE_INTERVAL] = value }
    }
}
