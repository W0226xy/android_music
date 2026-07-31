package com.example.myapplication.widget

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Widget 状态 Key 定义 — 基于 [PreferencesGlanceStateDefinition]。
 *
 * Glance 的 PreferencesGlanceStateDefinition 是单例对象，基于 DataStore Preferences。
 * 每个字段需要定义一个 [androidx.datastore.preferences.core.Preferences.Key]。
 */
object WidgetStateKeys {
    val songName    = stringPreferencesKey("songName")
    val artist      = stringPreferencesKey("artist")
    val isPlaying   = booleanPreferencesKey("isPlaying")
    val artworkPath = stringPreferencesKey("artworkPath")
    val duration    = longPreferencesKey("duration")
    val position    = longPreferencesKey("position")
}
