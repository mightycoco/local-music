package com.localmusic.player.data.saf

import com.localmusic.player.playlist.FakeSharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedPreferencesSafFolderSourceStoreTest {
    @Test
    fun removeDeletesAnExistingFolderAndRetainsOtherSources() {
        val store = SharedPreferencesSafFolderSourceStore(FakeSharedPreferences())
        store.add("content://tree/first")
        store.add("content://tree/second")

        assertTrue(store.remove("content://tree/first"))
        assertEquals(listOf("content://tree/second"), store.folders())
        assertFalse(store.remove("content://tree/missing"))
    }
}
