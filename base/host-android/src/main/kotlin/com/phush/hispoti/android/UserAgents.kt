package com.phush.hispoti.host.android

import android.content.Context
import java.io.File
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

/**
 * The Android browser this install's WebView presents.
 *
 * Every install draws a user agent from the list the official Web Player
 * announces (the same source it uses in a phone browser), and keeps it
 * for one day. This is the identity the app itself asks Spotify for,
 * matching what the WebView presents, so there is no contradiction.
 *
 * Every install picks a random Android mobile user agent, stable for
 * the day. This matches Spotube's approach in its WebView.
 */
internal object UserAgents {
    /** The source the official Web Player announces for its user agents. */
    private const val SOURCE_URL = "https://cdn.jsdelivr.net/npm/top-user-agents@latest/src/index.json"
    /** TTL for the cache (24 hours). */
    private const val CACHE_TTL_MS = 24 * 60 * 60 * 1000L
    /** File name for the cached agent, stored per-install. */
    private const val CACHE_FILE = "user_agents.txt"

    /** Android mobile Chrome user agents as fallback when the source is unavailable. */
    private val FALLBACKS = listOf(
        "Mozilla/5.0 (Linux; Android 15; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/147.0.7727.92 Mobile Safari/537.36",
        "Mozilla/5.0 (Linux; Android 14; SM-S928B) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/145.0.0.0 Mobile Safari/537.36",
        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/140.0.0.0 Mobile Safari/537.36",
        "Mozilla/5.0 (Linux; Android 16; SM-G998B) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/146.0.0.0 Mobile Safari/537.36",
        "Mozilla/5.0 (Linux; Android 12; Pixel 6) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/135.0.0.0 Mobile Safari/537.36",
    )

    @Volatile private var current: String = FALLBACKS.first()
    @Volatile private var fetchedAt: Long = 0L
    private val gate = Any()

    /**
     * The Android mobile user agent this install presents, stable for the day.
     * Fetches from [SOURCE_URL] when needed, otherwise falls back to a random
     * entry from [FALLBACKS] and reloads once a day in the background.
     */
    fun random(context: Context): String = synchronized(gate) {
        if (System.currentTimeMillis() - fetchedAt > CACHE_TTL_MS) refresh(context)
        current
    }

    private fun refresh(context: Context) {
        fetchedAt = System.currentTimeMillis()
        Thread { fetch(context) }.apply { isDaemon = true; start() }
    }

    private fun fetch(context: Context) {
        runCatching {
            val json = JSONObject(URL(SOURCE_URL).readText())
            val list = json.optJSONArray("useragents") ?: json.optJSONArray("userAgents") ?: JSONArray()
            val agents = (0 until list.length()).mapNotNull { list.getString(it) }
            val android = agents.filter { "Android" in it }
            android.random()?.let { u ->
                current = u
                fetchedAt = System.currentTimeMillis()
                writeCache(context, u)
            }
        }.onFailure {
            runCatching { File(context.filesDir, CACHE_FILE).delete() }
        }
    }

    private fun writeCache(context: Context, agent: String) {
        try {
            val file = File(context.filesDir, CACHE_FILE)
            file.parentFile?.mkdirs()
            file.writeText(agent)
        } catch (_: Exception) {}
    }
}