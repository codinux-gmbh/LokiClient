package net.codinux.log.loki.client.dto

import kotlinx.serialization.Serializable
import net.codinux.log.loki.client.serializer.StreamSerializer
import net.codinux.log.loki.model.LogEntry

@Serializable(with = StreamSerializer::class)
data class Stream(
    val stream: Map<String, String>,
    val values: List<LogEntry>,
)