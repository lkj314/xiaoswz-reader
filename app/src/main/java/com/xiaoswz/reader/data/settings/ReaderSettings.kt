package com.xiaoswz.reader.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.xiaoswz.reader.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.readerSettingsStore: DataStore<Preferences> by preferencesDataStore(
    name = "reader_settings",
)

/**
 * 阅读器设置（DataStore 持久化）
 */
data class ReaderSettings(
    val fontSize: Int = 18,
    val themeIndex: Int = THEME_DAY,
    val pageMode: Int = MODE_SCROLL,
    val lineSpacing: Float = 1.7f,
    /** 段落间额外空行数 0/1/2 */
    val paraSpacing: Int = 0,
    /** 左右边距档位 0窄(12dp)/1标准(20dp)/2宽(28dp) */
    val marginIndex: Int = 1,
    /** 段落首行缩进（两字符） */
    val indentFirstLine: Boolean = true,
    val volumeKeyPaging: Boolean = true,
    val keepScreenOn: Boolean = true,
    /** 局域网更新服务器地址 */
    val updateServerUrl: String = BuildConfig.DEFAULT_UPDATE_SERVER,
    /** 后台预读：向后预取章数（0=关闭），默认 3 → 翻章几乎零等待 */
    val prefetchNext: Int = 3,
    /** 后台预读：向前预取章数（0=关闭），默认 1 */
    val prefetchPrev: Int = 1,
    /** 滚动模式触底自动续读下一章（下一章已预读，切换无感） */
    val continuousScroll: Boolean = true,
    /** 听书语音语速 0.5~2.0，默认 1.0 */
    val ttsRate: Float = 1.0f,
    /** 护眼蓝光过滤：叠加暖色滤镜降低蓝光 */
    val blueLightFilter: Boolean = false,
    /** 连续阅读休息提醒开关 */
    val restReminderEnabled: Boolean = false,
    /** 休息提醒间隔（分钟） */
    val restReminderMinutes: Int = 20,
) {
    companion object {
        const val THEME_DAY = 0
        const val THEME_EYE_GREEN = 1
        const val THEME_NIGHT = 2
        const val THEME_BLACK = 3

        const val MODE_SCROLL = 0
        const val MODE_COVER = 1

        const val MIN_FONT_SIZE = 14
        const val MAX_FONT_SIZE = 28

        const val MIN_LINE_SPACING = 1.3f
        const val MAX_LINE_SPACING = 2.2f

        const val MIN_TTS_RATE = 0.5f
        const val MAX_TTS_RATE = 2.0f
    }
}

class ReaderSettingsRepository(private val context: Context) {

    private object Keys {
        val FONT_SIZE = intPreferencesKey("font_size")
        val THEME_INDEX = intPreferencesKey("theme_index")
        val PAGE_MODE = intPreferencesKey("page_mode")
        val LINE_SPACING = floatPreferencesKey("line_spacing")
        val PARA_SPACING = intPreferencesKey("para_spacing")
        val MARGIN_INDEX = intPreferencesKey("margin_index")
        val INDENT = booleanPreferencesKey("indent_first_line")
        val VOLUME_KEYS = booleanPreferencesKey("volume_key_paging")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val UPDATE_SERVER = stringPreferencesKey("update_server_url")
        val PREFETCH_NEXT = intPreferencesKey("prefetch_next")
        val PREFETCH_PREV = intPreferencesKey("prefetch_prev")
        val CONTINUOUS = booleanPreferencesKey("continuous_scroll")
        val TTS_RATE = floatPreferencesKey("tts_rate")
        val BLUE_LIGHT = booleanPreferencesKey("blue_light_filter")
        val REST_REMINDER = booleanPreferencesKey("rest_reminder_enabled")
        val REST_MINUTES = intPreferencesKey("rest_reminder_minutes")
    }

