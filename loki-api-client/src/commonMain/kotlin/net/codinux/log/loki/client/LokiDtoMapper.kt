package net.codinux.log.loki.client

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import net.codinux.log.loki.client.dto.LogDeletionRequest
import net.codinux.log.loki.client.dto.LokiResponse
import net.codinux.log.loki.client.dto.MatrixOrStreams
import net.codinux.log.loki.client.dto.PrometheusMatrix
import net.codinux.log.loki.client.dto.PrometheusVector
import net.codinux.log.loki.client.dto.ResponseData
import net.codinux.log.loki.client.dto.ResultType
import net.codinux.log.loki.client.dto.Stream
import net.codinux.log.loki.client.dto.VectorOrMatrix
import net.codinux.log.loki.client.dto.VectorOrStreams

open class LokiDtoMapper(
    protected val json: Json = Json {
        ignoreUnknownKeys = true
    }
) {

    open fun mapVectorOrMatrixResponse(response: ResponseData): VectorOrMatrix = when (response.resultType) {
        ResultType.Vector -> VectorOrMatrix.vector(deserializeVector(response.result))
        ResultType.Matrix -> VectorOrMatrix.matrix(deserializeMatrix(response.result))
        else -> throw IllegalArgumentException("Unexpected result type '${response.resultType}', " +
                "expected either 'vector' or 'matrix'. Full JSON:\n$response")
    }

    fun mapVectorOrStreamsResponse(response: LokiResponse): VectorOrStreams = when (response.data.resultType) {
        ResultType.Vector -> VectorOrStreams.vector(deserializeVector(response.data.result))
        ResultType.Streams -> VectorOrStreams.streams(deserializeStreams(response.data.result))
        else -> throw IllegalArgumentException("Unexpected result type '${response.data.resultType}', " +
                "expected either 'vector' or 'streams'. Full JSON:\n$response")
    }

    fun mapMatrixOrStreamsResponse(response: LokiResponse): MatrixOrStreams = when (response.data.resultType) {
        ResultType.Matrix -> MatrixOrStreams.matrix(deserializeMatrix(response.data.result))
        ResultType.Streams -> MatrixOrStreams.streams(deserializeStreams(response.data.result))
        else -> throw IllegalArgumentException("Unexpected result type '${response.data.resultType}', " +
                "expected either 'matrix' or 'streams'. Full JSON:\n$response")
    }

    fun mapLogDeletionRequestList(logDeletionRequestList: String): List<LogDeletionRequest> =
        json.decodeFromString(logDeletionRequestList)


    fun deserializeVector(vectorElement: JsonElement) =
        json.decodeFromJsonElement(ListSerializer(PrometheusVector.serializer()), vectorElement)

    fun deserializeMatrix(matrixElement: JsonElement) =
        json.decodeFromJsonElement(ListSerializer(PrometheusMatrix.serializer()), matrixElement)

    fun deserializeStreams(streamsElement: JsonElement) =
        json.decodeFromJsonElement(ListSerializer(Stream.serializer()), streamsElement)

}