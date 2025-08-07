package net.codinux.log.loki.model

import kotlinx.serialization.Serializable

@Serializable
data class MetricsResult(
    val metric: Map<String, String>,
    val values: List<MetricValue>
)