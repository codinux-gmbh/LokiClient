package net.codinux.log.loki.extensions

import net.dankito.datetime.Instant

const val ThirtyDaysSeconds = 30 * 24 * 60 * 60L

fun Instant.toEpochSecondsAsDouble(): Double =
    "${epochSeconds}.${nanosecondsOfSecond.toString().padStart(9, '0')}".toDouble()

fun Instant.minusSeconds(seconds: Long): Instant =
    Instant.ofEpochSeconds(this.toEpochSecondsAsDouble() - seconds)

fun Instant.minusThirtyDays() =
    this.minusSeconds(ThirtyDaysSeconds)

fun Instant.toEpochNanoseconds(): String = "$epochSeconds${nanosecondsOfSecond.toString().padStart(9, '0')}"