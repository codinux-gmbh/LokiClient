package net.codinux.log.loki.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class PrometheusVector(
    val metric: Map<String, String>,
    val value: ValuePoint
)