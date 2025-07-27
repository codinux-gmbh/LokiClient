package net.codinux.log.loki.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class PatternResponse(
    val status: String,
    val data: List<DetectedPattern> = emptyList(),
)