import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl


plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}


kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        // suppresses compiler warning: [EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING] 'expect'/'actual' classes (including interfaces, objects, annotations, enums, and 'actual' typealiases) are in Beta.
        freeCompilerArgs.add("-Xexpect-actual-classes")

        // avoid "variable has been optimised out" in debugging mode
        if (System.getProperty("idea.debugger.dispatch.addr") != null) {
            freeCompilerArgs.add("-Xdebug")
        }
    }


    jvmToolchain(11)

    jvm()

    js {
        binaries.library()

        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                    useFirefoxHeadless()
                }
            }
        }

        nodejs {
            testTask {
                useMocha {
                    timeout = "20s" // Mocha times out after 2 s, which is too short for bufferExceeded() test
                }
            }
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                    useFirefoxHeadless()
                }
            }
        }
    }


    linuxX64()
    mingwX64()

    iosX64()
    iosArm64()
    iosSimulatorArm64()
    macosX64()
    macosArm64()
    watchosArm64()
    watchosSimulatorArm64()
    tvosArm64()
    tvosSimulatorArm64()

    applyDefaultHierarchyTemplate()


    val kotlinxSerializationVersion: String by project
    val coroutinesVersion: String by project

    val kmpDateTimeVersion: String by project
    val kmpBaseVersion: String by project
    val jacksonVersion: String by project
    val webClientVersion: String by project
    val klfVersion: String by project

    val assertKVersion: String by project
    val logbackVersion: String by project

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinxSerializationVersion")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion") // for ValuePointSerializer

            implementation("net.dankito.datetime:kmp-datetime:$kmpDateTimeVersion")
            implementation("net.codinux.kotlin:kmp-base:$kmpBaseVersion")

            api("net.dankito.web:web-client-api:$webClientVersion")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))

            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")

            implementation("net.dankito.web:ktor-web-client:$webClientVersion")

            implementation("com.willowtreeapps.assertk:assertk:$assertKVersion")
        }

        jvmMain.dependencies {
            // needed for @JsonIgnore of kmp-base
            compileOnly("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
        }
        jvmTest.dependencies {
            implementation("ch.qos.logback:logback-classic:$logbackVersion")
        }
    }
}


if (File(projectDir, "../gradle/scripts/publish-codinux.gradle.kts").exists()) {
    apply(from = "../gradle/scripts/publish-codinux.gradle.kts")
}