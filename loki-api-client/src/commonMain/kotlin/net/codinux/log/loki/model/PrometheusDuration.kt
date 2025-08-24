package net.codinux.log.loki.model

class PrometheusDuration(
    val duration: Int,
    val unit: PrometheusDurationUnit
) {
    companion object {
        val SinceMaxValue = PrometheusDuration(30, PrometheusDurationUnit.Days)
    }


    val prometheusDurationString = "$duration${unit.unit}"

    override fun toString() = prometheusDurationString
}