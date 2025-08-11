package net.codinux.log.loki.client.dto

import kotlinx.serialization.Serializable

@Serializable
data class LogStream(
    val stream: Map<String, String>,
    val values: List<LogStreamValue>
)