package net.codinux.log.loki.api

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import net.codinux.log.loki.api.dto.LogDeletionRequest
import net.codinux.log.loki.api.dto.PrometheusMatrix
import net.codinux.log.loki.api.dto.PrometheusVector
import net.codinux.log.loki.api.dto.ResponseData
import net.codinux.log.loki.api.dto.ResultType
import net.codinux.log.loki.api.dto.VectorOrMatrix

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

    fun mapLogDeletionRequestList(logDeletionRequestList: String): List<LogDeletionRequest> =
        json.decodeFromString(logDeletionRequestList)


    fun deserializeVector(vectorElement: JsonElement) =
        json.decodeFromJsonElement(ListSerializer(PrometheusVector.serializer()), vectorElement)

    fun deserializeMatrix(matrixElement: JsonElement) =
        json.decodeFromJsonElement(ListSerializer(PrometheusMatrix.serializer()), matrixElement)

}