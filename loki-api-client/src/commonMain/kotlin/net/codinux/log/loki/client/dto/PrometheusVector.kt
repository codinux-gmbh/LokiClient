package net.codinux.log.loki.client.dto

import kotlinx.serialization.Serializable

@Serializable
data class PrometheusVector(
    val metric: Map<String, String>,
    val value: ValuePoint
)