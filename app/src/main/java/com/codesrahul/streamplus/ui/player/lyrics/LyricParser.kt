package com.codesrahul.streamplus.ui.player.lyrics

import android.content.Context
import android.net.Uri
import androidx.media3.common.Metadata
import androidx.media3.extractor.metadata.id3.CommentFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import java.io.File
import java.util.regex.Pattern

object LyricParser {
    // Regex for LRC timestamp: [mm:ss.xx] or [mm:ss.xxx] or [mm:ss]
    private val LRC_TIMESTAMP_REGEX = Pattern.compile("\\[(\\d{1,2}):(\\d{2})(?:\\.(\\d{1,3}))?\\]")

    /**
     * Parses standard LRC text or plain unsynced lyric text
     */
    fun parseLrc(content: String): LyricsData {
        val lines = mutableListOf<LyricLine>()
        var title: String? = null
        var artist: String? = null
        var hasTimestamp = false

        content.lineSequence().forEach { rawLine ->
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) return@forEach

            // Check metadata tags: [ti:Title], [ar:Artist]
            if (trimmed.startsWith("[ti:", ignoreCase = true) && trimmed.endsWith("]")) {
                title = trimmed.substring(4, trimmed.length - 1).trim()
                return@forEach
            }
            if (trimmed.startsWith("[ar:", ignoreCase = true) && trimmed.endsWith("]")) {
                artist = trimmed.substring(4, trimmed.length - 1).trim()
                return@forEach
            }

            val matcher = LRC_TIMESTAMP_REGEX.matcher(trimmed)
            val timestamps = mutableListOf<Long>()
            var lastEnd = 0

            while (matcher.find()) {
                hasTimestamp = true
                val minutes = matcher.group(1)?.toLongOrNull() ?: 0L
                val seconds = matcher.group(2)?.toLongOrNull() ?: 0L
                val millisRaw = matcher.group(3)
                val millis = when {
                    millisRaw == null -> 0L
                    millisRaw.length == 1 -> (millisRaw.toLongOrNull() ?: 0L) * 100
                    millisRaw.length == 2 -> (millisRaw.toLongOrNull() ?: 0L) * 10
                    else -> millisRaw.take(3).toLongOrNull() ?: 0L
                }
                val totalMs = (minutes * 60 + seconds) * 1000 + millis
                timestamps.add(totalMs)
                lastEnd = matcher.end()
            }

            val lyricText = if (timestamps.isNotEmpty()) {
                trimmed.substring(lastEnd).trim()
            } else {
                trimmed
            }

            if (timestamps.isNotEmpty()) {
                for (ts in timestamps) {
                    if (lyricText.isNotEmpty()) {
                        lines.add(LyricLine(ts, lyricText))
                    }
                }
            } else if (!trimmed.startsWith("[")) {
                lines.add(LyricLine(-1L, trimmed))
            }
        }

        lines.sortBy { if (it.timestampMs >= 0) it.timestampMs else Long.MAX_VALUE }
        return LyricsData(
            lines = lines,
            isSynchronized = hasTimestamp && lines.any { it.timestampMs >= 0 },
            title = title,
            artist = artist
        )
    }

    /**
     * Looks for a companion .lrc file next to the audio file
     */
    fun findCompanionLrcFile(context: Context, audioUri: Uri): String? {
        try {
            if (audioUri.scheme == "file") {
                val path = audioUri.path ?: return null
                val base = path.substringBeforeLast('.')
                val lrcFile = File("$base.lrc")
                if (lrcFile.exists() && lrcFile.canRead()) {
                    return lrcFile.readText()
                }
            } else if (audioUri.scheme == "content") {
                val projection = arrayOf(android.provider.MediaStore.MediaColumns.DATA)
                context.contentResolver.query(audioUri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val path = cursor.getString(0)
                        if (!path.isNullOrBlank()) {
                            val base = path.substringBeforeLast('.')
                            val lrcFile = File("$base.lrc")
                            if (lrcFile.exists() && lrcFile.canRead()) {
                                return lrcFile.readText()
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore file read error
        }
        return null
    }

    /**
     * Extracts lyrics from ExoPlayer / Media3 metadata frames
     */
    fun extractLyricsFromMetadata(metadata: Metadata): String? {
        for (i in 0 until metadata.length()) {
            val entry = metadata.get(i)
            when (entry) {
                is CommentFrame -> {
                    if (entry.description?.contains("lyrics", ignoreCase = true) == true ||
                        entry.id.equals("USLT", ignoreCase = true)
                    ) {
                        return entry.text
                    }
                }
                is TextInformationFrame -> {
                    if (entry.id.equals("USLT", ignoreCase = true) ||
                        entry.id.equals("SYLT", ignoreCase = true) ||
                        entry.description?.contains("lyrics", ignoreCase = true) == true
                    ) {
                        return entry.values.firstOrNull() ?: entry.description
                    }
                }
                is VorbisComment -> {
                    if (entry.key.equals("LYRICS", ignoreCase = true) ||
                        entry.key.equals("UNSYNCEDLYRICS", ignoreCase = true)
                    ) {
                        return entry.value
                    }
                }
            }
        }
        return null
    }

    /**
     * Returns demo synchronized lyrics for testing & verification
     */
    fun getSampleLrc(title: String, artist: String): String {
        return """
            [ti:$title]
            [ar:$artist]
            [00:02.00] In the neon glow of midnight city lights
            [00:06.50] Signals pulse across the cyber night
            [00:11.00] StreamPlus echoes in lossless sound
            [00:16.00] The rhythm takes us off the ground
            [00:21.00] Feel the bass line through the floor
            [00:26.00] Pushing higher, wanting more
            [00:31.50] Pure fidelity, pure frequency
            [00:37.00] Flowing through reality
            [00:43.00] Synthesizers fill the air
            [00:48.50] Sonic waves without a care
            [00:54.00] Listen closely to the beat
            [00:59.50] Where music and the future meet
        """.trimIndent()
    }
}
