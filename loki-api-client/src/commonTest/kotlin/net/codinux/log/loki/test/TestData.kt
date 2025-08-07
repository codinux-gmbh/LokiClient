package net.codinux.log.loki.test

import net.dankito.web.client.KtorWebClient
import net.dankito.web.client.auth.BasicAuthAuthentication

object TestData {

    const val LogsWithNamespaceLabelQuery = """namespace=~".+""""

    const val LogsWithJobLabelQuery = """job=~".+""""

    const val MetricsQuery = """sum(rate({job="podlogs"}[10m])) by (level)"""

    val webClient = KtorWebClient(
        baseUrl = "http://localhost:3100",
        authentication = null,
        ignoreCertificateErrors = true,
        defaultUserAgent = "codinux Loki HTTP API Client",
        enableBodyCompression = true
    )

}