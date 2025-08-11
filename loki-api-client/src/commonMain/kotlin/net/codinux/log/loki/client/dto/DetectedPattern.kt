package net.codinux.log.loki.client.dto

import kotlinx.serialization.Serializable

@Serializable
data class DetectedPattern(
    val pattern: String,
    val samples: List<ValuePoint>
)