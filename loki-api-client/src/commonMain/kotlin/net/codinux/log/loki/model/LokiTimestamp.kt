package net.codinux.log.loki.model

import net.codinux.log.loki.extensions.minusThirtyDays
import net.codinux.log.loki.extensions.toLokiTimestamp
import net.dankito.datetime.Instant
import net.dankito.datetime.LocalDate
import net.dankito.datetime.LocalDateTime
import net.dankito.datetime.Month
import kotlin.jvm.JvmInline

@JvmInline
value class LokiTimestamp(
    val timestamp: Instant,
) {

    companion object {

        fun now() = LokiTimestamp(Instant.now())

        fun ofEpochSeconds(secondsSinceEpoch: Long) = LokiTimestamp(Instant(secondsSinceEpoch))

        fun ofEpochSeconds(secondsSinceEpoch: Double) = LokiTimestamp(Instant.ofEpochSeconds(secondsSinceEpoch))

        fun ofEpochMillis(millisecondsSinceEpoch: Long) = LokiTimestamp(Instant.ofEpochMilli(millisecondsSinceEpoch))

        fun ofEpochNanos(nanosecondsSinceEpoch: Long) = LokiTimestamp(Instant.ofEpochNanoseconds(nanosecondsSinceEpoch))

        fun ofInstant(instant: Instant) = LokiTimestamp(instant)

        fun ofDate(year: Int, month: Month, day: Int) = ofDate(LocalDate(year, month, day))

        fun ofDate(year: Int, month: Int, day: Int) = ofDate(LocalDate(year, month, day))

        fun ofDate(date: LocalDate) = ofDateTime(date.atStartOfDay())

        fun ofDateTime(year: Int, month: Month, day: Int, hour: Int, minute: Int = 0, second: Int = 0, nanosecondOfSecond: Int = 0) =
            ofDateTime(LocalDateTime(year, month, day, hour, minute, second, nanosecondOfSecond))

        fun ofDateTime(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0, second: Int = 0, nanosecondOfSecond: Int = 0) =
            ofDateTime(LocalDateTime(year, month, day, hour, minute, second, nanosecondOfSecond))

        fun ofDateTime(dateTime: LocalDateTime) = LokiTimestamp(dateTime.toInstantAtUtc())

        fun thirtyDaysAgo(): LokiTimestamp = Instant.now().minusThirtyDays().toLokiTimestamp()

    }

}