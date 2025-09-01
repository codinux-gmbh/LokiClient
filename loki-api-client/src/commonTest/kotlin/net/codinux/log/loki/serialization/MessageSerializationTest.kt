package net.codinux.log.loki.serialization

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import kotlinx.serialization.json.Json
import net.codinux.log.loki.client.dto.WebSocketTailMessage
import net.dankito.datetime.LocalDate
import kotlin.test.Test

class MessageSerializationTest {

    private val json = Json

    @Test
    fun deserializeWebSocketTailMessage() {
        val messageJson = WebSocketTailMessage

        val message = json.decodeFromString<WebSocketTailMessage>(messageJson)

        assertThat(message).isNotNull()
        assertThat(message.streams).hasSize(2)

        assertThat(message.droppedEntries).hasSize(5)
        message.droppedEntries.forEach { droppedEntry ->
            assertThat(droppedEntry.labels).hasSize(7)
            assertThat(droppedEntry.timestamp.toLocalDateTimeAtUtc().date).isEqualTo(LocalDate(2025, 9, 1))
        }
    }


    companion object {

        private const val WebSocketTailMessage = """
{
  "streams": [
    {
      "stream": {
        "app": "ingress-nginx-controller",
        "detected_level": "info",
        "job": "podlogs",
        "namespace": "ingress-nginx",
        "node": "knoten",
        "service_name": "ingress-nginx-controller",
        "stream": "stdout"
      },
      "values": [
        [
          "1756749695032629429",
          "message"
        ]
      ]
    },
    {
      "stream": {
        "app": "ingress-nginx-controller",
        "detected_level": "info",
        "job": "podlogs",
        "namespace": "ingress-nginx",
        "node": "knoten",
        "service_name": "ingress-nginx-controller",
        "stream": "stdout"
      },
      "values": [
        [
          "1756749695032687212",
          "\t"
        ]
      ]
    }
  ],
  "dropped_entries": [
    {
      "timestamp": "1756737438665606681",
      "labels": {
        "detected_level": "info",
        "job": "podlogs",
        "namespace": "ingress-nginx",
        "node": "knoten",
        "service_name": "ingress-nginx-controller",
        "stream": "stdout",
        "app": "ingress-nginx-controller"
      }
    },
    {
      "timestamp": "1756737578521786701",
      "labels": {
        "detected_level": "info",
        "job": "podlogs",
        "namespace": "ingress-nginx",
        "node": "knoten",
        "service_name": "ingress-nginx-controller",
        "stream": "stdout",
        "app": "ingress-nginx-controller"
      }
    },
    {
      "timestamp": "1756737578521796559",
      "labels": {
        "app": "ingress-nginx-controller",
        "detected_level": "info",
        "job": "podlogs",
        "namespace": "ingress-nginx",
        "node": "knoten",
        "service_name": "ingress-nginx-controller",
        "stream": "stdout"
      }
    },
    {
      "timestamp": "1756737578521867026",
      "labels": {
        "stream": "stdout",
        "app": "ingress-nginx-controller",
        "detected_level": "info",
        "job": "podlogs",
        "namespace": "ingress-nginx",
        "node": "knoten",
        "service_name": "ingress-nginx-controller"
      }
    },
    {
      "timestamp": "1756737578521902882",
      "labels": {
        "app": "ingress-nginx-controller",
        "detected_level": "info",
        "job": "podlogs",
        "namespace": "ingress-nginx",
        "node": "knoten",
        "service_name": "ingress-nginx-controller",
        "stream": "stdout"
      }
    }
  ]
}
        """

    }

}