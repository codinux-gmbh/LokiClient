package net.codinux.log.loki.api.dto

import kotlinx.serialization.Serializable
import net.dankito.datetime.OffsetDateTime

@Serializable
data class BuildInformation(
    val version: String,
    val revision: String,
    val branch: String,
    val buildUser: String,
    val buildDate: OffsetDateTime,
    val goVersion: String, // was empty in my case
)