package net.codinux.log.loki.client

import assertk.assertThat
import assertk.assertions.*
import kotlinx.coroutines.test.runTest
import net.codinux.log.loki.client.dto.ResultType
import net.codinux.log.loki.extensions.minusThirtyDays
import net.codinux.log.loki.model.LokiTimestamp
import net.codinux.log.loki.model.PrometheusDuration
import net.codinux.log.loki.model.days
import net.codinux.log.loki.test.TestData
import net.dankito.datetime.Instant
import net.dankito.datetime.LocalDate
import kotlin.test.Ignore
import kotlin.test.Test

class LokiClientTest {

    private val underTest = LokiClient(TestData.lokiConfig, TestData.webClient)


    @Test
    fun rangeQuery_StreamsResult() = runTest {
        val result = underTest.rangeQuery("""{namespace="kube-system"}""", since = 2.days)

        assertThat(result::successful).isTrue()
        assertThat(result::body).isNotNull()

        val body = result.body!!
        assertThat(body::type).isEqualTo(ResultType.Streams)
        assertThat(body::streams).isNotNull().isNotEmpty()
    }

    @Test
    fun rangeQuery_MatrixResult() = runTest {
        val result = underTest.rangeQuery(TestData.MetricsQuery)

        assertThat(result::successful).isTrue()
        assertThat(result::body).isNotNull()

        val body = result.body!!
        assertThat(body::type).isEqualTo(ResultType.Matrix)
        assertThat(body::matrix).isNotNull().isNotEmpty()
    }


    @Test
    fun instantQuery_VectorResult() = runTest {
        val result = underTest.instantQuery(TestData.MetricsQuery)

        assertThat(result::successful).isTrue()
        assertThat(result::body).isNotNull()

        val body = result.body!!
        assertThat(body::type).isEqualTo(ResultType.Vector)
        assertThat(body::vector).isNotNull().isNotEmpty()
    }


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
    fun queryIndexVolume() = runTest {
        val result = underTest.queryIndexVolume(TestData.LogsWithJobLabelQuery, since = PrometheusDuration.SinceMaxValue)

        assertThat(result::successful).isTrue()
        assertThat(result::body).isNotNull()

        val body = result.body!!
        assertThat(body::status).isEqualTo("success")
        assertThat(body.data::resultType).isEqualTo("vector")
        assertThat(body.data::result).isNotEmpty()

        val vectors = body.data.result
        val today = LocalDate.today()
        vectors.forEach { vector ->
            assertThat(vector.value::valueAsLong).isGreaterThan(0)
            assertThat(vector.value.timestamp.toLocalDateTimeAtSystemTimeZone().date).isEqualTo(today)
        }
    }

    @Test
    fun queryIndexVolumeRange() = runTest {
        val result = underTest.queryIndexVolumeRange(TestData.LogsWithJobLabelQuery, since = PrometheusDuration.SinceMaxValue, step = "1d")

        assertThat(result::successful).isTrue()
        assertThat(result::body).isNotNull()

        val body = result.body!!
        assertThat(body::type).isEqualTo(ResultType.Matrix)
        assertThat(body::matrix).isNotNull()
        val data = body.matrix!!

        val byJob = data.map { it.metric["job"] to it.values.sumOf { it.valueAsLong } }
            .sortedByDescending { it.second }
        if (byJob.isNotEmpty()) {}
    }

    @Test
    fun queryIndexVolumeRangeOfJobByNamespace() = runTest {
        val result = underTest.queryIndexVolumeRange("""job="podlogs",namespace="monitoring"""", since = PrometheusDuration.SinceMaxValue, step = "1d", targetLabels = listOf("app"))

        assertThat(result::successful).isTrue()
        assertThat(result::body).isNotNull()

        val body = result.body!!
        assertThat(body.matrix ?: body.vector).isNotNull()

        if (body.matrix != null) {
            val data = body.matrix!!
            assertThat(data).isNotEmpty()

            val byApp = data.map { it.metric["app"] to it.values.sumOf { it.valueAsLong } }
                .sortedByDescending { it.second }
            if (byApp.isNotEmpty()) {}
        } else if (body.vector != null) {
            val data = body.vector
            assertThat(data).isNotNull().isNotEmpty()
        }
    }


    @Test
    fun patternsDetection() = runTest {
        val result = underTest.patternsDetection(TestData.LogsWithJobLabelQuery, since = PrometheusDuration.SinceMaxValue)

        assertThat(result::successful).isTrue()
        assertThat(result::body).isNotNull()

        val body = result.body!!
        assertThat(body::status).isEqualTo("success")

        val detectedPatterns = body.data
        detectedPatterns.forEach { pattern ->
            assertThat(pattern::pattern).isNotEmpty()
            assertThat(pattern::samples).isNotEmpty()
            pattern.samples.forEach { sample ->
                assertThat(sample::valueAsLong).isGreaterThan(0)
            }
        }
    }


    @Test
    fun listLogDeletionRequests() = runTest {
        val result = underTest.listLogDeletionRequests()

        assertThat(result::successful).isTrue()
        assertThat(result::body).isNotNull().isNotEmpty()
    }

    @Ignore // destructive test, don't execute automatically
    @Test
    fun requestLogDeletion_QueryWithLogLine() = runTest {
        val start = Instant.now().minusThirtyDays().minusThirtyDays().minusThirtyDays()

        val result = underTest.requestLogDeletion("""{app="loki"} |= "compacting"""", LokiTimestamp(start))

        assertThat(result::successful).isTrue()
        assertThat(result::body).isNotNull().isTrue()
    }

    @Ignore // only works if a log delete request has been created before
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