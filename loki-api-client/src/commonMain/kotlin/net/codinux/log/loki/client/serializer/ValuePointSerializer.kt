package net.codinux.log.loki.client.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import net.codinux.log.loki.client.dto.ValuePoint

object ValuePointSerializer : KSerializer<ValuePoint> {

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("ValuePoint") {
            element<Double>("timestamp")
            element<String>("value")
        }

    override fun deserialize(decoder: Decoder): ValuePoint {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("Expected JsonDecoder")

        val array = jsonDecoder.decodeJsonElement().jsonArray
        val timestamp = LokiTimestampDeserializer.deserializeLokiTimestamp(array[0].jsonPrimitive)
        val value = array[1].jsonPrimitive.content

        return ValuePoint(timestamp, value)
    }


    override fun serialize(encoder: Encoder, value: ValuePoint) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("Expected JsonEncoder")

        val jsonArray = JsonArray(
            listOf(
                JsonPrimitive(value.timestamp.toEpochSecondsAsDouble()),
                JsonPrimitive(value.value)
            )
        )

        jsonEncoder.encodeJsonElement(jsonArray)
    }

}