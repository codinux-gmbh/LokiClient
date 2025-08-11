package net.codinux.log.loki.client.dto

enum class AggregateBy(val apiValue: String) {
    Series("series"),

    Labels("labels")
}