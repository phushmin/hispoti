package com.phush.hispoti.host.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FingerprintTest {

    // Real Chrome on Android headers: the grease brand, its version and the order follow the major.
    @Test fun `brands match what Chrome on Android sends`() {
        val real = mapOf(
            116 to "\"Chromium\";v=\"116\", \"Not)A;Brand\";v=\"24\", \"Google Chrome\";v=\"116\"",
            120 to "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\"",
            124 to "\"Chromium\";v=\"124\", \"Google Chrome\";v=\"124\", \"Not-A.Brand\";v=\"99\"",
            131 to "\"Google Chrome\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"",
        )
        real.forEach { (major, header) ->
            assertEquals(header, Fingerprint.brandsOf("$major.0.0.0").joinToString(", ") { "\"${it.name}\";v=\"${it.major}\"" })
        }
    }

    @Test fun `the identity is Android and mobile`() {
        val f = Fingerprint("ua", "141.0.7390.122", "14")
        assertTrue(f.mobile)
        assertEquals("Android", f.platform)
        assertEquals("141.0.7390.122", f.fullVersion)
        assertTrue(f.brands.filter { it.name != "Chromium" && it.name != "Google Chrome" }.all { it.fullVersion.endsWith(".0.0.0") })
    }

    @Test fun `an unreadable WebView version falls back to a plausible Chrome`() {
        val (major, build, _) = Fingerprint.parseEngine("not a version") ?: Triple(0, 0, 0)
        assertEquals(0, major)
        assertEquals(0, build)
        assertNull(Fingerprint.parseEngine("141.0"))
        assertNull(Fingerprint.parseEngine(null))
        val fallback = Fingerprint.parseEngine("155.0.8160.0")
        assertTrue(fallback != null && fallback.first in 100..300 && fallback.second > 0)
    }

    @Test fun `accept-language is written the way Chromium writes it`() {
        assertEquals("fr-CH,fr;q=0.9,en;q=0.8", Fingerprint.acceptLanguage(listOf("fr-CH", "fr", "en")))
        assertEquals("en", Fingerprint.acceptLanguage(listOf("en")))
        assertFalse(Fingerprint.acceptLanguage(List(12) { "l$it" }).contains(";q=0.0"))
    }
}