package net.codinux.log.loki.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class PushLogsRequestBody(
    val streams: List<LogStream>
)