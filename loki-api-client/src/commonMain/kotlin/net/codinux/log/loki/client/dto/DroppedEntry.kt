package net.codinux.log.loki.client.dto

import kotlinx.serialization.Serializable
import net.dankito.datetime.Instant
import net.dankito.datetime.serialization.InstantEpochNanosecondsSerializer

@Serializable
data class DroppedEntry(
    @Serializable(with = InstantEpochNanosecondsSerializer::class)
    val timestamp: Instant,
    val labels: Map<String, String>,
)