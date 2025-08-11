package net.codinux.log.loki.client.dto

import kotlinx.serialization.Serializable

@Serializable
data class PatternResponse(
    val status: String,
    val data: List<DetectedPattern> = emptyList(),
)