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
}