package net.codinux.log.loki.client.dto

import kotlinx.serialization.Serializable

@Serializable
data class LogStatisticsResponse(
    val streams: Int,
    val chunks: Int,
    val entries: Int,
    val bytes: Int,
)
