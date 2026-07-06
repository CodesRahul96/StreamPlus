package com.codesrahul.streamplus.util

import android.app.Activity
import android.os.Build
import android.view.Display
import android.view.Surface
import android.view.ViewTreeObserver
import android.view.WindowManager

object RefreshRateHelper {
    /**
     * Enables the highest refresh rate available on the device's display, 
     * supporting up to 165Hz/fps smoothness.
     */
    fun enableHighRefreshRate(activity: Activity) {
        try {
            val window = activity.window ?: return
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val display = try {
                    activity.display
                } catch (e: NoSuchMethodError) {
                    @Suppress("DEPRECATION")
                    activity.windowManager.defaultDisplay
                }
                
                var maxMode: Display.Mode? = null
                var maxRefresh = 0f
                display?.supportedModes?.forEach { mode ->
                    if (mode.refreshRate > maxRefresh) {
                        maxRefresh = mode.refreshRate
                        maxMode = mode
                    }
                }
                
                maxMode?.let { mode ->
                    val layoutParams = window.attributes
                    layoutParams.preferredDisplayModeId = mode.modeId
                    layoutParams.preferredRefreshRate = mode.refreshRate
                    window.attributes = layoutParams
                }
            } else {
                @Suppress("DEPRECATION")
                val display = activity.windowManager.defaultDisplay
                var maxModeId = 0
                var maxRefreshRate = 0f
                @Suppress("DEPRECATION")
                display?.supportedModes?.forEach { mode ->
                    if (mode.refreshRate > maxRefreshRate) {
                        maxRefreshRate = mode.refreshRate
                        maxModeId = mode.modeId
                    }
                }
                if (maxModeId != 0) {
                    val params = window.attributes
                    params.preferredDisplayModeId = maxModeId
                    params.preferredRefreshRate = maxRefreshRate
                    window.attributes = params
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}
