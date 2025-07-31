package net.codinux.log.loki.api.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.dankito.datetime.Instant

/**
 * A serializer for Instant that represents an Instant value as seconds since Unix Epoch time
 * as Double with nanoseconds of second as decimal part.
 */
object InstantEpochSecondsSerializer: KSerializer<Instant> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("net.dankito.datetime.Instant", PrimitiveKind.DOUBLE)


    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeLong(value.epochSeconds) // TODO: add nanoseconds of second part
    }

    override fun deserialize(decoder: Decoder): Instant =
        Instant.ofEpochSeconds(decoder.decodeDouble())

}