    val settingsFlow: Flow<ReaderSettings> =
        context.readerSettingsStore.data.map { prefs ->
            val defaults = ReaderSettings()
            ReaderSettings(
                fontSize = prefs[Keys.FONT_SIZE] ?: defaults.fontSize,
                themeIndex = prefs[Keys.THEME_INDEX] ?: defaults.themeIndex,
                pageMode = prefs[Keys.PAGE_MODE] ?: defaults.pageMode,
                lineSpacing = prefs[Keys.LINE_SPACING] ?: defaults.lineSpacing,
                paraSpacing = prefs[Keys.PARA_SPACING] ?: defaults.paraSpacing,
                marginIndex = prefs[Keys.MARGIN_INDEX] ?: defaults.marginIndex,
                indentFirstLine = prefs[Keys.INDENT] ?: defaults.indentFirstLine,
                volumeKeyPaging = prefs[Keys.VOLUME_KEYS] ?: defaults.volumeKeyPaging,
                keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: defaults.keepScreenOn,
                updateServerUrl = prefs[Keys.UPDATE_SERVER] ?: defaults.updateServerUrl,
                prefetchNext = prefs[Keys.PREFETCH_NEXT] ?: defaults.prefetchNext,
                prefetchPrev = prefs[Keys.PREFETCH_PREV] ?: defaults.prefetchPrev,
                continuousScroll = prefs[Keys.CONTINUOUS] ?: defaults.continuousScroll,
                ttsRate = prefs[Keys.TTS_RATE] ?: defaults.ttsRate,
                blueLightFilter = prefs[Keys.BLUE_LIGHT] ?: defaults.blueLightFilter,
                restReminderEnabled = prefs[Keys.REST_REMINDER] ?: defaults.restReminderEnabled,
                restReminderMinutes = prefs[Keys.REST_MINUTES] ?: defaults.restReminderMinutes,
            )
        }

    suspend fun update(transform: (ReaderSettings) -> ReaderSettings) {
        context.readerSettingsStore.edit { prefs ->
            val current = ReaderSettings(
                fontSize = prefs[Keys.FONT_SIZE] ?: 18,
                themeIndex = prefs[Keys.THEME_INDEX] ?: ReaderSettings.THEME_DAY,
                pageMode = prefs[Keys.PAGE_MODE] ?: ReaderSettings.MODE_COVER,
                lineSpacing = prefs[Keys.LINE_SPACING] ?: 1.7f,
                paraSpacing = prefs[Keys.PARA_SPACING] ?: 0,
                marginIndex = prefs[Keys.MARGIN_INDEX] ?: 1,
                indentFirstLine = prefs[Keys.INDENT] ?: true,
                volumeKeyPaging = prefs[Keys.VOLUME_KEYS] ?: true,
                keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: true,
                updateServerUrl = prefs[Keys.UPDATE_SERVER] ?: BuildConfig.DEFAULT_UPDATE_SERVER,
                prefetchNext = prefs[Keys.PREFETCH_NEXT] ?: 3,
                prefetchPrev = prefs[Keys.PREFETCH_PREV] ?: 1,
                continuousScroll = prefs[Keys.CONTINUOUS] ?: true,
                ttsRate = prefs[Keys.TTS_RATE] ?: 1.0f,
                blueLightFilter = prefs[Keys.BLUE_LIGHT] ?: false,
                restReminderEnabled = prefs[Keys.REST_REMINDER] ?: false,
                restReminderMinutes = prefs[Keys.REST_MINUTES] ?: 20,
            )
            val next = transform(current)
            prefs[Keys.FONT_SIZE] = next.fontSize.coerceIn(
                ReaderSettings.MIN_FONT_SIZE, ReaderSettings.MAX_FONT_SIZE,
            )
            prefs[Keys.THEME_INDEX] = next.themeIndex
            prefs[Keys.PAGE_MODE] = next.pageMode
            prefs[Keys.LINE_SPACING] = next.lineSpacing.coerceIn(
                ReaderSettings.MIN_LINE_SPACING, ReaderSettings.MAX_LINE_SPACING,
            )
            prefs[Keys.PARA_SPACING] = next.paraSpacing.coerceIn(0, 2)
            prefs[Keys.MARGIN_INDEX] = next.marginIndex.coerceIn(0, 2)
            prefs[Keys.INDENT] = next.indentFirstLine
            prefs[Keys.VOLUME_KEYS] = next.volumeKeyPaging
            prefs[Keys.KEEP_SCREEN_ON] = next.keepScreenOn
            prefs[Keys.UPDATE_SERVER] = next.updateServerUrl
            prefs[Keys.PREFETCH_NEXT] = next.prefetchNext.coerceIn(0, 8)
            prefs[Keys.PREFETCH_PREV] = next.prefetchPrev.coerceIn(0, 4)
            prefs[Keys.CONTINUOUS] = next.continuousScroll
            prefs[Keys.TTS_RATE] = next.ttsRate.coerceIn(
                ReaderSettings.MIN_TTS_RATE, ReaderSettings.MAX_TTS_RATE,
            )
            prefs[Keys.BLUE_LIGHT] = next.blueLightFilter
            prefs[Keys.REST_REMINDER] = next.restReminderEnabled
            prefs[Keys.REST_MINUTES] = next.restReminderMinutes.coerceIn(5, 120)
        }
    }
}
