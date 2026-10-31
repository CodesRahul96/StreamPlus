package com.codesrahul.streamplus.data.parser.resolver

import com.codesrahul.streamplus.domain.model.M3UItem

interface StreamResolver {
    fun resolve(item: M3UItem): M3UItem
}
