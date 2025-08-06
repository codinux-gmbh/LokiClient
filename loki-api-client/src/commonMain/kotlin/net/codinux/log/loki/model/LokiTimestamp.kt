package net.codinux.log.loki.model

import net.dankito.datetime.Instant
import kotlin.jvm.JvmInline

@JvmInline
value class LokiTimestamp(
    val timestamp: Instant,
) {

    companion object {

        fun ofEpochSeconds(secondsSinceEpoch: Long) = LokiTimestamp(Instant(secondsSinceEpoch))

        fun ofEpochSeconds(secondsSinceEpoch: Double) = LokiTimestamp(Instant.ofEpochSeconds(secondsSinceEpoch))

        fun ofEpochMillis(millisecondsSinceEpoch: Long) = LokiTimestamp(Instant.ofEpochMilli(millisecondsSinceEpoch))

        fun ofEpochNanos(nanosecondsSinceEpoch: Long) = LokiTimestamp(Instant.ofEpochNanoseconds(nanosecondsSinceEpoch))

        fun ofInstant(instant: Instant) = LokiTimestamp(instant)

    }

}