import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

val kafkaVersion = "2.4.0"
val junitJupiterVersion = "5.7.0"
val jacksonVersionDatabind = "2.13.4.2"
val jacksonVersion = "2.13.4"
val testcontainersVersion = "1.16.2"

plugins {
    kotlin("jvm") version "1.7.0"
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://packages.confluent.io/maven/")
}

dependencies {
    api(kotlin("stdlib-jdk8"))
    api("org.apache.kafka:kafka-clients:$kafkaVersion")

    implementation("org.jetbrains.kotlin:kotlin-reflect:1.4.10")
    implementation("org.flywaydb:flyway-core:7.0.4")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("no.nav:vault-jdbc:1.3.7")
    implementation("org.postgresql:postgresql:42.5.0")
    implementation("com.github.seratch:kotliquery:1.3.1")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("net.logstash.logback:logstash-logback-encoder:6.4") {
        exclude("com.fasterxml.jackson.core")
        exclude("com.fasterxml.jackson.dataformat")
    }
    implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersionDatabind")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    testImplementation("no.nav:kafka-embedded-env:3.2.1")
    constraints {
        testImplementation("org.apache.kafka:3.3.1")
        testImplementation("org.glassfish.jersey.core:jersey-server:2.31")
    }
    testImplementation("com.opentable.components:otj-pg-embedded:0.13.3")
    testImplementation("io.mockk:mockk:1.10.2")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")

    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "17"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "17"
    }

    named<Jar>("jar") {
        archiveFileName.set("app.jar")

        manifest {
            attributes["Main-Class"] = "no.nav.helse.sparker.ApplicationKt"
            attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(separator = " ") {
                it.name
            }
        }

        doLast {
            configurations.runtimeClasspath.get().forEach {
                val file = File("$buildDir/libs/${it.name}")
                if (!file.exists()) {
                    it.copyTo(file)
                }
            }
        }
    }

    withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
            showCauses = true
            showExceptions = true
            exceptionFormat = FULL
            showStackTraces = true
        }
    }

    withType<Wrapper> {
        gradleVersion = "7.4.2"
    }
}
