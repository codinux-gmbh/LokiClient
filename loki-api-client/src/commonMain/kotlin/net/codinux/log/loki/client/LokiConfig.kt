package net.codinux.log.loki.client

import net.dankito.web.client.auth.Authentication

data class LokiConfig(

    /**
     * Loki base url e.g. `http://localhost:3100` or `http://loki.monitoring:3100`, without `/loki/api/v1`.
     */
    val baseUrl: String,

    /**
     * If Loki is protected e.g. with BasicAuth, configured its credentials here.
     */
    val authentication: Authentication? = null,

    /**
     * In case internal endpoints like `/ready`, `/config`, `/services`, `/metrics`, ...
     * are configured to have a path prefix like `/loki/internal`, configure this prefix here.
     *
     * This can e.g. be configured via a reverse proxy path rewrite.
     */
    val internalEndpointsPathPrefix: String? = null
)