package net.codinux.log.loki.test

import net.codinux.log.loki.client.LokiConfig
import net.dankito.web.client.KtorWebClient
import net.dankito.web.client.auth.BasicAuthAuthentication

object TestData {

    const val LogsWithNamespaceLabelQuery = """namespace=~".+""""

    const val LogsWithJobLabelQuery = """job=~".+""""

    const val MetricsQuery = """sum(rate({job="podlogs"}[10m])) by (level)"""


    val lokiConfig = LokiConfig("http://localhost:3100")

    val webClient = KtorWebClient(
        defaultUserAgent = "codinux Loki Client",
        enableWebSocket = true,
    )

}