package net.codinux.log.loki.api

import net.codinux.log.loki.api.dto.LabelValuesResponse
import net.codinux.log.loki.api.dto.LabelsResponse
import net.dankito.datetime.Instant
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
         *
         * In our implementation the curly braces can be omitted.
         */
        query: String? = null,
        /**
         * The start time for the query as a nanosecond Unix epoch. Defaults to 6 hours ago.
         *
         * LokiApiClient automatically converts the Instant to the appropriate Unix epoch timestamp.
         */
        start: Instant? = null,
        /**
         * The end time for the query as a nanosecond Unix epoch. Defaults to now.
         *
         * LokiApiClient automatically converts the Instant to the appropriate Unix epoch timestamp.
         */
        end: Instant? = null,
        /**
         * A `duration` used to calculate [start] relative to [end].
         * If [end] is in the future, [start] is calculated as this duration before now.
         * Any value specified for [start] supersedes this parameter.
         */
        since: String? = null,
    ): WebClientResult<LabelsResponse> {
        val queryParams = buildMap {
            if (query != null) { put("query", assertQueryFormat(query)) }
            if (start != null) { put("start", start.toEpochNanoseconds()) }
            if (end != null) { put("end", end.toEpochNanoseconds()) }
            if (since != null) { put("since", since) }
        }

        return webClient.get(RequestParameters("/loki/api/v1/label", LabelsResponse::class, queryParameters = queryParams))
    }


    /**
     * Retrieves the list of known values for a given label within a given time span.
     * Loki may use a larger time span than the one specified.
     */
    open suspend fun queryLabelValues(
        label: String,
        /**
         * Log stream selector that selects the streams to match and return label values for <name>.
         * Example: `{app="myapp", environment="dev"}`.
         *
         * In our implementation the curly braces can be omitted.
         */
        query: String? = null,
        /**
         * The start time for the query as a nanosecond Unix epoch. Defaults to 6 hours ago.
         *
         * LokiApiClient automatically converts the Instant to the appropriate Unix epoch timestamp.
         */
        start: Instant? = null,
        /**
         * The end time for the query as a nanosecond Unix epoch. Defaults to now.
         *
         * LokiApiClient automatically converts the Instant to the appropriate Unix epoch timestamp.
         */
        end: Instant? = null,
        /**
         * A `duration` used to calculate [start] relative to [end].
         * If [end] is in the future, [start] is calculated as this duration before now.
         * Any value specified for [start] supersedes this parameter.
         */
        since: String? = null,
    ): WebClientResult<LabelValuesResponse> {
        val queryParams = buildMap {
            if (query != null) { put("query", assertQueryFormat(query)) }
            if (start != null) { put("start", start.toEpochNanoseconds()) }
            if (end != null) { put("end", end.toEpochNanoseconds()) }
            if (since != null) { put("since", since) }
        }

        return webClient.get(RequestParameters("/loki/api/v1/label/$label/values", LabelValuesResponse::class, queryParameters = queryParams))
    }


    protected open fun assertQueryFormat(query: String): String =
        if (query.startsWith('{') && query.endsWith('}')) {
            query
        } else {
            "{${query}}"
        }


    fun Instant.toEpochNanoseconds(): String = "$epochSeconds${nanosecondsOfSecond.toString().padStart(9, '0')}"

}