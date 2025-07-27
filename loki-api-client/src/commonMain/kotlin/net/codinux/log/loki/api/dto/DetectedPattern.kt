package net.codinux.log.loki.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class DetectedPattern(
    val pattern: String,
    val samples: List<ValuePoint>
)