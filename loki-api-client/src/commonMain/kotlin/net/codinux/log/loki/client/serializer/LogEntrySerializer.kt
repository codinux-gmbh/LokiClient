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
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import net.codinux.log.loki.model.LogEntry

object LogEntrySerializer : KSerializer<LogEntry> {

    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildSerialDescriptor("LogEntry", StructureKind.LIST)


    override fun serialize(encoder: Encoder, value: LogEntry) {
        val composite = encoder as? JsonEncoder
            ?: throw SerializationException("This class can be serialized only by JSON")

        val jsonArray = buildJsonArray {
            add(JsonPrimitive(value.timestamp.toEpochNanosecondsString()))
            add(JsonPrimitive(value.message))
        }

        composite.encodeJsonElement(jsonArray)
    }


    override fun deserialize(decoder: Decoder): LogEntry {
        val input = decoder as? JsonDecoder
            ?: throw SerializationException("This class can be deserialized only by JSON")

        val jsonArray = input.decodeJsonElement().jsonArray

        if (jsonArray.size != 2) {
            throw SerializationException("Expected 2 elements in the array")
        }

        val timestamp = LokiTimestampDeserializer.deserializeLokiTimestamp(jsonArray[0].jsonPrimitive)
        val logLine = jsonArray[1].jsonPrimitive.content

        return LogEntry(timestamp, logLine)
    }

}