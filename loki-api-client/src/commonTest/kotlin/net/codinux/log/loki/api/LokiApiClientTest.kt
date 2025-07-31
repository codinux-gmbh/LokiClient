package net.codinux.log.loki.api

import assertk.assertThat
import assertk.assertions.*
import kotlinx.coroutines.test.runTest
import net.codinux.log.loki.extensions.minusThirtyDays
import net.codinux.log.loki.test.TestData
import net.dankito.datetime.Instant
import net.dankito.datetime.LocalDate
import kotlin.test.Test

class LokiApiClientTest {

    private val underTest = LokiApiClient(TestData.webClient, "/loki/internal")


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
    fun queryLogVolume() = runTest {
        val result = underTest.queryLogVolume(TestData.LogsWithJobLabelQuery, since = LokiApiClient.SinceMaxValue)

        assertThat(result::successful).isTrue()
        assertThat(result::body).isNotNull()

        val body = result.body!!
        assertThat(body::status).isEqualTo("success")
        assertThat(body.data::resultType).isEqualTo("vector")
        assertThat(body.data::result).isNotEmpty()

        val vectors = body.data.result
        val today = LocalDate.today()
        vectors.forEach { vector ->
            assertThat(vector.value::value).isGreaterThan(0)
            assertThat(vector.value.timestamp.toLocalDateTimeAtSystemTimeZone().date).isEqualTo(today)
        }
    }

    @Test
    fun queryLogVolumeRange() = runTest {
        val result = underTest.queryLogVolumeRange(TestData.LogsWithJobLabelQuery, since = LokiApiClient.SinceMaxValue, step = "1d")

        assertThat(result::successful).isTrue()
        assertThat(result::body).isNotNull()

        val body = result.body!!
        assertThat(body::status).isEqualTo("success")
        assertThat(body::matrixData).isNotNull()
        val data = body.matrixData!!
        assertThat(data::resultType).isEqualTo("matrix")
        assertThat(data::result).isNotEmpty()

        val byJob = data.result.map { it.metric["job"] to it.values.sumOf { it.value } }
            .sortedByDescending { it.second }
        if (byJob.isNotEmpty()) {}
    }

    @Test
    fun queryLogVolumeRangeOfJobByNamespace() = runTest {
        val result = underTest.queryLogVolumeRange("""job="fluentd",namespace="monitoring"""", since = LokiApiClient.SinceMaxValue, step = "1d", targetLabels = listOf("app"))

        assertThat(result::successful).isTrue()
        assertThat(result::body).isNotNull()

        val body = result.body!!
        assertThat(body::status).isEqualTo("success")
        assertThat(body::matrixData).isNotNull()
        val data = body.matrixData!!
        assertThat(data::resultType).isEqualTo("matrix")
        assertThat(data::result).isNotEmpty()

        val byApp = data.result.map { it.metric["app"] to it.values.sumOf { it.value } }
            .sortedByDescending { it.second }
        if (byApp.isNotEmpty()) {}
    }


    @Test
    fun patternsDetection() = runTest {
        val result = underTest.patternsDetection(TestData.LogsWithJobLabelQuery, since = LokiApiClient.SinceMaxValue)

        assertThat(result::successful).isTrue()
        assertThat(result::body).isNotNull()

        val body = result.body!!
        assertThat(body::status).isEqualTo("success")

        val detectedPatterns = body.data
        detectedPatterns.forEach { pattern ->
            assertThat(pattern::pattern).isNotEmpty()
            assertThat(pattern::samples).isNotEmpty()
            pattern.samples.forEach { sample ->
                assertThat(sample::value).isGreaterThan(0)
            }
        }
    }


    @Test
    fun requestLogDeletion() = runTest {
        val end = Instant.now().minusThirtyDays()
        val start = end.minusThirtyDays()

        val result = underTest.requestLogDeletion("""{app="mongodb"}""", start)

        assertThat(result).isTrue()
    }

    @Test
    fun requestLogDeletion_QueryWithLogLine() = runTest {
        val start = Instant.now().minusThirtyDays()

        val result = underTest.requestLogDeletion("""{app="loki"} |= "compacting"""", start)

        assertThat(result).isTrue()
    }

    @Test
    fun listLogDeletionRequests() = runTest {
        val result = underTest.listLogDeletionRequests()

        assertThat(result::successful).isTrue()
        assertThat(result::body).isNotNull().isNotEmpty()
    }

    @Test
    fun requestCancellationOfDeleteRequest() = runTest {
        val result = underTest.requestCancellationOfDeleteRequest("7ef08f71")

        assertThat(result::successful).isTrue()
        assertThat(result::body).isNotNull().isTrue()
    }


    @Test
    fun getBuildInformation() = runTest {
        val result = underTest.getBuildInformation()

        assertThat(result::successful).isTrue()
        assertThat(result::body).isNotNull()

        val buildInformation = result.body!!
        assertThat(buildInformation::version).isNotEmpty()
        assertThat(buildInformation::revision).isNotEmpty()
        assertThat(buildInformation::branch).isNotEmpty()
        assertThat(buildInformation::buildUser).isNotEmpty()
        assertThat(buildInformation::buildDate.get().dateTime.date.year).isGreaterThanOrEqualTo(2025)
    }


    @Test
    fun ready() = runTest {
        val result = underTest.ready()

        assertThat(result::successful).isTrue()
        assertThat(result::body).isEqualTo("ready\n")
    }

    @Test
    fun config() = runTest {
        val result = underTest.config()

        assertThat(result::successful).isTrue()
        assertThat(result::body).isNotNull().isNotEmpty()
    }

    @Test
    fun services() = runTest {
        val result = underTest.services()

        assertThat(result::successful).isTrue()
        assertThat(result::body).isNotNull().isNotEmpty()
    }

    @Test
    fun metrics() = runTest {
        val result = underTest.metrics()

        assertThat(result::successful).isTrue()
        assertThat(result::body).isNotNull().isNotEmpty()
    }

}