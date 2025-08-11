package net.codinux.log.loki.model

import net.codinux.log.loki.client.dto.ValuePoint

data class GetLogVolumeResult(
    val metrics: Map<String, String>,
    val aggregatedValue: Long,
    val values: List<ValuePoint>,
)