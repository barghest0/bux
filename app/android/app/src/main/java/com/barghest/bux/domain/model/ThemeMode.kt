package com.barghest.bux.domain.model

enum class ThemeMode(val value: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromValue(value: String): ThemeMode {
            return entries.firstOrNull { it.value == value } ?: SYSTEM
        }
    }
}
