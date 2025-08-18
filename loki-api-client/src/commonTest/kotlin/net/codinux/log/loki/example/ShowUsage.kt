package net.codinux.log.loki.example

import net.codinux.log.loki.client.LokiClient
import net.codinux.log.loki.client.LokiConfig
import net.codinux.log.loki.client.dto.AggregateBy
import net.codinux.log.loki.client.dto.SortOrder
import net.codinux.log.loki.model.LabelAnalyzationResults
import net.codinux.log.loki.model.LogEntryToSave
import net.codinux.log.loki.model.LokiTimestamp
import net.codinux.log.loki.model.hours
import net.codinux.log.loki.service.LokiService
import net.dankito.datetime.Instant
import net.dankito.web.client.KtorWebClient
import net.dankito.web.client.auth.BasicAuthAuthentication

class ShowUsage {

    private val webClient = KtorWebClient()

    private val client = LokiClient("http://localhost:3100", webClient)

    // or

    private val config = LokiConfig(
        baseUrl = "http://localhost:3100",
        authentication = BasicAuthAuthentication("username", "password"),
        internalEndpointsPathPrefix = null
    )

    private val clientAuthenticated = LokiClient(config, webClient)

    private val service = LokiService(client)


    suspend fun queryLogs() {
        // query logs of namespace 'monitoring' of last 2 hours
        val result = service.queryLogs(query = """{namespace="monitoring"}""", start = LokiTimestamp(Instant.now().minusHours(2)))
        result.mapResponseBodyIfSuccessful { logs ->
            println("Retrieved ${logs.size} logs:")
            logs.forEachIndexed { index, log -> println("[${index + 1}] $log") }
        }

        // other parameters:

        // Alternatively to `start` you can use `since`, which determines the start relative to `end`.
        // The sort order can be set with `direction` and the max results with `limit`.
        // If `query` starts and ends with curly braces, the curly braces can be left away.
        service.queryLogs("namespace=\"monitoring\"", since = 2.hours, end = LokiTimestamp.ofDate(2025, 8, 5),
            direction = SortOrder.Backward, limit = 500)
    }

    suspend fun pushLogs() {
        service.ingestLogs(
            LogEntryToSave(timestamp = LokiTimestamp.now(), message = "Something important happened"),
            LogEntryToSave(timestamp = LokiTimestamp.now(), message = "With Labels", labels = mapOf("namespace" to "monitoring", "job" to "podlogs")),
            LogEntryToSave(timestamp = LokiTimestamp.now(), message = "With structured metadata", labels = mapOf("namespace" to "monitoring"),
                structuredMetadata = mapOf("level" to "info", "pod" to "MonitoringApp-58f856b99-5gwtt")),
        )
    }

    suspend fun getLabels() {
        // all of the methods below can be restricted with query, start, end and since parameter

        val allLabels = service.getAllLabels()

        val valuesOfLabelNamespace = client.queryLabelValues("namespace")
    }

    suspend fun analyze() {
        // does the same as `logcli series --analyze-labels` does:
        val results: LabelAnalyzationResults = service.analyzeLabels()
        results.labels.forEach {
            println("${it.label}: Unique values: ${it.uniqueValues}. Found in Streams: ${it.foundInStreams}")
        }

        val streamsInNamespaceMonitoring: Set<Map<String, String>> = service.getAllStreams("namespace=~\"monitoring\"")
    }

    suspend fun getIndexVolume() {
        // e.g. get Log volume of each namespace
        service.getIndexVolume("namespace=~\".+\"").mapResponseBodyIfSuccessful { response, indexVolumes ->
            indexVolumes.forEach { volume -> println("${volume.metrics["namespace"]}: ${volume.aggregatedValue}") }
        }

        // group log volume by labels like 'service_name' and aggregate by labels or series
        service.getIndexVolume("namespace=~\".+\"", groupByLabels = listOf("service_name"), aggregateBy = AggregateBy.Labels)
    }

}