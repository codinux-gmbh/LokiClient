package net.codinux.log.loki.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class VectorResponse(
    val status: String,
    val data: VectorResponseData
)