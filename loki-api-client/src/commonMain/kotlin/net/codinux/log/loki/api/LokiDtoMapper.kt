package net.codinux.log.loki.api

import kotlinx.serialization.json.Json
import net.codinux.log.loki.api.dto.LogDeletionRequest
import net.codinux.log.loki.api.dto.VectorOrMatrixResponse
import net.codinux.log.loki.api.dto.VectorOrMatrixResponseEnvelop

open class LokiDtoMapper(
    protected val json: Json = Json {
        ignoreUnknownKeys = true
    }
) {

    open fun mapVectorOrMatrixResponse(responseJson: String): VectorOrMatrixResponse {
        val envelop = json.decodeFromString<VectorOrMatrixResponseEnvelop>(responseJson)

        return when (envelop.data.resultType) {
            "vector" -> VectorOrMatrixResponse.forVector(json.decodeFromString(responseJson))
            "matrix" -> VectorOrMatrixResponse.forMatrix(json.decodeFromString(responseJson))
            else -> throw IllegalArgumentException("Unexpected result type '${envelop.data.resultType}', " +
                    "expected either 'vector' or 'matrix'. Full JSON:\n$responseJson")
        }
    }

    fun mapLogDeletionRequestList(logDeletionRequestList: String): List<LogDeletionRequest> =
        json.decodeFromString(logDeletionRequestList)

}