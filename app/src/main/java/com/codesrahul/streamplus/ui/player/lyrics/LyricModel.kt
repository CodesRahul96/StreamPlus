package com.codesrahul.streamplus.ui.player.lyrics

data class LyricLine(
    val timestampMs: Long, // -1 if unsynchronized
    val text: String
)

data class LyricsData(
    val lines: List<LyricLine>,
    val isSynchronized: Boolean,
    val title: String? = null,
    val artist: String? = null
) {
    fun getActiveIndex(currentPositionMs: Long): Int {
        if (!isSynchronized || lines.isEmpty()) return -1
        for (i in lines.indices.reversed()) {
            if (currentPositionMs >= lines[i].timestampMs) {
                return i
            }
        }
        return 0
    }
}
