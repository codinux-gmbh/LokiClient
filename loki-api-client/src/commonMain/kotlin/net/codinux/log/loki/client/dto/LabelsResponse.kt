package net.codinux.log.loki.client.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LabelsResponse(
    val status: String,

    /**
     * If for a time span no labels are known, then [labels] is `null`.
     *
     * I guess this means that there is no log data for this time span.
     */
    @SerialName("data")
    val labels: List<String>? = null
)