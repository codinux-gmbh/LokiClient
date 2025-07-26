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


    /**
     * Retrieves the list of known labels within a given time span.
     * Loki may use a larger time span than the one specified.
     */
    open suspend fun queryLabels(
        /**
         * Log stream selector that selects the streams to match and return label names.
         * Example: `{app="myapp", environment="dev"}`.
         * In our implementation the curly braces can be omitted.
         */
        query: String? = null,
        /**
         * A `duration` used to calculate `start` relative to `end`.
         * If `end` is in the future, `start` is calculated as this duration before now.
         * Any value specified for `start` supersedes this parameter.
         */
        since: String? = null,
    ): WebClientResult<LabelsResponse> {
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