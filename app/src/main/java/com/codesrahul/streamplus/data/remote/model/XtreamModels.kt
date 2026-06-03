package com.codesrahul.streamplus.data.remote.model

import com.google.gson.annotations.SerializedName

data class XtreamAuthResponse(
    @SerializedName("user_info") val userInfo: XtreamUserInfo?,
    @SerializedName("server_info") val serverInfo: XtreamServerInfo?
)

data class XtreamUserInfo(
    @SerializedName("username") val username: String?,
    @SerializedName("password") val password: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("auth") val auth: Int?,
    @SerializedName("status") val status: String?,
    @SerializedName("exp_date") val expDate: String?,
    @SerializedName("is_trial") val isTrial: String?,
    @SerializedName("active_cons") val activeCons: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("max_connections") val maxConnections: String?,
    @SerializedName("allowed_output_formats") val allowedOutputFormats: List<String>?
)

data class XtreamServerInfo(
    @SerializedName("url") val url: String?,
    @SerializedName("port") val port: String?,
    @SerializedName("https_port") val httpsPort: String?,
    @SerializedName("server_protocol") val serverProtocol: String?,
    @SerializedName("rtmp_port") val rtmpPort: String?,
    @SerializedName("timezone") val timezone: String?,
    @SerializedName("timestamp_now") val timestampNow: Long?,
    @SerializedName("time_now") val timeNow: String?
)

data class XtreamCategory(
    @SerializedName("category_id") val categoryId: String,
    @SerializedName("category_name") val categoryName: String,
    @SerializedName("parent_id") val parentId: Int?
)

data class XtreamLiveStream(
    @SerializedName("num") val num: Int?,
    @SerializedName("name") val name: String,
    @SerializedName("stream_type") val streamType: String?,
    @SerializedName("stream_id") val streamId: Int,
    @SerializedName("stream_icon") val streamIcon: String?,
    @SerializedName("epg_channel_id") val epgChannelId: String?,
    @SerializedName("added") val added: String?,
    @SerializedName("category_id") val categoryId: String?,
    @SerializedName("custom_sid") val customSid: String?,
    @SerializedName("tv_archive") val tvArchive: Int?,
    @SerializedName("direct_source") val directSource: String?,
    @SerializedName("tv_archive_duration") val tvArchiveDuration: Int?
)

data class XtreamVodStream(
    @SerializedName("num") val num: Int?,
    @SerializedName("name") val name: String,
    @SerializedName("stream_type") val streamType: String?,
    @SerializedName("stream_id") val streamId: Int,
    @SerializedName("stream_icon") val streamIcon: String?,
    @SerializedName("rating") val rating: Double?,
    @SerializedName("rating_5based") val rating5Based: Double?,
    @SerializedName("added") val added: String?,
    @SerializedName("category_id") val categoryId: String?,
    @SerializedName("container_extension") val containerExtension: String?,
    @SerializedName("custom_sid") val customSid: String?,
    @SerializedName("direct_source") val directSource: String?
)

data class XtreamSeriesStream(
    @SerializedName("num") val num: Int?,
    @SerializedName("name") val name: String,
    @SerializedName("series_id") val seriesId: Int,
    @SerializedName("cover") val cover: String?,
    @SerializedName("plot") val plot: String?,
    @SerializedName("cast") val cast: String?,
    @SerializedName("director") val director: String?,
    @SerializedName("genre") val genre: String?,
    @SerializedName("releaseDate") val releaseDate: String?,
    @SerializedName("last_modified") val lastModified: String?,
    @SerializedName("rating") val rating: String?,
    @SerializedName("rating_5based") val rating5Based: Double?,
    @SerializedName("backdrop_path") val backdropPath: List<String>?,
    @SerializedName("youtube_trailer") val youtubeTrailer: String?,
    @SerializedName("episode_run_time") val episodeRunTime: String?,
    @SerializedName("category_id") val categoryId: String?
)

data class XtreamSeriesInfo(
    @SerializedName("seasons") val seasons: List<XtreamSeason>?,
    @SerializedName("info") val info: XtreamSeriesDetails?,
    @SerializedName("episodes") val episodes: Map<String, List<XtreamEpisode>>?
)

data class XtreamSeason(
    @SerializedName("air_date") val airDate: String?,
    @SerializedName("episode_count") val episodeCount: Int?,
    @SerializedName("id") val id: Int?,
    @SerializedName("name") val name: String?,
    @SerializedName("overview") val overview: String?,
    @SerializedName("season_number") val seasonNumber: Int?,
    @SerializedName("cover") val cover: String?,
    @SerializedName("cover_big") val coverBig: String?
)

data class XtreamSeriesDetails(
    @SerializedName("name") val name: String?,
    @SerializedName("cover") val cover: String?,
    @SerializedName("plot") val plot: String?,
    @SerializedName("cast") val cast: String?,
    @SerializedName("director") val director: String?,
    @SerializedName("genre") val genre: String?,
    @SerializedName("releaseDate") val releaseDate: String?,
    @SerializedName("last_modified") val lastModified: String?,
    @SerializedName("rating") val rating: String?,
    @SerializedName("rating_5based") val rating5Based: Double?,
    @SerializedName("backdrop_path") val backdropPath: List<String>?,
    @SerializedName("youtube_trailer") val youtubeTrailer: String?,
    @SerializedName("episode_run_time") val episodeRunTime: String?,
    @SerializedName("category_id") val categoryId: String?
)

data class XtreamEpisode(
    @SerializedName("id") val id: String,
    @SerializedName("episode_num") val episodeNum: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("container_extension") val containerExtension: String?,
    @SerializedName("info") val info: XtreamEpisodeInfo?,
    @SerializedName("custom_sid") val customSid: String?,
    @SerializedName("added") val added: String?,
    @SerializedName("season") val season: Int?,
    @SerializedName("direct_source") val directSource: String?
)

data class XtreamEpisodeInfo(
    @SerializedName("plot") val plot: String?,
    @SerializedName("duration_secs") val durationSecs: Int?,
    @SerializedName("duration") val duration: String?,
    @SerializedName("video") val video: Map<String, Any>?,
    @SerializedName("audio") val audio: Map<String, Any>?,
    @SerializedName("bitrate") val bitrate: Int?,
    @SerializedName("rating") val rating: String?,
    @SerializedName("season") val season: String?
)
