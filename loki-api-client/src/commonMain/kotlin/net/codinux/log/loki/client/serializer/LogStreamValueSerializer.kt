package net.codinux.log.loki.client.serializer

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.codinux.log.loki.client.dto.LogStreamValue

object LogStreamValueSerializer : KSerializer<LogStreamValue> {

    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildSerialDescriptor("LogStreamValue", StructureKind.LIST)


    override fun serialize(encoder: Encoder, value: LogStreamValue) {
        val composite = encoder as? JsonEncoder
            ?: throw SerializationException("This class can be serialized only by JSON")

        val jsonArray = buildJsonArray {
            add(JsonPrimitive(value.timestamp))
            add(JsonPrimitive(value.logLine))

            if (value.structuredMetadata.isNotEmpty()) {
                add(JsonObject(value.structuredMetadata.mapValues { JsonPrimitive(it.value) }))
            }
        }

        composite.encodeJsonElement(jsonArray)
    }

    override fun deserialize(decoder: Decoder): LogStreamValue {
        val input = decoder as? JsonDecoder
            ?: throw SerializationException("This class can be deserialized only by JSON")

        val jsonArray = input.decodeJsonElement().jsonArray

        if (jsonArray.size !in 2..3) {
            throw SerializationException("Expected 2 or 3 elements in the array")
        }

        val timestamp = jsonArray[0].jsonPrimitive.content
        val logLine = jsonArray[1].jsonPrimitive.content

        val metadata = if (jsonArray.size == 3) {
            jsonArray[2].jsonObject.mapValues {
                it.value.jsonPrimitive.content
            }
        } else {
            emptyMap()
        }

        return LogStreamValue(timestamp, logLine, metadata)
    }
}