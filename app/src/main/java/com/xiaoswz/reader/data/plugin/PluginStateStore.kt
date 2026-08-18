package com.xiaoswz.reader.data.plugin

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * 已装插件持久化（DataStore，仿 ReaderSettings 模式）。
 *
 * 不直接碰 Room（遵循「新持久化走文件/DataStore」铁律）；插件清单以 JSON 数组存储，
 * 读取即反序列化为 [PluginInstall] 列表。DIY 本地插件与广场安装插件一视同仁。
 */
private val Context.pluginStore: DataStore<Preferences> by preferencesDataStore(
    name = "creative_workshop_plugins",
)

private val KEY_INSTALLED = stringPreferencesKey("installed_plugins")

private val json = Json { ignoreUnknownKeys = true }

object PluginStateStore {

    /** 已装插件流（含启用状态）。无则返回空列表。 */
    fun installedFlow(ctx: Context): Flow<List<PluginInstall>> =
        ctx.pluginStore.data.map { prefs ->
            prefs[KEY_INSTALLED]?.let { raw ->
                runCatching {
                    json.decodeFromString(ListSerializer(PluginInstall.serializer()), raw)
                }.getOrElse { emptyList() }
            } ?: emptyList()
        }

    /** 安装/覆盖一个插件（默认启用） */
    suspend fun install(ctx: Context, manifest: PluginManifest, enabled: Boolean = true) {
        ctx.pluginStore.edit { prefs ->
            val list = currentList(prefs)
            val next = list.filter { it.manifest.id != manifest.id } +
                PluginInstall(manifest, enabled)
            prefs[KEY_INSTALLED] = json.encodeToString(ListSerializer(PluginInstall.serializer()), next)
        }
    }

    /** 设置启用/停用 */
    suspend fun setEnabled(ctx: Context, pluginId: String, enabled: Boolean) {
        ctx.pluginStore.edit { prefs ->
            val next = currentList(prefs).map {
                if (it.manifest.id == pluginId) it.copy(enabled = enabled) else it
            }
            prefs[KEY_INSTALLED] = json.encodeToString(ListSerializer(PluginInstall.serializer()), next)
        }
    }

    /** 卸载 */
    suspend fun uninstall(ctx: Context, pluginId: String) {
        ctx.pluginStore.edit { prefs ->
            val next = currentList(prefs).filter { it.manifest.id != pluginId }
            prefs[KEY_INSTALLED] = json.encodeToString(ListSerializer(PluginInstall.serializer()), next)
        }
    }

    private fun currentList(prefs: Preferences): List<PluginInstall> =
        prefs[KEY_INSTALLED]?.let { raw ->
            runCatching {
                json.decodeFromString(ListSerializer(PluginInstall.serializer()), raw)
            }.getOrElse { emptyList() }
        } ?: emptyList()
}
