package net.codinux.log.loki.client.dto

import kotlinx.serialization.Serializable
import net.codinux.log.loki.client.serializer.ValuePointSerializer
import net.dankito.datetime.Instant

@Serializable(with = ValuePointSerializer::class)
data class ValuePoint(
    val timestamp: Instant,
    val value: String,
) {
    val valueAsLong: Long by lazy { value.toLong() }

    val valueAsDouble: Double by lazy { value.toDouble() }
}