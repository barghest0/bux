package com.barghest.bux.data.local

import android.content.Context
import android.content.SharedPreferences
import com.barghest.bux.domain.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.value).apply()
        _themeMode.value = mode
    }

    private fun loadThemeMode(): ThemeMode {
        val value = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.value)
            ?: ThemeMode.SYSTEM.value
        return ThemeMode.fromValue(value)
    }

    companion object {
        private const val PREFS_NAME = "bux_prefs"
        private const val KEY_THEME_MODE = "theme_mode"
    }
}
