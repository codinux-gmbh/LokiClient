package net.codinux.log.loki.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.codinux.log.loki.api.serializer.InstantEpochSecondsSerializer
import net.dankito.datetime.Instant

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
    @Serializable(with = InstantEpochSecondsSerializer::class)
    val createdAt: Instant,

    @SerialName("start_time")
    @Serializable(with = InstantEpochSecondsSerializer::class)
    val startTime: Instant,

    @SerialName("end_time")
    @Serializable(with = InstantEpochSecondsSerializer::class)
    val endTime: Instant,
)
