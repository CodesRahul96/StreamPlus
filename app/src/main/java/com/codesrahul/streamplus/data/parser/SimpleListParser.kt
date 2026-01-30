package com.codesrahul.streamplus.data.parser

import android.util.Log
import com.codesrahul.streamplus.domain.model.M3UItem
import java.io.BufferedReader
import java.io.StringReader

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SimpleListParser @Inject constructor() {
    private val TAG = "SimpleListParser"

    fun parse(content: String): List<M3UItem> {
        val list = mutableListOf<M3UItem>()
        val reader = BufferedReader(StringReader(content))
        var line: String? = reader.readLine()
        var count = 0

        while (line != null) {
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && (trimmed.startsWith("http://") || trimmed.startsWith("https://"))) {
                list.add(
                    M3UItem(
                        name = "Channel ${count + 1}",
                        url = trimmed,
                        group = "Uncategorized"
                    )
                )
                count++
            }
            line = reader.readLine()
        }

        if (list.isNotEmpty()) {
            Log.i(TAG, "Parsed Simple List: ${list.size} channels")
        }
        return list
    }
}
