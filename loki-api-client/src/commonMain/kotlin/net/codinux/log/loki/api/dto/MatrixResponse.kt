package net.codinux.log.loki.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class MatrixResponse(
    val status: String,
    val data: MatrixResponseData
)