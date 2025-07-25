package net.codinux.log.loki.api

import net.codinux.log.loki.api.dto.LabelsResponse
import net.dankito.web.client.RequestParameters
import net.dankito.web.client.WebClient
import net.dankito.web.client.WebClientResult

open class LokiApiClient(
    protected val webClient: WebClient
) {

    companion object {
        const val SinceMaxValue = "30d"
    }


    open suspend fun queryLabels(query: String? = null, since: String? = null): WebClientResult<LabelsResponse> {
        val queryParams = buildMap {
            if (query != null) { put("query", assertQueryFormat(query)) }
            if (since != null) { put("since", since) }
        }

        return webClient.get(RequestParameters("/loki/api/v1/label", LabelsResponse::class, queryParameters = queryParams))
    }

    protected open fun assertQueryFormat(query: String): String =
        if (query.startsWith('{') && query.endsWith('}')) {
            query
        } else {
            "{${query}}"
        }

}