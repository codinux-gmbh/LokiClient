package net.codinux.log.loki.api

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import kotlinx.coroutines.test.runTest
import net.codinux.log.loki.test.TestData
import kotlin.test.Test

class LokiApiClientTest {

    private val underTest = LokiApiClient(TestData.webClient)


    @Test
    fun queryLabels() = runTest {
        val result = underTest.queryLabels()

        assertThat(result::successful).isTrue()
        assertThat(result::body).isNotNull()

        val body = result.body!!
        assertThat(body.status).isEqualTo("success")
        assertThat(body.labels!!.size).isGreaterThan(3)
    }


    @Test
    fun queryLabelValues() = runTest {
        val result = underTest.queryLabelValues("namespace")

        assertThat(result::successful).isTrue()
        assertThat(result::body).isNotNull()

        val body = result.body!!
        assertThat(body.status).isEqualTo("success")
        assertThat(body.labelValues!!.size).isGreaterThan(3)
    }

}