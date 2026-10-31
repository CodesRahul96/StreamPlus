package com.codesrahul.streamplus.ui.player.components

import android.app.Activity
import android.app.PictureInPictureParams
import android.os.Build
import android.util.Rational
import androidx.annotation.RequiresApi

object PlayerPipHelper {

    fun supportsPip(activity: Activity): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                activity.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    fun buildPipParams(aspectRatio: Rational? = null): PictureInPictureParams? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        val builder = PictureInPictureParams.Builder()
        if (aspectRatio != null && !aspectRatio.isZero && !aspectRatio.isInfinite && !aspectRatio.isNaN) {
            val clampedRatio = when {
                aspectRatio.toFloat() < 0.418410f -> Rational(418, 1000)
                aspectRatio.toFloat() > 2.390000f -> Rational(2390, 1000)
                else -> aspectRatio
            }
            try {
                builder.setAspectRatio(clampedRatio)
            } catch (_: Exception) {}
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(true)
            builder.setSeamlessResizeEnabled(true)
        }
        return builder.build()
    }

    fun enterPipMode(activity: Activity, videoWidth: Int = 0, videoHeight: Int = 0): Boolean {
        if (!supportsPip(activity)) return false
        val ratio = if (videoWidth > 0 && videoHeight > 0) Rational(videoWidth, videoHeight) else Rational(16, 9)
        val params = buildPipParams(ratio)
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && params != null) {
                activity.enterPictureInPictureMode(params)
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
