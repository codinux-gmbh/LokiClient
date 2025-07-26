package net.codinux.log.loki.service

import net.codinux.log.loki.api.LokiApiClient
import net.codinux.log.loki.api.dto.LabelsResponse
import net.dankito.datetime.Instant
import net.dankito.web.client.WebClientResult

open class LokiApiService(
    protected val client: LokiApiClient,
) {

    companion object {
        private const val ThirtyDaysSeconds = 30 * 24 * 60 * 60
    }

    open suspend fun getAllLabels(): Set<String> {
        val labels = mutableSetOf<String>()
        var response: WebClientResult<LabelsResponse>
        var end = Instant.now()

        do {
            response = client.queryLabels(end = end, since = LokiApiClient.SinceMaxValue)
            end = Instant.ofEpochSeconds(end.epochSeconds.toDouble() - ThirtyDaysSeconds)

            if (response.successful && response.body?.labels != null) {
                labels.addAll(response.body!!.labels!!)
            }
        } while (response.successful && response.body?.labels != null)

        return labels
    }

}