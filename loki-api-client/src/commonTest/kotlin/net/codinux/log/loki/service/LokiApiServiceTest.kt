package net.codinux.log.loki.service

import assertk.assertThat
import assertk.assertions.isNotEmpty
import kotlinx.coroutines.test.runTest
import net.codinux.log.loki.api.LokiApiClient
import net.codinux.log.loki.test.TestData
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes

class LokiApiServiceTest {

    private val underTest = LokiApiService(LokiApiClient(TestData.webClient))


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

        assertThat(result.streams).isNotEmpty()
        assertThat(result.labels).isNotEmpty()
    }

}