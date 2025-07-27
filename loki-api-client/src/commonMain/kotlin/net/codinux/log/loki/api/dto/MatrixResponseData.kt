package net.codinux.log.loki.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class MatrixResponseData(
    val resultType: String, // is always "matrix"
    val result: List<PrometheusMatrix>
)