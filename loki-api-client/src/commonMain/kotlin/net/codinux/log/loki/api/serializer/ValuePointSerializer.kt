package net.codinux.log.loki.api.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import net.codinux.log.loki.api.dto.ValuePoint
import net.dankito.datetime.Instant

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
        val timestamp = array[0].jsonPrimitive.double
        val value = array[1].jsonPrimitive.content

        return ValuePoint(Instant.ofEpochSeconds(timestamp), value)
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