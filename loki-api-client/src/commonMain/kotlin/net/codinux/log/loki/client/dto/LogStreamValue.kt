package net.codinux.log.loki.client.dto

import kotlinx.serialization.Serializable
import net.codinux.log.loki.client.serializer.LogStreamValueSerializer

@Serializable(with = LogStreamValueSerializer::class)
data class LogStreamValue(
    val timestamp: String,
    val logLine: String,
    val structuredMetadata: Map<String, String> = emptyMap(),
)