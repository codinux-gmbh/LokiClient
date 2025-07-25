package net.codinux.log.loki.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LabelsResponse(
    val status: String,

    @SerialName("data")
    val labels: List<String>
)