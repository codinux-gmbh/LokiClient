package net.codinux.log.loki.model

class PrometheusDuration(
    val duration: Int,
    val unit: PrometheusDurationUnit
) {
    val prometheusDurationString = "$duration${unit.unit}"

    override fun toString() = prometheusDurationString
}