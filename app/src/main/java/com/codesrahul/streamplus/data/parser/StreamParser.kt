package com.codesrahul.streamplus.data.parser

import com.codesrahul.streamplus.domain.model.M3UItem
import java.io.InputStream

interface StreamParser {
    suspend fun parse(input: InputStream, sourceUrl: String): List<M3UItem>
    fun canParse(url: String, prefix: String): Boolean
}
