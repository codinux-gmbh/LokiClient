package net.codinux.log.loki.client.dto

import kotlinx.serialization.Serializable

@Serializable
data class PrometheusMatrix(
    val metric: Map<String, String>,
    val values: List<ValuePoint>
)