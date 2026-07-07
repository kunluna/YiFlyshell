package com.yishell.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yishell.app.data.local.AppSettings
import com.yishell.app.data.local.SettingsDataStore
import com.yishell.app.data.local.TerminalColorScheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsDataStore.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    fun updateDarkTheme(isDark: Boolean) {
        viewModelScope.launch { settingsDataStore.updateDarkTheme(isDark) }
    }

    fun updateDefaultPort(port: Int) {
        viewModelScope.launch { settingsDataStore.updateDefaultPort(port) }
    }

    fun updateFontSize(size: Int) {
        viewModelScope.launch { settingsDataStore.updateFontSize(size) }
    }

    fun updateTerminalColorScheme(scheme: TerminalColorScheme) {
        viewModelScope.launch { settingsDataStore.updateTerminalColorScheme(scheme) }
    }

    fun resetTheme() {
        viewModelScope.launch {
            settingsDataStore.updateDarkTheme(false)
            settingsDataStore.updateFontSize(14)
            settingsDataStore.updateDefaultPort(22)
            settingsDataStore.updateTerminalColorScheme(TerminalColorScheme.AUTO)
            settingsDataStore.updateKeyboardLayout("[\"⏎ Enter\",\"␣ Space\",\"←\",\"↑\",\"→\",\"↓\",\"Esc\",\"Tab\",\"Ctrl\",\"Alt\",\";\",\"/\",\"|\",\"-\",\"_\",\"~\",\".\",\"历史↑\",\"历史↓\",\"PgUp\",\"PgDn\"]")
        }
    }

    fun updateKeyboardLayout(layout: String) {
        viewModelScope.launch { settingsDataStore.updateKeyboardLayout(layout) }
    }

    fun updateGlassEffect(value: Boolean) {
        viewModelScope.launch { settingsDataStore.updateGlassEffect(value) }
    }

    fun updateSshTimeout(value: Int) {
        viewModelScope.launch { settingsDataStore.updateSshTimeout(value) }
    }

    fun updateAutoReconnect(value: Boolean) {
        viewModelScope.launch { settingsDataStore.updateAutoReconnect(value) }
    }

    fun updateKeepAliveInterval(value: Int) {
        viewModelScope.launch { settingsDataStore.updateKeepAliveInterval(value) }
    }
}
