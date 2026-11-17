package com.phush.hispoti.host.android

import android.content.Context
import androidx.webkit.WebViewCompat

/**
 * The browser identity this install presents to Spotify: a real Android mobile Chrome, matching
 * what the WebView presents. The user agent comes from [UserAgents]; the version hints follow the
 * WebView build; everything else is derived like Chrome on Android would write it.
 *
 * Each install draws its user agent once, keeps it stable for a day, and never changes it mid-run.
 * The WebView already reports Android/mobile client hints; these hints only echo what the WebView
 * already sends, so nothing contradicts what Spotify's network stack already knows.
 */
internal data class Fingerprint(
    val userAgent: String,
    val fullVersion: String,
    val platformVersion: String,
) {

    /** The brand list, matching Chrome on Android's sec-ch-ua brand ordering. */
    val brands: List<Brand> get() = brandsOf(fullVersion)

    /** The sec-ch-ua header these brands make, as Chromium writes it. */
    val secChUa: String get() = brands.joinToString(", ") { "\"${it.name}\";v=\"${it.major}\"" }

    /** Chrome on Android reports itself as mobile. */
    val mobile: Boolean get() = true

    /** Chrome on Android reports itself as Android. */
    val platform: String get() = "Android"

    data class Brand(val name: String, val major: String, val fullVersion: String)

    companion object {
        const val BACK_RELEASES = 2
        const val BUILDS_PER_MAJOR = 55

        /** Used only when the WebView's version cannot be read. */
        private const val FALLBACK_ENGINE = "155.0.8160.0"

        // Chromium's GREASE: the fake brand, its version and the order all follow from the major.
        private val GREASE_CHARS = listOf(" ", "(", ":", "-", ".", "/", ")", ";", "=", "?", "_")
        private val GREASE_VERSIONS = listOf("8", "99", "24")
        private val ORDERS = listOf(
            intArrayOf(0, 1, 2), intArrayOf(0, 2, 1), intArrayOf(1, 0, 2),
            intArrayOf(1, 2, 0), intArrayOf(2, 0, 1), intArrayOf(2, 1, 0),
        )

        /** A Chrome version string ("141.0.7390.122") as (major, build, patch); null if it is not one. */
        fun parseEngine(version: String?): Triple<Int, Int, Int>? {
            val parts = version?.trim()?.split('.')?.map { it.toIntOrNull() } ?: return null
            if (parts.size != 4 || parts.any { it == null }) return null
            val (major, _, build, patch) = parts.map { it!! }
            return if (major in 100..300 && build > 0) Triple(major, build, patch) else null
        }

        fun brandsOf(fullVersion: String): List<Brand> {
            val major = fullVersion.split('.').firstOrNull()?.toIntOrNull() ?: 0
            val greaseVersion = GREASE_VERSIONS[major % GREASE_VERSIONS.size]
            val grease = Brand(
                "Not${GREASE_CHARS[major % GREASE_CHARS.size]}A${GREASE_CHARS[(major + 1) % GREASE_CHARS.size]}Brand",
                greaseVersion,
                "$greaseVersion.0.0.0",
            )
            val order = ORDERS[major % ORDERS.size]
            val out = arrayOfNulls<Brand>(3)
            out[order[0]] = grease
            out[order[1]] = Brand("Chromium", "$major", fullVersion)
            out[order[2]] = Brand("Google Chrome", "$major", fullVersion)
            return out.map { it!! }
        }

        /** Chrome's own Accept-Language shape: the first language plain, then q=0.9, 0.8… down to 0.1. */
        fun acceptLanguage(tags: List<String>): String = tags.filter { it.isNotBlank() }
            .mapIndexed { i, tag -> if (i == 0) tag else "$tag;q=0.${maxOf(1, 10 - i)}" }
            .joinToString(",")

        /** The WebView's own build (whatever it reports) plus the Android platform version. */
        fun of(context: Context): Fingerprint {
            val engineVersion = runCatching {
                WebViewCompat.getCurrentWebViewPackage(context)?.versionName
            }.getOrNull()
            val (major, build, patch) = parseEngine(engineVersion) ?: parseEngine(FALLBACK_ENGINE)!!
            return Fingerprint(
                userAgent = UserAgents.random(context),
                fullVersion = "$major.$build.$patch",
                platformVersion = android.os.Build.VERSION.RELEASE,
            )
        }
    }
}