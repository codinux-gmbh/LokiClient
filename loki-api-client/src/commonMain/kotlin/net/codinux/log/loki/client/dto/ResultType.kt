package net.codinux.log.loki.client.dto

import kotlinx.serialization.SerialName

enum class ResultType {

    @SerialName("vector")
    Vector,

    @SerialName("matrix")
    Matrix,

    @SerialName("streams")
    Streams,

}