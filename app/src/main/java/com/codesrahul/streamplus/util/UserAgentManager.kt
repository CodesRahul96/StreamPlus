package com.codesrahul.streamplus.util

data class UserAgentPreset(
    val name: String,
    val value: String,
    val description: String,
    val iconName: String // Logic mapping to icons
)

object UserAgentManager {
    val presets = listOf(
        UserAgentPreset("Chrome", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36", "Mozilla/5.0 (Windows NT 10.0; ...", "chrome"),
        UserAgentPreset("Firefox", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:122.0) Gecko/20100101 Firefox/122.0", "Mozilla/5.0 (Windows NT 10.0; ...", "firefox"),
        UserAgentPreset("Safari", "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_2_1) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Safari/605.1.15", "Mozilla/5.0 (Macintosh; Intel M...", "safari"),
        UserAgentPreset("Edge", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36 Edg/121.0.0.0", "Mozilla/5.0 (Windows NT 10.0; ...", "edge"),
        UserAgentPreset("Mobile Chrome", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Mobile Safari/537.36", "Mozilla/5.0 (Linux; Android 10;...", "chrome_mobile"),
        UserAgentPreset("Android 14", "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.6167.164 Mobile Safari/537.36", "Mozilla/5.0 (Linux; ...", "android"),
        UserAgentPreset("iPhone 15", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.3 Mobile/15E148 Safari/604.1", "Mozilla/5.0 (iPhone; ...", "ios"),
        UserAgentPreset("Smart TV", "Mozilla/5.0 (Web0S; SmartTV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.5735.197 Safari/537.36", "Mozilla/5.0 (Web0S; ...", "tv"),
        UserAgentPreset("TiviMate", "TiviMate/5.0.4 (Linux; Android 11)", "TiviMate IPTV Player", "tv"),
        UserAgentPreset("OTT Navigator", "OTT-Navigator/1.7.1.1 (Linux; Android 10)", "OTT Navigator IPTV", "tv")
    )

    const val DEFAULT_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
    
    fun getFullUserAgent(name: String, customValue: String = ""): String {
        if (name == "Custom") return customValue.takeIf { it.isNotBlank() } ?: DEFAULT_UA
        return presets.find { it.name == name }?.value ?: DEFAULT_UA
    }
}
