package net.codinux.log.loki.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class VectorOrMatrixResponseEnvelop(
    val status: String,
    val data: VectorOrMatrixResponseData
)

@Serializable
data class VectorOrMatrixResponseData(
    val resultType: String,
)