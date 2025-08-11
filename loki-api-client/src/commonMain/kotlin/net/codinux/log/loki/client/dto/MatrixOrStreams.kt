package net.codinux.log.loki.client.dto

class MatrixOrStreams private constructor(
    val type: ResultType,
    val matrix: List<PrometheusMatrix>?,
    val streams: List<Stream>?,
) {
    companion object {
        fun matrix(matrix: List<PrometheusMatrix>) = MatrixOrStreams(ResultType.Matrix, matrix, null)

        fun streams(streams: List<Stream>) = MatrixOrStreams(ResultType.Streams, null, streams)
    }


    override fun toString() =
        if (matrix != null) "Matrix $matrix"
        else "Streams $streams"
}