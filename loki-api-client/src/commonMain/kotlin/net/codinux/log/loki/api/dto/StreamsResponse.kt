package net.codinux.log.loki.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StreamsResponse(
    val status: String,

    @SerialName("data")
    val streams: List<Map<String, String>>? = null
)