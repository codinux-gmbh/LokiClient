package net.codinux.log.loki.client.dto

import kotlinx.serialization.Serializable

@Serializable
data class PushLogsRequestBody(
    val streams: List<LogStream>
)