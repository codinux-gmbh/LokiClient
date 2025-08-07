package net.codinux.log.loki.model

import kotlinx.serialization.Serializable
import net.dankito.datetime.Instant

@Serializable
data class MetricValue(
    val timestamp: Instant,
    val value: Double,
)