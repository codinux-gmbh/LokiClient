package net.codinux.log.loki.service

import net.codinux.log.loki.api.LokiApiClient
import net.codinux.log.loki.api.dto.AggregateBy
import net.codinux.log.loki.api.dto.LogDeletionRequest
import net.codinux.log.loki.extensions.minusThirtyDays
import net.codinux.log.loki.model.GetLogVolumeResult
import net.codinux.log.loki.model.LabelAnalyzationResult
import net.codinux.log.loki.model.LabelAnalyzationResults
import net.codinux.log.loki.model.LokiTimestamp
import net.dankito.datetime.Instant
import net.dankito.web.client.WebClientResult

open class LokiApiService(
    protected val client: LokiApiClient,
) {

    open suspend fun getAllLabels(): Set<String> {
        return getAll { end ->
            client.queryLabels(end = end, since = LokiApiClient.SinceMaxValue).body?.labels
        }
    }

    open suspend fun getAllStreams(query: String): Set<Map<String, String>> {
        return getAll { end ->
            client.queryStreams(query, end = end, since = LokiApiClient.SinceMaxValue).body?.streams?.takeUnless { it.isEmpty() }
        }
    }


    /**
     * Does the same as `logcli series --analyze-labels` does:
     * Get a summary of labels including count of label value combinations, useful for debugging high cardinality series.
     *
     * It is possible to send an empty label matcher '{}' to return all streams.
     */
    open suspend fun analyzeLabels(query: String = ""): LabelAnalyzationResults {
        val streams = getAllStreams(query)

        val foundInStreams = mutableMapOf<String, Int>()
        val uniqueValues = mutableMapOf<String, MutableSet<String>>()

        streams.forEach { stream ->
            stream.entries.forEach { (label, value) ->
                uniqueValues.getOrPut(label) { mutableSetOf() }.add(value)
                val count = foundInStreams.getOrPut(label) { 0 }
                foundInStreams[label] = count + 1
            }
        }

        val labels = foundInStreams.map { (label, count) ->
            LabelAnalyzationResult(label, count, uniqueValues[label] ?: emptySet())
        }.sortedByDescending { it.foundInStreams }

        return LabelAnalyzationResults(streams, labels)
    }


    open suspend fun getLogVolume(query: String, groupByLabels: List<String>? = null, aggregateBy: AggregateBy? = null): WebClientResult<List<GetLogVolumeResult>> {
        val response = client.queryLogVolume(query, targetLabels = groupByLabels, aggregateBy = aggregateBy)

        return response.mapResponseBodyIfSuccessful { body ->
            val vectorData = body.data.result
            val mapped =vectorData.map { datum ->
                GetLogVolumeResult(datum.metric, datum.value.valueAsLong, listOf(datum.value))
            }
            mapped.sortedByDescending { it.aggregatedValue }
        }
    }

    open suspend fun getLogVolumeRange(query: String, groupByLabels: List<String>? = null, aggregateBy: AggregateBy? = null): WebClientResult<List<GetLogVolumeResult>> {
        val response = client.queryLogVolumeRange(query, targetLabels = groupByLabels, aggregateBy = aggregateBy,
            since = LokiApiClient.SinceMaxValue, step = "1d")

        return response.mapResponseBodyIfSuccessful { body ->
            val mapped = if (body.matrix != null) {
                response.body!!.matrix!!.map { datum ->
                    GetLogVolumeResult(datum.metric, datum.values.sumOf { it.valueAsLong }, datum.values)
                }
            } else {
                response.body!!.vector!!.map { datum ->
                    GetLogVolumeResult(datum.metric, datum.value.valueAsLong, listOf(datum.value))
                }
            }
            mapped.sortedByDescending { it.aggregatedValue }
        }
    }

    /**
     * Creates a new log deletion request and in case of success fetches and returns the newly created [LogDeletionRequest].
     */
    open suspend fun requestLogDeletion(query: String, start: LokiTimestamp? = null, end: LokiTimestamp? = null, maxInterval: String? = null): WebClientResult<LogDeletionRequest?> {
        val deletionResponse = client.requestLogDeletion(query, start, end, maxInterval)

        val createLogDeletionRequest = if (deletionResponse.successfulAndBodySet) {
            val deletionRequestsResponse = client.listLogDeletionRequests()
            if (deletionRequestsResponse.successfulAndBodySet) {
                val deletionRequests = deletionRequestsResponse.body!!
                deletionRequests.filter { it.query == query }
                    .sortedByDescending { it.createdAt }
                    .firstOrNull()
            } else {
                null
            }
        } else {
            null
        }

        return deletionResponse.mapResponseBodyIfSuccessful { createLogDeletionRequest }
    }


    protected open suspend fun <T> getAll(retrieve: suspend (end: LokiTimestamp) -> List<T>?): Set<T> {
        val results = mutableSetOf<T>()
        var retrievedSuccess: Boolean
        var end = Instant.now()

        do {
            val callResponse = retrieve(LokiTimestamp(end))

            retrievedSuccess = callResponse != null
            end = end.minusThirtyDays()

            if (callResponse != null) {
                results.addAll(callResponse)
            }
        } while (retrievedSuccess)

        return results
    }

}