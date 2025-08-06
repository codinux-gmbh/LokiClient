package net.codinux.log.loki.extensions

import net.codinux.log.loki.model.LokiTimestamp
import net.dankito.datetime.toKmpInstant
import java.time.Instant

fun Instant.toLokiTimestamp() = this.toKmpInstant().toLokiTimestamp()

fun LokiTimestamp.Companion.ofInstant(instant: Instant) = ofInstant(instant.toKmpInstant())