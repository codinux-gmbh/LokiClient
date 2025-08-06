package net.codinux.log.loki.api

import net.codinux.log.loki.api.dto.*
import net.codinux.log.loki.model.LokiTimestamp
import net.dankito.web.client.RequestParameters
import net.dankito.web.client.WebClient
import net.dankito.web.client.WebClientResult
import net.dankito.web.client.get

open class LokiApiClient(
    protected val webClient: WebClient,
    /**
     * In case internal endpoints like /ready, /config, /services, /metrics, ...
     * are configured to have a path prefix like `/loki/internal`, configure this prefix here.
     */
    protected val internalEndpointsPrefix: String = "",
    protected val mapper: LokiDtoMapper = LokiDtoMapper(),
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
         */
        start: LokiTimestamp? = null,
        /**
         * The end time for the query as a nanosecond Unix epoch. Defaults to now.
         */
        end: LokiTimestamp? = null,
        /**
         * A `duration` used to calculate [start] relative to [end].
         * If [end] is in the future, [start] is calculated as this duration before now.
         * Any value specified for [start] supersedes this parameter.
         */
        since: String? = null,
    ): WebClientResult<LabelsResponse> {
        val queryParams = queryParams(query, start, end, since)

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
         */
        start: LokiTimestamp? = null,
        /**
         * The end time for the query as a nanosecond Unix epoch. Defaults to now.
         */
        end: LokiTimestamp? = null,
        /**
         * A `duration` used to calculate [start] relative to [end].
         * If [end] is in the future, [start] is calculated as this duration before now.
         * Any value specified for [start] supersedes this parameter.
         */
        since: String? = null,
    ): WebClientResult<LabelValuesResponse> {
        val queryParams = queryParams(query, start, end, since)

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
         */
        start: LokiTimestamp? = null,
        /**
         * The end time for the query as a nanosecond Unix epoch. Defaults to now.
         */
        end: LokiTimestamp? = null,
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

        val queryParams = queryParams(null, start, end, since, mapOf("match[]" to assertQueryFormat(query)))

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
        start: LokiTimestamp? = null,
        /**
         * End timestamp.
         */
        end: LokiTimestamp? = null,
        /**
         * Not documented, but seems to work: A `duration` used to calculate [start] relative to [end].
         * If [end] is in the future, [start] is calculated as this duration before now.
         * Any value specified for [start] supersedes this parameter.
         */
        since: String? = null,
    ): WebClientResult<LogStatisticsResponse> {
        // TODO: for larger queries use POST and url-encoded request body
        val queryParams = queryParams(query, start, end, since)

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
    open suspend fun queryLogVolume(
        /**
         * The LogQL matchers to check (that is, `{job="foo", env!="dev"}`).
         *
         * In our implementation the curly braces can be omitted.
         */
        query: String,

        /**
         * Start timestamp.
         */
        start: LokiTimestamp? = null,
        /**
         * End timestamp.
         */
        end: LokiTimestamp? = null,
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
        val queryParams = queryParams(query, start, end, since, mapOf(
            "limit" to limit,
            "targetLabels" to targetLabels?.joinToString(","),
            "aggregateBy" to aggregateBy?.apiValue
        ))

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
    open suspend fun queryLogVolumeRange(
        /**
         * The LogQL matchers to check (that is, `{job="foo", env!="dev"}`).
         *
         * In our implementation the curly braces can be omitted.
         */
        query: String,

        /**
         * Start timestamp.
         */
        start: LokiTimestamp? = null,
        /**
         * End timestamp.
         */
        end: LokiTimestamp? = null,
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
    ): WebClientResult<VectorOrMatrixResponse> {
        // TODO: for larger queries use POST and url-encoded request body
        val queryParams = queryParams(query, start, end, since, mapOf(
            "limit" to limit,
            "step" to step,

            "targetLabels" to targetLabels?.joinToString(","),
            "aggregateBy" to aggregateBy?.apiValue
        ))

        val response = webClient.get(RequestParameters("/loki/api/v1/index/volume_range", String::class, queryParameters = queryParams))

        // i guess it's a bug in Loki that it sometimes returns a VectorResponse instead of a MatrixResponse
        return response.mapResponseBodyIfSuccessful { body -> mapper.mapVectorOrMatrixResponse(body) }
    }


    /**
     * The `/loki/api/v1/patterns` endpoint can be used to query loki for patterns detected in the logs.
     * This helps understand the structure of the logs Loki has ingested.
     *
     * The query should be a valid LogQL stream selector, for example `{job="foo", env=~".+"}`.
     * The result is aggregated by the `pattern` from all matching streams.
     *
     * For each pattern detected, the response includes the pattern itself and the number of samples for
     * each pattern at each timestamp.
     *
     * To enable this feature you must configure:
     * ```yaml
     * pattern_ingester:
     *   enabled: true
     * ```
     */
    open suspend fun patternsDetection(
        /**
         * The LogQL matchers to check (that is, `{job="foo", env=~".+"}`).
         *
         * In our implementation the curly braces can be omitted.
         */
        query: String,

        /**
         * Start timestamp.
         */
        start: LokiTimestamp? = null,
        /**
         * End timestamp.
         */
        end: LokiTimestamp? = null,
        /**
         * Not documented, but seems to work: A `duration` used to calculate [start] relative to [end].
         * If [end] is in the future, [start] is calculated as this duration before now.
         * Any value specified for [start] supersedes this parameter.
         */
        since: String? = null,
        /**
         * Step between samples for occurrences of this pattern.
         * A Prometheus duration string of the form `[0-9]+[smhdwy]` or float number of seconds.
         */
        step: String? = null,
    ): WebClientResult<PatternResponse> {
        // TODO: for larger queries use POST and url-encoded request body
        val queryParams = queryParams(query, start, end, since, mapOf("step" to step))

        return webClient.get(RequestParameters("/loki/api/v1/patterns", PatternResponse::class, queryParameters = queryParams))
    }


    /**
     * Create a new delete request for the authenticated tenant.
     * The [log entry deletion](https://grafana.com/docs/loki/latest/operations/storage/logs-deletion/)
     * documentation has configuration details.
     *
     * Log entry deletion is supported only when TSDB or BoltDB Shipper is configured for the index store.
     *
     * The query parameter can also include filter operations. For example `query={foo="bar"} |= "other"` will filter
     * out lines that contain the string “other” for the streams matching the stream selector `{foo="bar"}`.
     *
     * A 204 response indicates success.
     */
    open suspend fun requestLogDeletion(
        /**
         * Query argument that identifies the streams from which to delete with optional line filters.
         */
        query: String,
        /**
         * A timestamp that identifies the start of the time window within which entries will be deleted.
         */
        start: LokiTimestamp? = null,
        /**
         * A timestamp that identifies the end of the time window within which entries will be deleted.
         * If not specified, defaults to the current time.
         */
        end: LokiTimestamp? = null,
        /**
         * The maximum time period the delete request can span.
         * If the request is larger than this value, it is split into several requests of <= `max_interval`.
         * Valid time units are `s`, `m`, and `h`.
         */
        maxInterval: String? = null,
    ): WebClientResult<Boolean> {
        val queryParams = queryParams(other = mapOf(
            "query" to assertQueryWithLogLineFormat(query),
            "start" to start?.let { toEpochSecondsOrRfc3339(start) },
            "end" to end?.let { toEpochSecondsOrRfc3339(end) },
            "max_interval" to maxInterval
        ))

        val response = webClient.put(RequestParameters("/loki/api/v1/delete", String::class, queryParameters = queryParams))

        return response.mapResponseBodyIfSuccessful { response.statusCode == 204 }
    }

    /**
     * List the existing delete requests for the authenticated tenant.
     * The log entry deletion documentation has configuration details.
     *
     * Log entry deletion is supported only when TSDB or BoltDB Shipper is configured for the index store.
     *
     * This endpoint returns both processed and unprocessed deletion requests.
     * It does not list canceled requests, as those requests will have been removed from storage.
     */
    open suspend fun listLogDeletionRequests(): WebClientResult<List<LogDeletionRequest>> {
        val response = webClient.get<String>("/loki/api/v1/delete")

        // don't know why, but KtorWebClient fails to decode a List, so we need to do it manually
        return response.mapResponseBodyIfSuccessful { body ->
            mapper.mapLogDeletionRequestList(body)
        }
    }

    /**
     * Remove a delete request for the authenticated tenant.
     * The log entry deletion documentation has configuration details.
     *
     * Loki allows cancellation of delete requests until the requests are picked up for processing.
     * It is controlled by the `delete_request_cancel_period` YAML configuration or the equivalent command line
     * option when invoking Loki.
     * To cancel a delete request that has been picked up for processing or is partially complete,
     * pass the `force=true` query parameter to the API.
     *
     * Log entry deletion is supported only when TSDB or BoltDB Shipper is configured for the index store.
     *
     * Note:
     * Some data from the request may still be deleted and the deleted request will be listed as ‘processed’.
     */
    open suspend fun requestCancellationOfDeleteRequest(
        /**
         * Identifies the delete request to cancel; IDs are found using [listLogDeletionRequests].
         */
        requestId: String,
        /**
         * When the force query parameter is true, partially completed delete requests will be canceled.
         */
        force: Boolean? = null,
    ): WebClientResult<Boolean> {
        val queryParams = queryParams(other = mapOf(
            "request_id" to requestId,
            "force" to force
        ))

        val response = webClient.delete(RequestParameters("/loki/api/v1/delete", String::class, queryParameters = queryParams))

        return response.mapResponseBodyIfSuccessful { response.statusCode == 204 }
    }



    open suspend fun getBuildInformation(): WebClientResult<BuildInformation> =
        webClient.get("/loki/api/v1/status/buildinfo")


    /*          Internal endpoints          */

    /**
     * /ready returns HTTP 200 when the Loki instance is ready to accept traffic.
     */
    open suspend fun ready(): WebClientResult<String> =
        webClient.get("$internalEndpointsPrefix/ready")

    open suspend fun config(): WebClientResult<String> =
        webClient.get("$internalEndpointsPrefix/config")

    open suspend fun services(): WebClientResult<String> =
        webClient.get("$internalEndpointsPrefix/services")

    open suspend fun metrics(): WebClientResult<String> =
        webClient.get("$internalEndpointsPrefix/metrics")


    protected open fun queryParams(query: String? = null, start: LokiTimestamp? = null, end: LokiTimestamp? = null, since: String? = null,
                                   other: Map<String, Any?> = emptyMap()): Map<String, Any> =
        buildMap {
            if (query != null) { put("query", assertQueryFormat(query)) }

            if (start != null) { put("start", toEpochNanos(start)) }
            if (end != null) { put("end", toEpochNanos(end)) }
            if (since != null) { put("since", since) }

            other.forEach { (key, value) ->
                if (value != null) {
                    put(key, value)
                }
            }
        }

    protected open fun assertQueryFormat(query: String): String =
        if (query.startsWith('{') && query.endsWith('}')) {
            query
        } else {
            "{${query}}"
        }

    /**
     * Checks the format of a LogQL query including a log line, e.g. `{foo="bar"} |= "other".
     */
    protected open fun assertQueryWithLogLineFormat(query: String): String =
        if (query.startsWith('{')) {
            query
        } else {
            if (query.contains(" |= ")) { // TODO: make more robust, e.g. for cases when the white spaces around '|=' are missing
                "{${query.substringBefore(" |= ")}} |= ${query.substringAfter(" |= ")}"
            } else {
                "{${query}}"
            }
        }

    protected open fun toEpochNanosOrNull(instant: LokiTimestamp?) = instant?.let { toEpochNanos(it) }

    protected open fun toEpochNanos(timestamp: LokiTimestamp) = timestamp.timestamp.toEpochNanosecondsString()

    protected open fun toEpochSecondsOrRfc3339(timestamp: LokiTimestamp): String = timestamp.timestamp.toString()

}