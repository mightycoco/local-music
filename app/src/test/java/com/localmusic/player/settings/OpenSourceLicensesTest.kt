package com.localmusic.player.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenSourceLicensesTest {
    @Test
    fun noticesCoverRuntimeDependenciesWithApacheTerms() {
        val notices = OpenSourceLicenses.notices

        assertEquals(3, notices.size)
        assertTrue(notices.all { it.licenseName == "Apache License 2.0" })
        assertTrue(notices.all { it.licenseText.contains("Apache License") })
    }
}
