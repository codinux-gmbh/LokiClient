package net.codinux.log.loki.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StreamsRequest(
    @SerialName("match[]")
    val match: String,
    val start: String? = null,
    val end: String? = null,
    val since: String? = null,
)