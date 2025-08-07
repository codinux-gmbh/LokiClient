package net.codinux.log.loki.model

val Int.seconds: PrometheusDuration get() = PrometheusDuration(this, PrometheusDurationUnit.Seconds)

val Int.minutes: PrometheusDuration get() = PrometheusDuration(this, PrometheusDurationUnit.Minutes)

val Int.hours: PrometheusDuration get() = PrometheusDuration(this, PrometheusDurationUnit.Hours)

val Int.days: PrometheusDuration get() = PrometheusDuration(this, PrometheusDurationUnit.Days)