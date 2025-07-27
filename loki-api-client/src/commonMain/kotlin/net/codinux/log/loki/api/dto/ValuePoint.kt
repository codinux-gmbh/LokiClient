package net.codinux.log.loki.api.dto

import kotlinx.serialization.Serializable
import net.codinux.log.loki.api.serializer.ValuePointSerializer
import net.dankito.datetime.Instant

@Serializable(with = ValuePointSerializer::class)
data class ValuePoint(
    val timestamp: Instant,
    val value: Long, // actually a String, but we convert it to Long, at least for Log Volume its fitting
)