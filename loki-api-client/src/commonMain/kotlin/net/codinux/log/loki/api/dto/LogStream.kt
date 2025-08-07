package net.codinux.log.loki.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class LogStream(
    val stream: Map<String, String>,
    val values: List<LogStreamValue>
)