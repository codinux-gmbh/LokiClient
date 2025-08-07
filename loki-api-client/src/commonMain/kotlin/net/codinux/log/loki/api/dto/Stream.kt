package net.codinux.log.loki.api.dto

import kotlinx.serialization.Serializable
import net.codinux.log.loki.api.serializer.StreamSerializer
import net.codinux.log.loki.model.LogEntry

@Serializable(with = StreamSerializer::class)
data class Stream(
    val stream: Map<String, String>,
    val values: List<LogEntry>,
)