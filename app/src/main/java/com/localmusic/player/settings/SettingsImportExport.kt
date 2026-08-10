package com.localmusic.player.settings

/** Lightweight settings import/export format for source-first local backups. */
class SettingsImportExport {
    fun export(bundle: SettingsBundle): String = buildString {
        appendLine("folderUris=${bundle.folderUris.joinToString(";")}")
        appendLine("carModeEnabled=${bundle.carModeEnabled}")
        appendLine("lastSelectedSort=${bundle.lastSelectedSort}")
        appendLine("externalArtworkDownloadEnabled=${bundle.externalArtworkDownloadEnabled}")
        appendLine("visualizerPreferred=${bundle.visualizerPreferred}")
    }

    fun parse(content: String): SettingsBundle {
        val values =
                content.lineSequence()
                        .mapNotNull { line ->
                            val key = line.substringBefore('=', missingDelimiterValue = "")
                            val value = line.substringAfter('=', missingDelimiterValue = "")
                            if (key.isBlank()) null else key to value
                        }
                        .toMap()

        return SettingsBundle(
                folderUris = values["folderUris"]?.split(';')?.filter { it.isNotBlank() }.orEmpty(),
                carModeEnabled = values["carModeEnabled"].toBoolean(),
                lastSelectedSort = values["lastSelectedSort"].orEmpty(),
                externalArtworkDownloadEnabled =
                        values["externalArtworkDownloadEnabled"]?.toBoolean() ?: true,
                visualizerPreferred = values["visualizerPreferred"]?.toBoolean() ?: false
        )
    }
}
