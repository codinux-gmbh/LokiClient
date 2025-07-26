package net.codinux.log.loki.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class StatisticsResponse(
    val streams: Int,
    val chunks: Int,
    val entries: Int,
    val bytes: Int,
)
