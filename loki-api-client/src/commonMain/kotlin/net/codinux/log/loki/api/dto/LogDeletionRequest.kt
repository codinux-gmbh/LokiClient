package net.codinux.log.loki.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.dankito.datetime.Instant
import net.dankito.datetime.serialization.InstantEpochSecondsAsDoubleSerializer

@Serializable
data class LogDeletionRequest(
    @SerialName("request_id")
    val requestId: String,

    val query: String,

    /**
     * Possible values: received
     */
    val status: String,

    @SerialName("created_at")
    @Serializable(with = InstantEpochSecondsAsDoubleSerializer::class)
    val createdAt: Instant,

    @SerialName("start_time")
    @Serializable(with = InstantEpochSecondsAsDoubleSerializer::class)
    val startTime: Instant,

    @SerialName("end_time")
    @Serializable(with = InstantEpochSecondsAsDoubleSerializer::class)
    val endTime: Instant,
)
