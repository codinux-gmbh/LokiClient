package net.codinux.log.loki.model

import kotlinx.serialization.Serializable

@Serializable
data class QueryLogResult(
    val stream: Map<String, String>,
    val entries: List<LogEntry>,
)