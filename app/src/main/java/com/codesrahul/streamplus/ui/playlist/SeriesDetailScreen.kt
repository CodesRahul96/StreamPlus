package com.codesrahul.streamplus.ui.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import com.codesrahul.streamplus.ui.components.PremiumBackground
import com.codesrahul.streamplus.ui.components.shimmerEffect
import com.codesrahul.streamplus.ui.util.tvFocusable
import com.codesrahul.streamplus.ui.util.glassmorphic

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SeriesDetailScreen(
    playlistId: Int,
    seriesId: Int,
    seriesName: String,
    onBack: () -> Unit,
    viewModel: SeriesDetailViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val seriesInfo by viewModel.seriesInfo.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val playlist by viewModel.playlist.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current

    var selectedSeason by remember { mutableStateOf(1) }

    androidx.activity.compose.BackHandler(enabled = true) {
        onBack()
    }

    LaunchedEffect(playlistId, seriesId) {
        viewModel.loadSeriesInfo(playlistId, seriesId)
    }

    // Update selected season when data loads if season 1 doesn't exist
    LaunchedEffect(seriesInfo) {
        seriesInfo?.seasons?.firstOrNull()?.seasonNumber?.let {
            if (selectedSeason == 1 && seriesInfo?.seasons?.any { it.seasonNumber == 1 } == false) {
                selectedSeason = it
            }
        }
    }

    PremiumBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    windowInsets = WindowInsets(0.dp),
                    title = { Text(seriesName, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (isLoading) {
                    ShimmerSeriesDetail()
                } else if (error != null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(error!!, color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    seriesInfo?.let { info ->
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Backdrop and Info Header
                            Box(modifier = Modifier.height(200.dp).fillMaxWidth()) {
                                val sharedTransitionModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                                    with(sharedTransitionScope) {
                                        Modifier.sharedElement(
                                            rememberSharedContentState(key = "series_cover_${seriesId}"),
                                            animatedVisibilityScope = animatedVisibilityScope
                                        )
                                    }
                                } else {
                                    Modifier
                                }

                                AsyncImage(
                                    model = info.info?.cover ?: "",
                                    contentDescription = null,
                                    modifier = Modifier
                                        .then(sharedTransitionModifier)
                                        .fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    alpha = 0.4f
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color.Transparent, Color.Black.copy(0.7f))
                                            )
                                        )
                                )
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        info.info?.name ?: seriesName,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    info.info?.genre?.let {
                                        Text(it, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
                                    }
                                }
                            }

                            // Season Selection
                            if (!info.seasons.isNullOrEmpty()) {
                                LazyRow(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(info.seasons, key = { it.seasonNumber ?: it.id ?: 0 }) { season ->
                                        val isSelected = selectedSeason == season.seasonNumber
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedSeason = season.seasonNumber ?: 1 },
                                            label = { Text("Season ${season.seasonNumber}") },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }
                                }
                            }

                            // Episode List
                            val episodes = info.episodes?.get(selectedSeason.toString()) ?: emptyList()
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(episodes, key = { it.id }) { episode ->
                                    EpisodeItem(
                                        episode = episode,
                                        onClick = {
                                            playlist?.let { p ->
                                                val username = android.net.Uri.encode(p.username ?: "")
                                                val password = android.net.Uri.encode(p.password ?: "")
                                                val serverInfoUrl = info.info?.let { null } // We don't have serverInfo as a separate field here yet, but we can pass it if we add it.
                                                // Wait, XtreamSeriesInfo doesn't have ServerInfo. We get it from Authentication.
                                                // For now, getStreamBaseUrl handles it if we provide the raw host.
                                                
                                                val serverBaseUrl = viewModel.getStreamBaseUrl(p, null) 
                                                // Support adaptive paths in Series Detail
                                                val isShortPath = serverBaseUrl.contains(Regex("/[^/]+/[^/]+/[0-9]+", RegexOption.IGNORE_CASE))
                                                
                                                val ext = episode.containerExtension ?: "mp4"
                                                val streamUrl = if (isShortPath) {
                                                    "$serverBaseUrl/$username/$password/${episode.id}"
                                                } else {
                                                    "$serverBaseUrl/series/$username/$password/${episode.id}.$ext"
                                                }
                                                
                                                val intent = android.content.Intent(context, com.codesrahul.streamplus.ui.player.PlayerActivity::class.java).apply {
                                                    putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_STREAM_URL, streamUrl)
                                                    putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_STREAM_TITLE, episode.title ?: "Episode ${episode.episodeNum}")
                                                    putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_USER_AGENT, p.userAgent)
                                                    putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_PLAYLIST_ID, p.id)
                                                }
                                                context.startActivity(intent)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EpisodeItem(
    episode: com.codesrahul.streamplus.data.remote.model.XtreamEpisode,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusable(cornerRadius = 12.dp)
            .clickable(onClick = onClick)
            .glassmorphic(cornerRadius = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = null,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp, 45.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "E${episode.episodeNum}: ${episode.title ?: "Untitled"}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                episode.info?.duration?.let {
                    Text(text = "Duration: $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun ShimmerSeriesDetail() {
    Column(modifier = Modifier.fillMaxSize()) {
        // 1. Backdrop shimmer
        Box(
            modifier = Modifier
                .height(200.dp)
                .fillMaxWidth()
                .shimmerEffect()
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // 2. Season chips shimmers
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(100.dp, 36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .shimmerEffect()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Episode list shimmers
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(5) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = null,
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().glassmorphic(cornerRadius = 12.dp)) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp, 45.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .shimmerEffect()
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.6f)
                                        .height(16.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .shimmerEffect()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.3f)
                                        .height(12.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .shimmerEffect()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

