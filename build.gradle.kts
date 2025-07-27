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
    version = "1.0.0-SNAPSHOT"


    ext["sourceCodeRepositoryBaseUrl"] = "github.com/codinux/LokiApi"

    ext["projectDescription"] = "Implements the Loki (https://grafana.com/docs/loki) HTTP API for all Kotlin Multiplatform targets"
}