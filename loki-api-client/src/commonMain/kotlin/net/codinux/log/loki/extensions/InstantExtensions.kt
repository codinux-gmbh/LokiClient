package net.codinux.log.loki.extensions

import net.dankito.datetime.Instant

fun Instant.minusThirtyDays() =
    this.minusDays(30)