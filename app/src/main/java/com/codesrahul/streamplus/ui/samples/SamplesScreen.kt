package com.codesrahul.streamplus.ui.samples

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class SampleCategory(
    val title: String,
    val streams: List<SampleStream>
)

data class SampleStream(
    val title: String,
    val url: String,
    val drmScheme: String? = null,
    val drmLicense: String? = null
)

val sampleData = listOf(
    SampleCategory("Clear DASH", listOf(
        SampleStream("Angel One", "https://cwip-shaka-proxy.appspot.com/no_auth/angel_one.mpd"),
        SampleStream("Big Buck Bunny", "https://dash.akamaized.net/akamai/bbb_30fps/bbb_30fps.mpd")
    )),
    SampleCategory("Widevine DASH (MP4, H264)", listOf(
        SampleStream("Tears of Steel (SD)", "https://storage.googleapis.com/wvmedia/cenc/h264/tears/tears_sd.mpd", "widevine", "https://proxy.uat.widevine.com/proxy?provider=widevine_test"),
        SampleStream("Tears of Steel (HD)", "https://storage.googleapis.com/wvmedia/cenc/h264/tears/tears_hd.mpd", "widevine", "https://proxy.uat.widevine.com/proxy?provider=widevine_test")
    )),
    SampleCategory("HLS", listOf(
        SampleStream("Apple BipBop Basic", "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_4x3/bipbop_4x3_variant.m3u8"),
        SampleStream("Big Buck Bunny", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8")
    )),
    SampleCategory("SmoothStreaming", listOf(
        SampleStream("Super Speed Racing", "https://playready.directtaps.net/smoothstreaming/SSWSS720H264/SuperSpeedRacing.ism/Manifest")
    )),
    SampleCategory("SonGharLive", listOf(
        SampleStream("SL M3U (Cookies)", "https://raw.githubusercontent.com/Sflex0719/SonGharLive/main/SL.m3u"),
        SampleStream("SL JSON (Categories)", "https://raw.githubusercontent.com/Sflex0719/SonGharLive/main/sl.json"),
        SampleStream("JTV Worker", "https://jtv.pfy.workers.dev"),
        SampleStream("Z5 Worker", "https://z5.pfy.workers.dev"),
        SampleStream("ZTV Worker", "https://ztv.pfy.workers.dev")
    ))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SamplesScreen() {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Samples") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sampleData) { category ->
                SampleCategoryItem(category)
            }
        }
    }
}

@Composable
fun SampleCategoryItem(category: SampleCategory) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    category.streams.forEach { stream ->
                        ListItem(
                            headlineContent = { Text(stream.title) },
                            supportingContent = { Text(stream.url, maxLines = 1) }, // Truncate long URLs
                            leadingContent = { Icon(Icons.Default.PlayCircleOutline, null) },
                            modifier = Modifier.clickable {
                                val intent = android.content.Intent(context, com.codesrahul.streamplus.ui.player.PlayerActivity::class.java).apply {
                                    putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_STREAM_URL, stream.url)
                                    putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_STREAM_TITLE, stream.title)
                                    stream.drmLicense?.let { putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_DRM_LICENSE_URL, it) }
                                    stream.drmScheme?.let { putExtra(com.codesrahul.streamplus.ui.player.PlayerActivity.EXTRA_DRM_SCHEME, it) }
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }
}
