package net.codinux.log.loki.api.dto

enum class AggregateBy(val apiValue: String) {
    Series("series"),

    Labels("labels")
}