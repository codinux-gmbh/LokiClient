# Loki Client
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/net.codinux.log.loki/loki-client/badge.svg)](https://maven-badges.herokuapp.com/maven-central/net.codinux.log.loki/loki-client)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)


**LokiClient** implements the [Grafana Loki](https://grafana.com/docs/loki) [HTTP API](https://grafana.com/docs/loki/latest/reference/loki-http-api) for all Kotlin Multiplatform targets.

It lets you **send logs**, **query log streams**, and **analyze log data** from **any Kotlin target**. 
So you can build cross-platform logging tools or dashboards and integrate logging into monitoring and alerting workflows.


## Features

* **Query Logs**: Query logs using LogQL with full support for range and instant queries.
* **Push Logs**: Send structured log entries to Loki including support for structured metadata.
* **Explore Labels**: Analyze available labels and their possible values to understand the structure of your log data.
* **Analyze Streams**: Explore log streams (series) based on their label combinations for better observability.
* **Inspect Log Volume**: Monitor storage usage per label, stream, or time window.
* **Pattern Detection**: Identify frequently occurring log messages to detect anomalies, reduce noise, and highlight important trends.
* **Flexible Log Analysis**: Extract structured information from logs and perform powerful, custom data analysis using Kotlin’s language features and standard libraries.
* **Delete Logs**: Delete logs you don't need anymore to compact log data.


## Setup

### Gradle

```
implementation("net.codinux.log.loki:loki-client:0.5.0")
```

### Maven

```xml
<dependency>
   <groupId>net.codinux.log.loki</groupId>
   <artifactId>loki-client-jvm</artifactId>
   <version>0.5.0</version>
</dependency>
```



## Configuration

### Add WebClient implementation

Add an implementation of the WebClient API like `ktor-web-client` (for Kotlin Multiplatform) or `java-http-client-web-client` (for dependency-less Java 11+ HttpClient, e.g. for native image):

```kotlin
implementation("net.dankito.web:ktor-web-client:1.6.0")
```

### Loki endpoint and authentication

Now configure Loki base url and authentication and instantiate WebClient implementation:

```kotlin
private val webClient = KtorWebClient()

private val client = LokiClient("http://localhost:3100", webClient)

// or

private val config = LokiConfig(
    baseUrl = "http://localhost:3100",
    authentication = BasicAuthAuthentication("username", "password"),
    internalEndpointsPathPrefix = null
)

private val clientAuthenticated = LokiClient(config, webClient)
```


### LokiService

`LokiClient` implements the Loki API as is. For a higher level abstraction use `LokiService` which uses `LokiClient` under the hood:

```kotlin
private val service = LokiService(client)
```

See examples below:


## Usage Examples

### Query logs

```kotlin
// query logs of namespace 'monitoring' of last 2 hours
val result = service.queryLogs(query = """{namespace="monitoring"}""", start = LokiTimestamp(Instant.now().minusHours(2)))
result.mapBodyOnSuccess { logs ->
    println("Retrieved ${logs.size} logs:")
    logs.forEachIndexed { index, log -> println("[${index + 1}] $log") }
}

// other parameters:

// Alternatively to `start` you can use `since`, which determines the start relative to `end`.
// The sort order can be set with `direction` and the max results with `limit`.
// If `query` starts and ends with curly braces, the curly braces can be left away.
service.queryLogs("namespace=\"monitoring\"", since = 2.hours, end = LokiTimestamp.ofDate(2025, 8, 5), 
    direction = SortOrder.Backward, limit = 500)
```


### Push logs

```kotlin
service.ingestLogs(
    LogEntryToSave(timestamp = LokiTimestamp.now(), message = "Something important happened"),
    LogEntryToSave(timestamp = LokiTimestamp.now(), message = "With Labels", labels = mapOf("namespace" to "monitoring", "job" to "podlogs")),
    LogEntryToSave(timestamp = LokiTimestamp.now(), message = "With structured metadata", labels = mapOf("namespace" to "monitoring"), 
        structuredMetadata = mapOf("level" to "info", "pod" to "MonitoringApp-58f856b99-5gwtt")),
)
```


### Get labels

```kotlin
// all of the methods below can be restricted with query, start, end and since parameter

val allLabels = service.getAllLabels()

val valuesOfLabelNamespace = client.queryLabelValues("namespace")
```


### Get index volume

```kotlin
// e.g. get Log volume of each namespace
service.getIndexVolume("namespace=~\".+\"").mapBodyOnSuccess { response, indexVolumes ->
    indexVolumes.forEach { volume -> println("${volume.metrics["namespace"]}: ${volume.aggregatedValue}") }
}

// group log volume by labels like 'service_name' and aggregate by labels or series
service.getIndexVolume("", groupByLabels = listOf("service_name"), aggregateBy = AggregateBy.Labels)
```


### Pattern detection


### Analyze

```kotlin
// does the same as `logcli series --analyze-labels` does:
val results: LabelAnalyzationResults = service.analyzeLabels()
results.labels.forEach {
    println("${it.label}: Unique values: ${it.uniqueValues}. Found in Streams: ${it.foundInStreams}")
}

val streamsInNamespaceMonitoring: Set<Map<String, String>> = service.getAllStreams("namespace=~\"monitoring\"")
```


## License
```
Copyright 2025 codinux GmbH & Co. KG

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
