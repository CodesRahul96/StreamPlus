package com.codesrahul.streamplus.util

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import com.codesrahul.streamplus.R
import com.codesrahul.streamplus.SplashActivity
import com.codesrahul.streamplus.ui.player.PlayerActivity

object ShortcutHelper {
    private const val ID_PLAY_LAST = "play_last_channel"

    fun updateShortcuts(
        context: Context,
        streamTitle: String,
        streamUrl: String,
        channelLogo: String?,
        playlistId: Int,
        groupTitle: String
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return

        val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return
        
        // Intent that SplashActivity will handle
        val intent = Intent(context, SplashActivity::class.java).apply {
            action = "com.codesrahul.streamplus.ACTION_PLAY_LAST"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            
            // Pass all required player data
            putExtra("stream_url", streamUrl)
            putExtra("stream_title", streamTitle)
            putExtra("channel_logo", channelLogo)
            putExtra("EXTRA_PLAYLIST_ID", playlistId)
            putExtra("EXTRA_STREAM_GROUP", groupTitle)
        }

        val shortcut = ShortcutInfo.Builder(context, ID_PLAY_LAST)
            .setShortLabel(if (streamTitle.length > 10) streamTitle.take(10) + "..." else streamTitle)
            .setLongLabel("Play Last: $streamTitle")
            .setIcon(Icon.createWithResource(context, R.drawable.ic_play_shortcut)) // We'll need to ensure this drawable exists or use a default
            .setIntent(intent)
            .build()

        try {
            shortcutManager.dynamicShortcuts = listOf(shortcut)
        } catch (e: Exception) {
            // Handle cases where we might exceed limits
        }
    }
}
