package com.codesrahul.streamplus.data.player

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.mp4.Mp4Extractor
import androidx.media3.extractor.ts.AdtsExtractor
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider

@UnstableApi
object MediaCodecConfig {

    /**
     * Creates an optimized, all-format ExtractorsFactory supporting:
     * - MPEG-TS (DTS audio, non-IDR keyframes, access units)
     * - MP4 / Fragmented MP4 (motion metadata, tfdt workaround, sample directories)
     * - Matroska / WebM (MKV, WebM with all audio/subtitle streams)
     * - ADTS / AAC (constant bitrate seeking)
     * - FLV, AVI, OGG, WAV, FLAC, AMR
     * - Universal Subtitle Transcoding (SRT, ASS, VTT, TTML)
     */
    fun createUniversalExtractorsFactory(): DefaultExtractorsFactory {
        return DefaultExtractorsFactory().apply {
            // Disable constant bitrate seeking so progressive live streams (TS / ADTS) are never artificially capped with a fake duration
            setConstantBitrateSeekingEnabled(false)
            
            // TS Extractor: Allow non-IDR keyframes and DTS streams (Do NOT detect access units as many IPTV streams lack AUDs)
            val tsFlags = DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES or
                    DefaultTsPayloadReaderFactory.FLAG_ENABLE_HDMV_DTS_AUDIO_STREAMS
            setTsExtractorFlags(tsFlags)
            
            // MP4 / MOV / M4A Extractor: ignore invalid edit lists for better playback sync
            setMp4ExtractorFlags(Mp4Extractor.FLAG_WORKAROUND_IGNORE_EDIT_LISTS)
            
            // Universal subtitle transcoding: converts SubRip, ASS/SSA, TTML into standard Cues
            setTextTrackTranscodingEnabled(true)
        }
    }

    /**
     * Creates a high-performance RenderersFactory with:
     * - Automatic Hardware -> Software Decoder Fallback (avoids black screens on high profile HEVC/AV1)
     * - Tunneling / Audio track playback params support
     * - Configurable extension renderer mode (Prefer, On, Off)
     */
    fun createRenderersFactory(
        context: Context,
        decoderPriority: Int,
        enableTunneling: Boolean = false
    ): RenderersFactory {
        return DefaultRenderersFactory(context).apply {
            setExtensionRendererMode(when (decoderPriority) {
                1 -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                2 -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
                else -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
            })
            
            // Auto fallback to Google/c2 software decoders if hardware crashes or returns unsupported
            setEnableDecoderFallback(true)
            
            if (enableTunneling) {
                setEnableAudioTrackPlaybackParams(true)
            }
        }
    }

    /**
     * Builds a MediaSourceFactory with universal format support and DRM session management.
     */
    fun createMediaSourceFactory(
        context: Context,
        dataSourceFactory: DataSource.Factory,
        drmSessionManagerProvider: DrmSessionManagerProvider?
    ): DefaultMediaSourceFactory {
        val extractorsFactory = createUniversalExtractorsFactory()
        val factory = DefaultMediaSourceFactory(context, extractorsFactory)
            .setDataSourceFactory(dataSourceFactory)
        
        if (drmSessionManagerProvider != null) {
            factory.setDrmSessionManagerProvider(drmSessionManagerProvider)
        }
        return factory
    }
}
