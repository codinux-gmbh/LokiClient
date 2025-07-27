package net.codinux.log.loki.api

import net.codinux.log.loki.api.dto.*
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
    ): WebClientResult<LogStatisticsResponse> {
        // TODO: for larger queries use POST and url-encoded request body
        val queryParams = buildMap {
            put("query", assertQueryFormat(query))
            if (start != null) { put("start", toEpochNanos(start)) }
            if (end != null) { put("end", toEpochNanos(end)) }
            if (since != null) { put("since", since) }
        }

        return webClient.get(RequestParameters("/loki/api/v1/index/stats", LogStatisticsResponse::class, queryParameters = queryParams))
    }


    /**
     * The `/loki/api/v1/index/volume` and `/loki/api/v1/index/volume_range` endpoints can be used to query the index
     * for volume information about label and label-value combinations.
     * This is helpful in exploring the logs Loki has ingested to find high or low volume streams.
     *
     * The `volume` endpoint returns results for a single point in time, the time the query was processed.
     * Each datapoint represents an aggregation of the matching label or series over the requested time period,
     * returned in a Prometheus style vector response.
     *
     * The query should be a valid LogQL stream selector, for example `{job="foo", env=~".+"}`.
     * By default, these endpoints will aggregate into series consisting of all matches for labels included in the query.
     * For example, assuming you have the streams `{job="foo", env="prod", team="alpha"}`,
     * `{job="bar", env="prod", team="beta"}`, `{job="foo", env="dev", team="alpha"}`, and
     * `{job="bar", env="dev", team="beta"}` in your system.
     * The query `{job="foo", env=~".+"}` would return the two metric series `{job="foo", env="dev"}` and
     * `{job="foo", env="prod"}`, each with datapoints representing the accumulate values of chunks for the streams
     * matching that selector, which in this case would be the streams `{job="foo", env="dev", team="alpha"}` and
     * `{job="foo", env="prod", team="alpha"}`, respectively.
     *
     * There are two parameters which can affect the aggregation strategy.
     *
     * First, a comma-separated list of `targetLabels` can be provided, allowing volumes to be aggregated by the specified
     * `targetLabels` only. This is useful for negations. For example, if you said `{team="alpha", env!="dev"}`, the
     * default behavior would include env in the aggregation set. However, maybe you’re looking for all non-dev jobs
     * for team alpha, and you don’t care which env those are in (other than caring that they’re not dev jobs). To
     * achieve this, you could specify `targetLabels=team,job`, resulting in a single metric series (in this case)
     * of `{team="alpha", job="foo}`.
     *
     * The other way to change aggregations is with the `aggregateBy` parameter.
     * The default value for this is `series`, which aggregates into combinations of matching key-value pairs.
     * Alternately this can be specified as `labels`, which will aggregate into labels only. In this case, the response
     * will have a metric series with a label name matching each label, and a label value of "". This is useful for
     * exploring logs at a high level. For example, if you wanted to know what percentage of your logs had a team label,
     * you could query your logs with `aggregateBy=labels` and a query with either an exact or regex match on `team`, or
     * by including `team` in the list of `targetLabels`.
     */
    open suspend fun queryLogValue(
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

        /**
         * How many metric series to return. The parameter is optional, the default is `100`.
         */
        limit: Int? = null,

        /**
         * A comma separated list of labels to aggregate into. This parameter is optional.
         * When not provided, volumes will be aggregated into the matching labels or label-value pairs.
         */
        targetLabels: Collection<String>? = null,
        /**
         * Whether to aggregate into labels or label-value pairs.
         * This parameter is optional, the default is label-value pairs.
         */
        aggregateBy: AggregateBy? = null,
    ): WebClientResult<VectorResponse> {
        // TODO: for larger queries use POST and url-encoded request body
        val queryParams = buildMap {
            put("query", assertQueryFormat(query))

            if (start != null) { put("start", toEpochNanos(start)) }
            if (end != null) { put("end", toEpochNanos(end)) }
            if (since != null) { put("since", since) }

            if (limit != null) { put("limit", limit) }

            if (targetLabels != null) { put("targetLabels", targetLabels.joinToString(",")) }
            if (aggregateBy != null) { put("aggregateBy", aggregateBy.apiValue) }
        }

        return webClient.get(RequestParameters("/loki/api/v1/index/volume", VectorResponse::class, queryParameters = queryParams))
    }

    /**
     * The `/loki/api/v1/index/volume` and `/loki/api/v1/index/volume_range` endpoints can be used to query the index
     * for volume information about label and label-value combinations.
     * This is helpful in exploring the logs Loki has ingested to find high or low volume streams.
     *
     * The `volume_range` endpoint returns a series of datapoints over a range of time, in Prometheus style matrix
     * response, for each matching set of labels or series.
     * The number of timestamps returned when querying `volume_range` will be determined by the provided step parameter
     * and the requested time range.
     *
     * The query should be a valid LogQL stream selector, for example `{job="foo", env=~".+"}`.
     * By default, these endpoints will aggregate into series consisting of all matches for labels included in the query.
     * For example, assuming you have the streams `{job="foo", env="prod", team="alpha"}`,
     * `{job="bar", env="prod", team="beta"}`, `{job="foo", env="dev", team="alpha"}`, and
     * `{job="bar", env="dev", team="beta"}` in your system.
     * The query `{job="foo", env=~".+"}` would return the two metric series `{job="foo", env="dev"}` and
     * `{job="foo", env="prod"}`, each with datapoints representing the accumulate values of chunks for the streams
     * matching that selector, which in this case would be the streams `{job="foo", env="dev", team="alpha"}` and
     * `{job="foo", env="prod", team="alpha"}`, respectively.
     *
     * There are two parameters which can affect the aggregation strategy.
     *
     * First, a comma-separated list of `targetLabels` can be provided, allowing volumes to be aggregated by the specified
     * `targetLabels` only. This is useful for negations. For example, if you said `{team="alpha", env!="dev"}`, the
     * default behavior would include env in the aggregation set. However, maybe you’re looking for all non-dev jobs
     * for team alpha, and you don’t care which env those are in (other than caring that they’re not dev jobs). To
     * achieve this, you could specify `targetLabels=team,job`, resulting in a single metric series (in this case)
     * of `{team="alpha", job="foo}`.
     *
     * The other way to change aggregations is with the `aggregateBy` parameter.
     * The default value for this is `series`, which aggregates into combinations of matching key-value pairs.
     * Alternately this can be specified as `labels`, which will aggregate into labels only. In this case, the response
     * will have a metric series with a label name matching each label, and a label value of "". This is useful for
     * exploring logs at a high level. For example, if you wanted to know what percentage of your logs had a team label,
     * you could query your logs with `aggregateBy=labels` and a query with either an exact or regex match on `team`, or
     * by including `team` in the list of `targetLabels`.
     */
    open suspend fun queryLogValueRange(
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

        /**
         * How many metric series to return. The parameter is optional, the default is `100`.
         */
        limit: Int? = null,
        /**
         * Query resolution step width in `duration` format or float number of seconds.
         * `duration` refers to Prometheus duration strings of the form `[0-9]+[smhdwy]`.
         * For example, `5m` refers to a duration of 5 minutes.
         * Defaults to a dynamic value based on `start` and `end`.
         * The default step configured for range queries will be used when not provided.
         */
        step: String? = null,

        /**
         * A comma separated list of labels to aggregate into. This parameter is optional.
         * When not provided, volumes will be aggregated into the matching labels or label-value pairs.
         */
        targetLabels: Collection<String>? = null,
        /**
         * Whether to aggregate into labels or label-value pairs.
         * This parameter is optional, the default is label-value pairs.
         */
        aggregateBy: AggregateBy? = null,
    ): WebClientResult<MatrixResponse> {
        // TODO: for larger queries use POST and url-encoded request body
        val queryParams = buildMap {
            put("query", assertQueryFormat(query))

            if (start != null) { put("start", toEpochNanos(start)) }
            if (end != null) { put("end", toEpochNanos(end)) }
            if (since != null) { put("since", since) }

            if (limit != null) { put("limit", limit) }
            if (step != null) { put("step", step) }

            if (targetLabels != null) { put("targetLabels", targetLabels.joinToString(",")) }
            if (aggregateBy != null) { put("aggregateBy", aggregateBy.apiValue) }
        }

        return webClient.get(RequestParameters("/loki/api/v1/index/volume_range", MatrixResponse::class, queryParameters = queryParams))
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