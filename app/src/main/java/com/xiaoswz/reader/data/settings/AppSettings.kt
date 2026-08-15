package com.xiaoswz.reader.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import com.xiaoswz.reader.BuildConfig
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
        // ── 云同步相关（不碰 Room，全部放 DataStore）──
        val DEVICE_ID = stringPreferencesKey("device_id") // 匿名设备身份，首次生成后不变
        val LAST_SYNC_AT = longPreferencesKey("last_sync_at") // 上次成功同步时间戳
        val BACKEND_BASE_URL = stringPreferencesKey("backend_base_url") // 后端地址（可改）
        val AUTO_SYNC = booleanPreferencesKey("auto_sync") // 启动时自动同步
        // 上次同步推送到云端的书架 slug 集合（用于检测本地删除并传播到云端）
        val SYNCED_SLUGS = stringPreferencesKey("synced_slugs")
    }

    val themeModeFlow: Flow<Int> = context.appSettingsStore.data.map { prefs ->
        prefs[Keys.THEME_MODE] ?: AppThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: Int) {
        context.appSettingsStore.edit { it[Keys.THEME_MODE] = mode }
    }

    // ── 设备身份 ──
    /** 返回已持久化的设备 ID；若不存在则生成 UUID 并保存（一次性） */
    suspend fun getDeviceId(): String {
        val existing = context.appSettingsStore.data.first()[Keys.DEVICE_ID]
        if (!existing.isNullOrBlank()) return existing
        val newId = java.util.UUID.randomUUID().toString()
        context.appSettingsStore.edit { it[Keys.DEVICE_ID] = newId }
        return newId
    }

    val deviceIdFlow: Flow<String?> = context.appSettingsStore.data.map { it[Keys.DEVICE_ID] }

    // ── 上次同步时间 ──
    suspend fun getLastSyncAt(): Long {
        return context.appSettingsStore.data.first()[Keys.LAST_SYNC_AT] ?: 0L
    }

    suspend fun setLastSyncAt(ts: Long) {
        context.appSettingsStore.edit { it[Keys.LAST_SYNC_AT] = ts }
    }

    val lastSyncAtFlow: Flow<Long> = context.appSettingsStore.data.map { it[Keys.LAST_SYNC_AT] ?: 0L }

    // ── 后端地址 ──
    suspend fun getBackendBaseUrl(): String {
        return context.appSettingsStore.data.first()[Keys.BACKEND_BASE_URL]
            ?: BuildConfig.BACKEND_BASE_URL
    }

    suspend fun setBackendBaseUrl(url: String) {
        context.appSettingsStore.edit { it[Keys.BACKEND_BASE_URL] = url }
    }

    // ── 后端地址一次性迁移（0.6.5）──
    /**
     * 旧版（0.6.4 及之前）BACKEND_BASE_URL 指向 *.vercel.app，国内被 DNS 污染，
     * 且该值曾被持久化进 DataStore，会覆盖 BuildConfig 默认局域网地址。
     * 启动检测到含 vercel.app 时自动改写到 BuildConfig.BACKEND_BASE_URL（局域网后端）并回写。
     */
    suspend fun migrateBackendUrlIfNeeded() {
        val current = context.appSettingsStore.data.first()[Keys.BACKEND_BASE_URL]
        if (!current.isNullOrBlank() && current.contains("vercel.app", ignoreCase = true)) {
            context.appSettingsStore.edit { it[Keys.BACKEND_BASE_URL] = BuildConfig.BACKEND_BASE_URL }
        }
    }

    val backendBaseUrlFlow: Flow<String> = context.appSettingsStore.data.map {
        it[Keys.BACKEND_BASE_URL] ?: BuildConfig.BACKEND_BASE_URL
    }

    // ── 自动同步开关 ──
    suspend fun getAutoSync(): Boolean {
        return context.appSettingsStore.data.first()[Keys.AUTO_SYNC] ?: true
    }

    suspend fun setAutoSync(on: Boolean) {
        context.appSettingsStore.edit { it[Keys.AUTO_SYNC] = on }
    }

    val autoSyncFlow: Flow<Boolean> = context.appSettingsStore.data.map { it[Keys.AUTO_SYNC] ?: true }

    // ── 已同步 slug 集合（换行分隔存储）──
    suspend fun getSyncedSlugs(): Set<String> {
        val raw = context.appSettingsStore.data.first()[Keys.SYNCED_SLUGS] ?: ""
        return if (raw.isBlank()) emptySet() else raw.split("\n").filter { it.isNotBlank() }.toSet()
    }

    suspend fun setSyncedSlugs(set: Set<String>) {
        context.appSettingsStore.edit { it[Keys.SYNCED_SLUGS] = set.joinToString("\n") }
    }
}
