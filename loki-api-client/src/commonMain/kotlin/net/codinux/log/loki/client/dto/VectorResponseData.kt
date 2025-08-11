package net.codinux.log.loki.client.dto

import kotlinx.serialization.Serializable

@Serializable
data class VectorResponseData(
    val resultType: String, // is always "vector"
    val result: List<PrometheusVector>
)