package com.codesrahul.streamplus.data.parser.resolver

import com.codesrahul.streamplus.domain.model.M3UItem

class CompositeStreamResolver(private val resolvers: List<StreamResolver>) : StreamResolver {

    override fun resolve(item: M3UItem): M3UItem {
        var params = item
        resolvers.forEach { resolver ->
            params = resolver.resolve(params)
        }
        return params
    }
}
