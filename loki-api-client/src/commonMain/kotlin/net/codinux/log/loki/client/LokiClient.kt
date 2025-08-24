package net.codinux.log.loki.client

import net.codinux.log.loki.client.dto.*
import net.codinux.log.loki.model.LokiTimestamp
import net.codinux.log.loki.model.PrometheusDuration
import net.codinux.log.loki.model.PrometheusDurationUnit
import net.dankito.web.client.RequestParameters
import net.dankito.web.client.WebClient
import net.dankito.web.client.WebClientResult
import net.dankito.web.client.auth.Authentication

open class LokiClient(
    config: LokiConfig,
    protected val webClient: WebClient,
    protected val mapper: LokiDtoMapper = LokiDtoMapper(),
) {

    companion object {
        private fun removeSlashAtEnd(url: String): String =
            if (url.endsWith("/")) url.substring(0, url.length - 1) else url
    }


    constructor(baseUrl: String, webClient: WebClient) : this(baseUrl, null, webClient)

    constructor(baseUrl: String, authentication: Authentication?, webClient: WebClient) : this(LokiConfig(baseUrl, authentication), webClient)


    protected val apiEndpoint = removeSlashAtEnd(config.baseUrl) + "/loki/api/v1"

    protected val internalEndpoint = removeSlashAtEnd(config.baseUrl) + removeSlashAtEnd(config.internalEndpointsPathPrefix ?: "")

    protected val authentication: Authentication? = config.authentication


    /**
     * `/loki/api/v1/query_range` is used to do a query over a range of time.
     * This type of query is often referred to as a range query.
     * Range queries are used for both log and metric type LogQL queries.
     */
    open suspend fun rangeQuery(
        /**
         * The [LogQL](https://grafana.com/docs/loki/latest/query/) query to perform.
         */
        query: String,
        /**
         * The start time for the query as a nanosecond Unix epoch or another supported format.
         * Defaults to one hour ago. Loki returns results with timestamp greater or equal to this value.
         */
        start: LokiTimestamp? = null,
        /**
         * The end time for the query as a nanosecond Unix epoch or another supported format.
         * Defaults to now. Loki returns results with timestamp lower than this value.
         */
        end: LokiTimestamp? = null,
        /**
         * A `duration` used to calculate `start` relative to `end`. If `end` is in the future, `start` is
         * calculated as this duration before now. Any value specified for `start` supersedes this parameter.
         */
        since: PrometheusDuration? = null,
        /**
         * The max number of entries to return. It defaults to `100`. Only applies to query
         * types which produce a stream (log lines) response.
         */
        limit: Int? = null,
        /**
         * Query resolution step width in `duration` format or float number of seconds.
         * `duration` refers to Prometheus duration strings of the form `[0-9]+[smhdwy]`.
         * For example, `5m` refers to a duration of 5 minutes.
         * Defaults to a dynamic value based on `start` and `end`.
         * Only applies to query types which produce a matrix response (e.g. metric queries like `count_over_time()`, `rate()`, ...).
         *
         * Use the `step` parameter when making metric queries to Loki, or queries which return a matrix response.
         * It is evaluated in exactly the same way Prometheus evaluates `step`.
         * First the query will be evaluated at `start` and then evaluated again at `start + step` and again at
         * `start + step + step` until `end` is reached.
         * The result will be a matrix of the query result evaluated at each step.
         */
        step: PrometheusDuration? = null,
        /**
         * Only return entries at (or greater than) the specified interval, can be a `duration` format or
         * float number of seconds. Only applies to queries which produce a stream response (like log queries).
         * Not to be confused with step, which is only applied for metric queries.
         *
         * Use the `interval` parameter when making log queries to Loki, or queries which return a stream response.
         * It is evaluated by returning a log entry at `start`, then the next entry will be returned an entry with
         * `timestamp >= start + interval`, and again at `start + interval + interval` and so on until `end` is reached.
         * It does not fill missing entries.
         */
        interval: PrometheusDuration? = null,
        /**
         * Determines the sort order of logs. Defaults to `backward`.
         */
        direction: SortOrder? = null,
    ): WebClientResult<MatrixOrStreams> {
        val queryParams = queryParams(query, start, end, since, other = mapOf(
            "limit" to limit, "direction" to direction?.apiValue,
            "step" to step?.prometheusDurationString, "interval" to interval?.prometheusDurationString,
        ))

        return webClient.get(RequestParameters("$apiEndpoint/query_range", LokiResponse::class, queryParameters = queryParams, authentication = authentication))
            .mapResponseBodyIfSuccessful { body -> mapper.mapMatrixOrStreamsResponse(body) }
    }

    /**
     * `/loki/api/v1/query` allows for doing queries against a single point in time.
     * This type of query is often referred to as an instant query.
     * Instant queries are only used for metric type LogQL queries and will return a 400
     * (Bad Request) in case a log type query is provided.
     *
     * In other words: In most cases you want to use [rangeQuery]
     */
    open suspend fun instantQuery(
        /**
         * The [LogQL](https://grafana.com/docs/loki/latest/query/) query to perform.
         * Requests that do not use valid LogQL syntax will return errors.
         */
        query: String,
        /**
         * The max number of entries to return. It defaults to `100`. Only applies to query
         * types which produce a stream (log lines) response.
         */
        limit: Int? = null,
        /**
         * The evaluation time for the query as a nanosecond Unix epoch or another supported format. Defaults to now.
         */
        time: LokiTimestamp? = null,
        /**
         * Determines the sort order of logs. Defaults to `backward`.
         */
        direction: SortOrder? = null,
    ): WebClientResult<VectorOrStreams> {
        val queryParams = queryParams(query, other = mapOf("limit" to limit, "time" to time, "direction" to direction?.apiValue))

        return webClient.get(RequestParameters("$apiEndpoint/query", LokiResponse::class, queryParameters = queryParams, authentication = authentication))
            .mapResponseBodyIfSuccessful { body -> mapper.mapVectorOrStreamsResponse(body) }
    }


    /**
     * Send log entries to Loki.
     */
    open suspend fun ingestLogs(logEntries: List<LogStream>): WebClientResult<Boolean> =
        webClient.post(RequestParameters("$apiEndpoint/push", Unit::class, PushLogsRequestBody(logEntries), authentication = authentication))
            // If block_ingestion_until is configured and push requests are blocked, the endpoint will return the
            // status code configured in block_ingestion_status_code (260 by default) along with an error message.
            // If the configured status code is 200, no error message will be returned.
            .let { it.copyWithBody(it.statusCode in 200..259) }


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
        since: PrometheusDuration? = null,
    ): WebClientResult<LabelsResponse> {
        val queryParams = queryParams(query, start, end, since)

        return webClient.get(RequestParameters("$apiEndpoint/label", LabelsResponse::class, queryParameters = queryParams, authentication = authentication))
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
        since: PrometheusDuration? = null,
    ): WebClientResult<LabelValuesResponse> {
        val queryParams = queryParams(query, start, end, since)

        return webClient.get(RequestParameters("$apiEndpoint/label/$label/values", LabelValuesResponse::class, queryParameters = queryParams, authentication = authentication))
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
        since: PrometheusDuration? = null,
    ): WebClientResult<StreamsResponse> {
        // TODO: for larger queries use POST and url-encoded request body:
        // You can URL-encode these parameters directly in the request body by using the POST method and
        // Content-Type: application/x-www-form-urlencoded header. This is useful when specifying a large or dynamic
        // number of stream selectors that may breach server-side URL character limits.

        val queryParams = queryParams(null, start, end, since, mapOf("match[]" to assertQueryFormat(query)))

        return webClient.get(RequestParameters("$apiEndpoint/series", StreamsResponse::class, queryParameters = queryParams, authentication = authentication))
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
        since: PrometheusDuration? = null,
    ): WebClientResult<LogStatisticsResponse> {
        // TODO: for larger queries use POST and url-encoded request body
        val queryParams = queryParams(query, start, end, since)

        return webClient.get(RequestParameters("$apiEndpoint/index/stats", LogStatisticsResponse::class, queryParameters = queryParams, authentication = authentication))
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
    open suspend fun queryIndexVolume(
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
        since: PrometheusDuration? = null,

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

        return webClient.get(RequestParameters("$apiEndpoint/index/volume", VectorResponse::class, queryParameters = queryParams, authentication = authentication))
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
    open suspend fun queryIndexVolumeRange(
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
        since: PrometheusDuration? = null,

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
    ): WebClientResult<VectorOrMatrix> {
        // TODO: for larger queries use POST and url-encoded request body
        val queryParams = queryParams(query, start, end, since, mapOf(
            "limit" to limit,
            "step" to step,

            "targetLabels" to targetLabels?.joinToString(","),
            "aggregateBy" to aggregateBy?.apiValue
        ))

        val response = webClient.get(RequestParameters("$apiEndpoint/index/volume_range", LokiResponse::class, queryParameters = queryParams, authentication = authentication))

        // i guess it's a bug in Loki that it sometimes returns a VectorResponse instead of a MatrixResponse
        return response.mapResponseBodyIfSuccessful { body -> mapper.mapVectorOrMatrixResponse(body.data) }
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
        since: PrometheusDuration? = null,
        /**
         * Step between samples for occurrences of this pattern.
         * A Prometheus duration string of the form `[0-9]+[smhdwy]` or float number of seconds.
         */
        step: String? = null,
    ): WebClientResult<PatternResponse> {
        // TODO: for larger queries use POST and url-encoded request body
        val queryParams = queryParams(query, start, end, since, mapOf("step" to step))

        return webClient.get(RequestParameters("$apiEndpoint/patterns", PatternResponse::class, queryParameters = queryParams, authentication = authentication))
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
        val queryParams = queryParams(query, other = mapOf(
            "start" to start?.let { toEpochSecondsOrRfc3339(start) },
            "end" to end?.let { toEpochSecondsOrRfc3339(end) },
            "max_interval" to maxInterval
        ))

        val response = webClient.put(RequestParameters("$apiEndpoint/delete", String::class, queryParameters = queryParams, authentication = authentication))

        return response.mapResponseBodyIfSuccessful { _ -> response.statusCode == 204 }
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
        val response = webClient.get(RequestParameters("$apiEndpoint/delete", String::class, authentication = authentication))

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

        val response = webClient.delete(RequestParameters("$apiEndpoint/delete", String::class, queryParameters = queryParams, authentication = authentication))

        return response.mapResponseBodyIfSuccessful { _ -> response.statusCode == 204 }
    }



    open suspend fun getBuildInformation(): WebClientResult<BuildInformation> =
        webClient.get(RequestParameters("$apiEndpoint/status/buildinfo", BuildInformation::class, authentication = authentication))


    /*          Internal endpoints          */

    /**
     * /ready returns HTTP 200 when the Loki instance is ready to accept traffic.
     */
    open suspend fun ready(): WebClientResult<String> =
        webClient.get(RequestParameters("$internalEndpoint/ready", String::class, authentication = authentication))

    open suspend fun config(): WebClientResult<String> =
        webClient.get(RequestParameters("$internalEndpoint/config", String::class, authentication = authentication))

    open suspend fun services(): WebClientResult<String> =
        webClient.get(RequestParameters("$internalEndpoint/services", String::class, authentication = authentication))

    open suspend fun metrics(): WebClientResult<String> =
        webClient.get(RequestParameters("$internalEndpoint/metrics", String::class, authentication = authentication))


    protected open fun queryParams(query: String? = null, start: LokiTimestamp? = null, end: LokiTimestamp? = null, since: PrometheusDuration? = null,
                                   other: Map<String, Any?> = emptyMap()): Map<String, Any> =
        buildMap {
            if (query != null) { put("query", assertQueryFormat(query)) }

            if (start != null) { put("start", toEpochNanos(start)) }
            if (end != null) { put("end", toEpochNanos(end)) }
            if (since != null) { put("since", since.prometheusDurationString) }

            other.forEach { (key, value) ->
                if (value != null) {
                    put(key, value)
                }
            }
        }

    protected open fun assertQueryFormat(query: String): String =
        // query can also contain a metric query like `count_over_time()` or `rate()` or end with a log line filter
        // like `|= "table"` so that a check if query starts and ends with '{' and '}' is not valid
        if (query.contains('{') == false && query.contains('}') == false) {
            "{${query}}"
        } else {
            query
        }

    protected open fun toEpochNanos(timestamp: LokiTimestamp) = timestamp.timestamp.toEpochNanosecondsString()

    protected open fun toEpochSecondsOrRfc3339(timestamp: LokiTimestamp): String = timestamp.timestamp.isoString

}