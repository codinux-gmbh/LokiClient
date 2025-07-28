package net.codinux.log.loki.service

import net.codinux.log.loki.api.LokiApiClient
import net.codinux.log.loki.model.LabelAnalyzationResult
import net.codinux.log.loki.model.LabelAnalyzationResults
import net.dankito.datetime.Instant

open class LokiApiService(
    protected val client: LokiApiClient,
) {

    companion object {
        private const val ThirtyDaysSeconds = 30 * 24 * 60 * 60
    }


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


    protected open suspend fun <T> getAll(retrieve: suspend (end: Instant) -> List<T>?): Set<T> {
        val results = mutableSetOf<T>()
        var retrievedSuccess: Boolean
        var end = Instant.now()

        do {
            val callResponse = retrieve(end)

            retrievedSuccess = callResponse != null
            end = Instant.ofEpochSeconds(end.epochSeconds.toDouble() - ThirtyDaysSeconds)

            if (callResponse != null) {
                results.addAll(callResponse)
            }
        } while (retrievedSuccess)

        return results
    }

}