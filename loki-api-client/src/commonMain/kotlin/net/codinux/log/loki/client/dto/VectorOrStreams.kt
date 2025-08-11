package net.codinux.log.loki.client.dto

class VectorOrStreams private constructor(
    val type: ResultType,
    val vector: List<PrometheusVector>?,
    val streams: List<Stream>?,
) {
    companion object {
        fun vector(vector: List<PrometheusVector>) = VectorOrStreams(ResultType.Vector, vector, null)

        fun streams(streams: List<Stream>) = VectorOrStreams(ResultType.Streams, null, streams)
    }


    override fun toString() =
        if (vector != null) "Vector $vector"
        else "Streams $streams"
}