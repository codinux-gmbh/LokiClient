package net.codinux.log.loki.model

data class LogEntryToSave(
    val timestamp: LokiTimestamp,
    val message: String,
    val labels: Map<String, String>,
    val structuredMetadata: Map<String, String> = emptyMap(),
)