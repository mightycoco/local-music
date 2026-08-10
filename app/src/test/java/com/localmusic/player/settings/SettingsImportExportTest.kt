package com.localmusic.player.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsImportExportTest {
    private val codec = SettingsImportExport()

    @Test
    fun roundTripSettingsBundle() {
        val bundle =
                SettingsBundle(
                        folderUris = listOf("content://tree/music", "content://tree/downloads"),
                        carModeEnabled = true,
                        lastSelectedSort = "NewestAdded",
                        externalArtworkDownloadEnabled = false,
                        visualizerPreferred = true
                )

        assertEquals(bundle, codec.parse(codec.export(bundle)))
    }

    @Test
    fun missingExternalArtworkSettingDefaultsToEnabled() {
        val parsed = codec.parse("folderUris=\ncarModeEnabled=false\nlastSelectedSort=Name\n")

        assertEquals(true, parsed.externalArtworkDownloadEnabled)
        assertEquals(false, parsed.visualizerPreferred)
    }
}
