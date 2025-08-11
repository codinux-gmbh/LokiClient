package net.codinux.log.loki.client.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class LokiResponse(
    val status: String,
    val data: ResponseData
)

@Serializable
data class ResponseData(
    val resultType: ResultType,
    val result: JsonElement,
)