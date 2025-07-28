package net.codinux.log.loki.service

import net.codinux.log.loki.api.LokiApiClient
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