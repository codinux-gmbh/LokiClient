package net.codinux.log.loki.service

import assertk.assertThat
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import kotlinx.coroutines.test.runTest
import net.codinux.log.loki.api.LokiApiClient
import net.codinux.log.loki.extensions.toLokiTimestamp
import net.codinux.log.loki.model.days
import net.codinux.log.loki.test.TestData
import net.dankito.datetime.Instant
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes

class LokiApiServiceTest {

    private val client = LokiApiClient(TestData.webClient)

    private val underTest = LokiApiService(client)


    @Test
    fun queryLogs() = runTest {
        val result = underTest.queryLogs("""job="podlogs"""", since = 1.days)

        assertThat(result::successful).isTrue()
        assertThat(result::body).isNotNull().isNotEmpty()
    }

    @Test
    fun queryMetrics() = runTest {
        val result = underTest.queryMetrics(TestData.MetricsQuery, since = 1.days)

        assertThat(result::successful).isTrue()
        assertThat(result::body).isNotNull().isNotEmpty()
    }


    @Test
    fun getAllLabels() = runTest {
        val result = underTest.getAllLabels()

        assertThat(result).isNotEmpty()
    }


    @Test
    fun getAllStreams() = runTest {
        val result = underTest.getAllStreams("""namespace=~".+"""")

        assertThat(result).isNotEmpty()
    }

    @Test
    fun analyzeLabels() = runTest(timeout = 100.minutes) {
        val result = underTest.analyzeLabels()

        assertThat(result::streams).isNotEmpty()
        assertThat(result::labels).isNotEmpty()
    }


    @Test
    fun getLogVolume() = runTest {
        val result = underTest.getLogVolume(TestData.LogsWithNamespaceLabelQuery)

        assertThat(result::successful).isTrue()
        assertThat(result::body).isNotNull().isNotEmpty()
    }

    @Test
    fun getLogVolumeGroupedByNamespaceAndApp() = runTest {
        val result = underTest.getLogVolume(TestData.LogsWithNamespaceLabelQuery, listOf("namespace", "app"))

        assertThat(result::successful).isTrue()
        assertThat(result::body).isNotNull().isNotEmpty()
    }


    @Test
    fun requestLogDeletion() = runTest {
        val now = Instant.now()

        // request a very small time window, so that if cancelling below fails it's very unlikely that really data gets deleted
        val result = underTest.requestLogDeletion("""{app="loki"} |= "compacting"""", now.minusMilliseconds(1).toLokiTimestamp(), now.toLokiTimestamp())

        assertThat(result::successful).isTrue()
        assertThat(result::body).isNotNull()

        val createdLogDeletionRequest = result.body!!

        val cancellationResult = client.requestCancellationOfDeleteRequest(createdLogDeletionRequest.requestId)

        assertThat(cancellationResult::successful).isTrue()
        assertThat(cancellationResult::body).isNotNull().isTrue()
    }

}