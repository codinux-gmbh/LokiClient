package net.codinux.log.loki.service

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import assertk.assertions.startsWith
import kotlinx.coroutines.test.runTest
import net.codinux.log.loki.api.LokiApiClient
import net.codinux.log.loki.extensions.toLokiTimestamp
import net.codinux.log.loki.model.LogEntryToSave
import net.codinux.log.loki.model.days
import net.codinux.log.loki.model.seconds
import net.codinux.log.loki.test.TestData
import net.dankito.datetime.Instant
import kotlin.test.Ignore
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


    @Ignore // don't execute automatically, would save a lot of test data in Loki and is therefore not non-destructive
    @Test
    fun ingestLogs() = runTest {
        val timestamp = Instant.now()

        val result = underTest.ingestLogs(listOf(
            LogEntryToSave(timestamp.toLokiTimestamp(), "Test 1", mapOf("namespace" to "default", "app" to "test")),
            LogEntryToSave(timestamp.toLokiTimestamp(), "Test 2", mapOf("namespace" to "default", "app" to "test"), mapOf("level" to "info")),
        ))

        assertThat(result::successful).isTrue()
        assertThat(result::body).isNotNull().isTrue()


        val queryResult = underTest.queryLogs("""{namespace="default",app="test"}""", since = 10.seconds)
        assertThat(queryResult::successful).isTrue()
        assertThat(queryResult::body).isNotNull().hasSize(2)

        val savedLogEntries = queryResult.body!!
        savedLogEntries.forEach { entry ->
            assertThat(entry::entries).hasSize(1)
            assertThat(entry.entries.first()::timestamp).isEqualTo(timestamp)
            assertThat(entry.entries.first()::message).startsWith("Test ")

            assertThat(entry::stream.get().size).isGreaterThanOrEqualTo(4)
            assertThat(entry.stream["namespace"]).isEqualTo("default")
            assertThat(entry.stream["app"]).isEqualTo("test")
        }
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