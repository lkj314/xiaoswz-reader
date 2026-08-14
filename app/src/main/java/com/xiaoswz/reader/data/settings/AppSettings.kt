package com.xiaoswz.reader.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appSettingsStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_settings",
)

/** 应用外壳主题模式（与阅读器内部 4 套主题独立） */
object AppThemeMode {
    const val SYSTEM = 0
    const val LIGHT = 1
    const val DARK = 2
}

class AppSettingsRepository(private val context: Context) {
    private object Keys {
        val THEME_MODE = intPreferencesKey("theme_mode")
    }

    val themeModeFlow: Flow<Int> = context.appSettingsStore.data.map { prefs ->
        prefs[Keys.THEME_MODE] ?: AppThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: Int) {
        context.appSettingsStore.edit { it[Keys.THEME_MODE] = mode }
    }
}
