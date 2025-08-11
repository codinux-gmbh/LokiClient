package net.codinux.log.loki.client.dto

import kotlinx.serialization.Serializable

@Serializable
data class VectorResponse(
    val status: String,
    val data: VectorResponseData
)