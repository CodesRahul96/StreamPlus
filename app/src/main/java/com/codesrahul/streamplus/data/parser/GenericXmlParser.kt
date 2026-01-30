package com.codesrahul.streamplus.data.parser

import android.util.Log
import com.codesrahul.streamplus.domain.model.M3UItem
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.Reader
import java.io.StringReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GenericXmlParser @Inject constructor() {
    private val TAG = "GenericXmlParser"

    fun parse(reader: Reader): List<M3UItem> {
        val list = mutableListOf<M3UItem>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            val xpp = factory.newPullParser()
            xpp.setInput(reader)

            var eventType = xpp.eventType
            var currentItem: M3UItem? = null
            var currentTag: String? = null
            
            var name: String? = null
            var url: String? = null
            var logo: String? = null
            var group: String? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = xpp.name.lowercase()
                        // Check attributes for URLs/Logos
                        for (i in 0 until xpp.attributeCount) {
                            val attrName = xpp.getAttributeName(i).lowercase()
                            val attrValue = xpp.getAttributeValue(i)
                            if (isUrl(attrValue)) {
                                if (isImage(attrValue)) logo = attrValue else url = attrValue
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val text = xpp.text.trim()
                        if (text.isNotEmpty() && currentTag != null) {
                            when {
                                isNameTag(currentTag) -> name = text
                                isUrlTag(currentTag) -> url = text
                                isLogoTag(currentTag) -> logo = text
                                isGroupTag(currentTag) -> group = text
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val endTag = xpp.name.lowercase()
                        if (isItemEndTag(endTag)) {
                            if (!url.isNullOrBlank()) {
                                val snifferResult = MetadataSniffer.sniffMetadata(url!!, name)
                                list.add(
                                    M3UItem(
                                        name = snifferResult.name ?: name ?: "Channel ${list.size + 1}",
                                        url = url!!,
                                        logoUrl = if (!logo.isNullOrBlank()) logo!! else (snifferResult.logo ?: ""),
                                        group = if (!group.isNullOrBlank()) group!! else (snifferResult.group ?: "Uncategorized"),
                                        drmScheme = snifferResult.drmScheme,
                                        licenseUrl = snifferResult.drmLicense,
                                        isPlaylist = url!!.lowercase().run { contains(".m3u") || contains(".json") || contains("m3u=1") }
                                    )
                                )
                            }
                            // Don't reset everything, some might be shared (like group)
                            name = null
                            url = null
                            logo = null
                        }
                        currentTag = null
                    }
                }
                eventType = xpp.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "XML Parse error", e)
        }
        return list
    }

    private fun isNameTag(tag: String) = tag in listOf("name", "title", "label", "displayname", "string")
    private fun isUrlTag(tag: String) = tag in listOf("url", "link", "location", "src", "source", "path")
    private fun isLogoTag(tag: String) = tag in listOf("image", "logo", "icon", "thumbnail", "thumb", "img")
    private fun isGroupTag(tag: String) = tag in listOf("group", "category", "genre")
    private fun isItemEndTag(tag: String) = tag in listOf("station", "track", "item", "channel", "link")

    private fun isUrl(text: String) = text.startsWith("http") || text.startsWith("rtsp") || text.startsWith("mms")
    private fun isImage(text: String) = text.lowercase().run { endsWith(".png") || endsWith(".jpg") || endsWith(".jpeg") || endsWith(".webp") || contains("image") }
    
    fun parse(content: String): List<M3UItem> = parse(StringReader(content))
}
