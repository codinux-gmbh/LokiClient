package net.codinux.log.loki.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class Stream(
    val stream: Map<String, String>,
    val values: List<ValuePoint>
)