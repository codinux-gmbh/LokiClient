package net.codinux.log.loki.api.serializer

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.codinux.log.loki.api.dto.Stream
import net.codinux.log.loki.model.LogEntry

object StreamSerializer : KSerializer<Stream> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Stream") {
        element("stream", MapSerializer(String.serializer(), String.serializer()).descriptor)
        element("values", ListSerializer(LogEntrySerializer).descriptor)
    }


    override fun serialize(encoder: Encoder, value: Stream) {
        val composite = encoder.beginStructure(descriptor)
        composite.encodeSerializableElement(descriptor, 0, MapSerializer(String.serializer(), String.serializer()), value.stream)
        composite.encodeSerializableElement(descriptor, 1, ListSerializer(LogEntrySerializer), value.values)
        composite.endStructure(descriptor)
    }


    override fun deserialize(decoder: Decoder): Stream {
        val dec = decoder.beginStructure(descriptor)
        var stream: Map<String, String>? = null
        var values: List<LogEntry>? = null

        loop@ while (true) {
            when (val index = dec.decodeElementIndex(descriptor)) {
                0 -> stream = dec.decodeSerializableElement(descriptor, 0, MapSerializer(String.serializer(), String.serializer()))
                1 -> values = dec.decodeSerializableElement(descriptor, 1, ListSerializer(LogEntrySerializer))
                CompositeDecoder.DECODE_DONE -> break@loop
                else -> throw SerializationException("Unexpected index $index")
            }
        }

        dec.endStructure(descriptor)

        @OptIn(ExperimentalSerializationApi::class)
        return Stream(
            stream ?: throw MissingFieldException(listOf("stream"), "stream"),
            values ?: throw MissingFieldException(listOf("values"), "values")
        )
    }

}