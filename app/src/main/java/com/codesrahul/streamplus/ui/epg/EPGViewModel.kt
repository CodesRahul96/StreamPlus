package com.codesrahul.streamplus.ui.epg

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codesrahul.streamplus.data.epg.EPGManager
import com.codesrahul.streamplus.data.epg.EPGProgram
import com.codesrahul.streamplus.data.local.PlaylistEntity
import com.codesrahul.streamplus.domain.model.M3UItem
import com.codesrahul.streamplus.domain.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class EPGGridUiState(
    val isLoading: Boolean = true,
    val playlists: List<PlaylistEntity> = emptyList(),
    val selectedPlaylist: PlaylistEntity? = null,
    val categories: List<String> = emptyList(),
    val selectedCategory: String = "All",
    val channels: List<M3UItem> = emptyList(),
    val channelPrograms: Map<String, List<EPGProgram>> = emptyMap(),
    val timelineStart: Long = 0L,
    val timelineEnd: Long = 0L,
    val focusedProgram: EPGProgram? = null,
    val focusedChannel: M3UItem? = null,
    val currentTime: Long = System.currentTimeMillis(),
    val favoriteKeys: Set<String> = emptySet()
)

@HiltViewModel
class EPGViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val epgManager: EPGManager,
    private val favoriteDao: com.codesrahul.streamplus.data.local.FavoriteDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(EPGGridUiState())
    val uiState: StateFlow<EPGGridUiState> = _uiState.asStateFlow()

    private var allChannelsForPlaylist: List<M3UItem> = emptyList()

    init {
        initTimeline()
        observePlaylists()
        observeFavorites()
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            favoriteDao.getAllFavorites().collect { favList ->
                val keys = favList.map { "${it.name}_${it.groupTitle}_${it.playlistId}" }.toSet()
                _uiState.update { it.copy(favoriteKeys = keys) }
            }
        }
    }

    private fun initTimeline() {
        val now = System.currentTimeMillis()
        // Align timeline to the beginning of the current hour minus 2 hours (to show past/catchup)
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.HOUR_OF_DAY, -2)
        val start = cal.timeInMillis

        // 12 hours forward from start
        cal.add(Calendar.HOUR_OF_DAY, 12)
        val end = cal.timeInMillis

        _uiState.update { it.copy(timelineStart = start, timelineEnd = end, currentTime = now) }
    }

    private fun observePlaylists() {
        viewModelScope.launch {
            playlistRepository.getAllPlaylists().collect { playlists ->
                val active = _uiState.value.selectedPlaylist ?: playlists.firstOrNull()
                _uiState.update { it.copy(playlists = playlists, selectedPlaylist = active) }
                if (active != null) {
                    loadPlaylistChannels(active)
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun selectPlaylist(playlist: PlaylistEntity) {
        _uiState.update { it.copy(selectedPlaylist = playlist, isLoading = true) }
        loadPlaylistChannels(playlist)
    }

    private fun loadPlaylistChannels(playlist: PlaylistEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // If playlist has EPG URL configured, trigger EPG sync
                playlist.epgUrl?.let { url ->
                    if (url.isNotBlank()) {
                        epgManager.loadEPG(url)
                    }
                }

                val items = playlistRepository.loadPlaylist(
                    url = playlist.url,
                    userAgent = playlist.userAgent,
                    playlistId = playlist.id
                )
                // Filter to live channels
                val liveChannels = items.filter { !it.isVod }
                allChannelsForPlaylist = liveChannels

                val rawCategories = liveChannels.map { it.group }.filter { it.isNotBlank() }.distinct().sorted()
                val categories = listOf("All", "Favorites") + rawCategories
                _uiState.update { it.copy(categories = categories) }

                filterChannelsByCategory(_uiState.value.selectedCategory)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun selectCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
        filterChannelsByCategory(category)
    }

    private fun filterChannelsByCategory(category: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val filtered = when (category) {
                "All" -> allChannelsForPlaylist
                "Favorites" -> {
                    val favKeys = _uiState.value.favoriteKeys
                    allChannelsForPlaylist.filter { favKeys.contains("${it.name}_${it.group}_${it.playlistId}") }
                }
                else -> allChannelsForPlaylist.filter { it.group.equals(category, ignoreCase = true) }
            }

            val tStart = _uiState.value.timelineStart
            val tEnd = _uiState.value.timelineEnd

            // Map each channel URL to its programs in the [tStart, tEnd] window
            val programMap = mutableMapOf<String, List<EPGProgram>>()
            for (channel in filtered) {
                val epgId = channel.epgId ?: channel.name
                val progs = epgManager.getProgramsForChannel(epgId, tStart, tEnd)
                programMap[channel.url] = fillGapsWithNoInfo(progs, tStart, tEnd)
            }

            val firstCh = filtered.firstOrNull()
            val initialProg = firstCh?.let { ch ->
                val chProgs = programMap[ch.url]
                chProgs?.find { it.isLive } ?: chProgs?.firstOrNull()
            }

            _uiState.update {
                it.copy(
                    channels = filtered,
                    channelPrograms = programMap,
                    isLoading = false,
                    focusedChannel = firstCh,
                    focusedProgram = initialProg
                )
            }
        }
    }

    /**
     * Synthesizes 'No Information' blocks for timeline gaps where no XMLTV guide exists.
     */
    private fun fillGapsWithNoInfo(
        existing: List<EPGProgram>,
        start: Long,
        end: Long
    ): List<EPGProgram> {
        if (existing.isEmpty()) {
            return listOf(
                EPGProgram(
                    title = "No Information",
                    start = start,
                    stop = end,
                    description = "Program details not provided by EPG source"
                )
            )
        }

        val result = mutableListOf<EPGProgram>()
        var cursor = start

        for (prog in existing) {
            if (prog.start > cursor) {
                // Gap detected before this program
                result.add(
                    EPGProgram(
                        title = "No Information",
                        start = cursor,
                        stop = prog.start,
                        description = "Program details not provided by EPG source"
                    )
                )
            }
            result.add(prog)
            cursor = prog.stop
        }

        if (cursor < end) {
            result.add(
                EPGProgram(
                    title = "No Information",
                    start = cursor,
                    stop = end,
                    description = "Program details not provided by EPG source"
                )
            )
        }

        return result
    }

    fun toggleFavorite(channel: M3UItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val isFav = favoriteDao.isFavorite(channel.name, channel.group, channel.playlistId)
            if (isFav) {
                favoriteDao.removeFavorite(channel.name, channel.group, channel.playlistId)
            } else {
                favoriteDao.addFavorite(
                    com.codesrahul.streamplus.data.local.FavoriteEntity(
                        name = channel.name,
                        groupTitle = channel.group,
                        playlistId = channel.playlistId,
                        url = channel.url,
                        logoUrl = channel.logoUrl,
                        userAgent = channel.userAgent,
                        headers = channel.headers,
                        licenseUrl = channel.licenseUrl,
                        drmScheme = channel.drmScheme,
                        drmHeaders = channel.drmHeaders,
                        streamType = channel.streamType
                    )
                )
            }
        }
    }

    fun isChannelFavorite(channel: M3UItem): Boolean {
        return _uiState.value.favoriteKeys.contains("${channel.name}_${channel.group}_${channel.playlistId}")
    }

    fun setFocusedItem(channel: M3UItem, program: EPGProgram) {
        _uiState.update { it.copy(focusedChannel = channel, focusedProgram = program) }
    }

    fun refreshTime() {
        _uiState.update { it.copy(currentTime = System.currentTimeMillis()) }
    }
}
