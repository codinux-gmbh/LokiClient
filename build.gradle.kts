buildscript {
    repositories {
        mavenCentral()
    }

    val kotlinVersion: String by extra

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    }
}


allprojects {
    repositories {
        mavenCentral()
        mavenLocal()
    }

    group = "net.codinux.log.loki"
    version = "0.5.1-SNAPSHOT"


    ext["projectName"] = "Loki Client"
    ext["sourceCodeRepositoryBaseUrl"] = "github.com/codinux-gmbh/LokiClient"
    ext["projectInceptionYear"] = "2025"

    ext["projectDescription"] = "Implements the Loki (https://grafana.com/docs/loki) HTTP API to query from and push logs to Loki for all Kotlin Multiplatform targets"
}