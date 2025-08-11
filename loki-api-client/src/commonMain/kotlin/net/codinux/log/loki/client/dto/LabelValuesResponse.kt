package net.codinux.log.loki.client.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LabelValuesResponse(
    val status: String,

    /**
     * If for a time span no label values are known, then [labelValues] is `null`.
     */
    @SerialName("data")
    val labelValues: List<String>? = null
)