package net.codinux.log.loki.model

import kotlinx.serialization.Serializable

@Serializable
data class LabelAnalyzationResult(
    val label: String,
    val foundInStreams: Int,
    val uniqueValues: Set<String>,
) {
    val countUniqueValues: Int = uniqueValues.size
}