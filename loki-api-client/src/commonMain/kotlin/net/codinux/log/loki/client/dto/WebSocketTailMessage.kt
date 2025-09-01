package net.codinux.log.loki.client.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A WebSocket message of `/loki/api/v1/tail` endpoint.
 *
 * See [https://grafana.com/docs/loki/latest/reference/loki-http-api/#stream-logs](https://grafana.com/docs/loki/latest/reference/loki-http-api/#stream-logs).
 */
@Serializable
data class WebSocketTailMessage(
    val streams: List<Stream> = emptyList(),

    @SerialName("dropped_entries")
    val droppedEntries: List<DroppedEntry> = emptyList(),
)