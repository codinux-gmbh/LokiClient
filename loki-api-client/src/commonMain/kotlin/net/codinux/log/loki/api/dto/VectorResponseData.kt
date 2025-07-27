package net.codinux.log.loki.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class VectorResponseData(
    val resultType: String, // is always "vector"
    val result: List<PrometheusVector>
)