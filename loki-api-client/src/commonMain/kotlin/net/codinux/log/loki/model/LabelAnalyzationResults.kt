package net.codinux.log.loki.model

import kotlinx.serialization.Serializable

@Serializable
data class LabelAnalyzationResults(
    val streams: Set<Map<String, String>>,
    val labels: List<LabelAnalyzationResult>
)