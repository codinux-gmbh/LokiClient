package net.codinux.log.loki.extensions

import net.codinux.log.loki.model.LokiTimestamp
import net.dankito.datetime.Instant

fun Instant.minusThirtyDays() =
    this.minusDays(30)

fun Instant.toLokiTimestamp() = LokiTimestamp(this)