package net.codinux.log.loki.model

import kotlinx.serialization.Serializable
import net.dankito.datetime.Instant

@Serializable
data class LogEntry(
    val timestamp: Instant,
    val message: String,
)