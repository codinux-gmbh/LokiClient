package net.codinux.log.loki.api.dto

/**
 * I guess it's a bug in Loki, but volume_range sometimes returns a VectorResponse instead of a MatrixResponse.
 *
 * So this class cares for this case and ensures, that either [vectorData] or [matrixData] is set.
 * There is no case where both are set to `null`.
 */
data class VectorOrMatrixResponse private constructor(
    val status: String,
    val vectorData: VectorResponseData?,
    val matrixData: MatrixResponseData?,
) {
    companion object {
        fun forVector(vectorResponse: VectorResponse) =
            VectorOrMatrixResponse(vectorResponse.status, vectorResponse.data, null)

        fun forMatrix(matrixResponse: MatrixResponse) =
            VectorOrMatrixResponse(matrixResponse.status, null, matrixResponse.data)
    }
}