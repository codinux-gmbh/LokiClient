package net.codinux.log.loki.api.dto

class VectorOrMatrix private constructor(
    val type: ResultType,
    val vector: List<PrometheusVector>?,
    val matrix: List<PrometheusMatrix>?,
) {
    companion object {
        fun vector(vector: List<PrometheusVector>) = VectorOrMatrix(ResultType.Vector, vector, null)

        fun matrix(matrix: List<PrometheusMatrix>) = VectorOrMatrix(ResultType.Matrix, null, matrix)
    }


    override fun toString() =
        if (vector != null) "Vector $vector"
        else "Matrix $matrix"
}