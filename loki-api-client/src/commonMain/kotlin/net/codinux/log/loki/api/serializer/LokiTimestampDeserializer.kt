package net.codinux.log.loki.api.serializer

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import net.dankito.datetime.Instant

object LokiTimestampDeserializer {

    /**
     * In Loki a timestamp can have 3 different formats:
     * - Seconds since epoch as Double
     * - Nanoseconds since epoch as Long
     * - A RFC3339 string
     */
    fun deserializeLokiTimestamp(jsonPrimitive: JsonPrimitive): Instant =
        jsonPrimitive.longOrNull?.let { Instant.ofEpochNanoseconds(it) }
            ?: jsonPrimitive.doubleOrNull?.let { Instant.ofEpochSeconds(it) }
            ?: Instant.parse(jsonPrimitive.content)

}