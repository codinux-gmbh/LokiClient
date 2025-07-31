package net.codinux.log.loki.extensions

import net.dankito.datetime.Instant

const val ThirtyDaysSeconds = 30 * 24 * 60 * 60L

fun Instant.minusSeconds(seconds: Long): Instant =
    Instant.ofEpochSeconds(this.epochSeconds.toDouble() - seconds)

fun Instant.minusThirtyDays() =
    this.minusSeconds(ThirtyDaysSeconds)