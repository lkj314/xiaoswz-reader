package com.xiaoswz.reader.data.plugin

/**
 * 0.16.3 起废弃：官方高亮 / 书签 / 教程不再作为「插件」实体存在。
 * - 高亮 / 书签 → 解耦为阅读器内置标注能力（见 [PluginRepository.builtinAnnotationManifests]）
 * - 教程 → 纯文档（见 PluginPlazaScreen 教程 Tab）
 *
 * 保留此文件仅为过渡：[all] 恒为空，所有历史引用自然失效。后续版本可整体删除本文件。
 */
object BundledPlugins {
    val all: List<PluginManifest> = emptyList()
}
