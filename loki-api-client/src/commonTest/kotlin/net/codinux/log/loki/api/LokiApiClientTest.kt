package net.codinux.log.loki.api

import assertk.assertThat
import assertk.assertions.*
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
        assertThat(body::status).isEqualTo("success")
        assertThat(body.labels!!.size).isGreaterThan(3)
    }


    @Test
    fun queryLabelValues() = runTest {
        val result = underTest.queryLabelValues("namespace")

        assertThat(result::successful).isTrue()
        assertThat(result::body).isNotNull()

        val body = result.body!!
        assertThat(body::status).isEqualTo("success")
        assertThat(body.labelValues!!.size).isGreaterThan(3)
    }


    @Test
    fun queryStreams() = runTest {
        val result = underTest.queryStreams(TestData.LogsWithNamespaceLabelQuery)

        assertThat(result::successful).isTrue()
        assertThat(result::body).isNotNull()

        val body = result.body!!
        assertThat(body::status).isEqualTo("success")
        assertThat(body.streams!!.size).isGreaterThan(3)
    }


    @Test
    fun queryLogStatistics() = runTest {
        val result = underTest.queryLogStatistics(TestData.LogsWithNamespaceLabelQuery)

        assertThat(result::successful).isTrue()
        assertThat(result::body).isNotNull()

        val body = result.body!!
        assertThat(body::streams).isGreaterThan(0)
        assertThat(body::chunks).isGreaterThan(0)
        assertThat(body::entries).isGreaterThan(0)
        assertThat(body::bytes).isGreaterThan(0)
    }


    @Test
    fun queryLogValueRange() = runTest {
        val result = underTest.queryLogValueRange(TestData.LogsWithJobLabelQuery, since = LokiApiClient.SinceMaxValue, step = "1d")

        assertThat(result::successful).isTrue()
        assertThat(result::body).isNotNull()

        val body = result.body!!
        assertThat(body::status).isEqualTo("success")
        assertThat(body.data::resultType).isEqualTo("matrix")
        assertThat(body.data::result).isNotEmpty()

        val byJob = body.data.result.map { it.metric["job"] to it.values.sumOf { it.value } }
            .sortedByDescending { it.second }
        if (byJob.isNotEmpty()) {}
    }

    @Test
    fun queryLogValueRangeOfJobByNamespace() = runTest {
        val result = underTest.queryLogValueRange("""job="fluentd",namespace="monitoring"""", since = LokiApiClient.SinceMaxValue, step = "1d", targetLabels = listOf("app"))

        assertThat(result::successful).isTrue()
        assertThat(result::body).isNotNull()

        val body = result.body!!
        assertThat(body::status).isEqualTo("success")
        assertThat(body.data::resultType).isEqualTo("matrix")
        assertThat(body.data::result).isNotEmpty()

        val byApp = body.data.result.map { it.metric["app"] to it.values.sumOf { it.value } }
            .sortedByDescending { it.second }
        if (byApp.isNotEmpty()) {}
    }

}