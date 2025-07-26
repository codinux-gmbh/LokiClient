package net.codinux.log.loki.api

import net.codinux.log.loki.api.dto.LabelValuesResponse
import net.codinux.log.loki.api.dto.LabelsResponse
import net.codinux.log.loki.api.dto.StatisticsResponse
import net.codinux.log.loki.api.dto.StreamsResponse
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
            if (start != null) { put("start", toEpochNanos(start)) }
            if (end != null) { put("end", toEpochNanos(end)) }
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
            if (start != null) { put("start", toEpochNanos(start)) }
            if (end != null) { put("end", toEpochNanos(end)) }
            if (since != null) { put("since", since) }
        }

        return webClient.get(RequestParameters("/loki/api/v1/label/$label/values", LabelValuesResponse::class, queryParameters = queryParams))
    }


    /**
     * This endpoint returns the list of streams (unique set of labels) that match a certain given selector.
     */
    open suspend fun queryStreams(
        /**
         * Repeated log stream selector argument that selects the streams to return.
         *
         * In our implementation the curly braces can be omitted.
         */
        query: String,
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
    ): WebClientResult<StreamsResponse> {
        // TODO: for larger queries use POST and url-encoded request body:
        // You can URL-encode these parameters directly in the request body by using the POST method and
        // Content-Type: application/x-www-form-urlencoded header. This is useful when specifying a large or dynamic
        // number of stream selectors that may breach server-side URL character limits.

        val queryParams = buildMap {
            put("match[]", assertQueryFormat(query))
            if (start != null) { put("start", toEpochNanos(start)) }
            if (end != null) { put("end", toEpochNanos(end)) }
            if (since != null) { put("since", since) }
        }

        return webClient.get(RequestParameters("/loki/api/v1/series", StreamsResponse::class, queryParameters = queryParams))
    }


    /**
     * The `/loki/api/v1/index/stats` endpoint can be used to query the index for the number of `streams`, `chunks`,
     * `entries`, and `bytes` that a query resolves to.
     *
     * It is an approximation with the following caveats:
     *
     * - It does not include data from the ingesters.
     * - It is a probabilistic technique.
     * - Streams/chunks which span multiple period configurations may be counted twice.
     *
     * These make it generally more helpful for larger queries. It can be used for better understanding the throughput
     * requirements and data topology for a list of matchers over a period of time.
     */
    open suspend fun queryLogStatistics(
        /**
         * The LogQL matchers to check (that is, `{job="foo", env!="dev"}`).
         *
         * In our implementation the curly braces can be omitted.
         */
        query: String,
        /**
         * Start timestamp.
         */
        start: Instant? = null,
        /**
         * End timestamp.
         */
        end: Instant? = null,
        /**
         * Not documented, but seems to work: A `duration` used to calculate [start] relative to [end].
         * If [end] is in the future, [start] is calculated as this duration before now.
         * Any value specified for [start] supersedes this parameter.
         */
        since: String? = null,
    ): WebClientResult<StatisticsResponse> {
        // TODO: for larger queries use POST and url-encoded request body
        val queryParams = buildMap {
            put("query", assertQueryFormat(query))
            if (start != null) { put("start", toEpochNanos(start)) }
            if (end != null) { put("end", toEpochNanos(end)) }
            if (since != null) { put("since", since) }
        }

        return webClient.get(RequestParameters("/loki/api/v1/index/stats", StatisticsResponse::class, queryParameters = queryParams))
    }


    protected open fun assertQueryFormat(query: String): String =
        if (query.startsWith('{') && query.endsWith('}')) {
            query
        } else {
            "{${query}}"
        }

    protected open fun toEpochNanosOrNull(instant: Instant?) = instant?.let { toEpochNanos(it) }

    protected open fun toEpochNanos(instant: Instant): String = instant.toEpochNanoseconds()

    fun Instant.toEpochNanoseconds(): String = "$epochSeconds${nanosecondsOfSecond.toString().padStart(9, '0')}"

}