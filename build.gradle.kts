import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

val kafkaVersion = "2.4.0"
val junitJupiterVersion = "5.7.0"

plugins {
    kotlin("jvm") version "1.4.10"
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
    implementation("com.zaxxer:HikariCP:3.4.5")
    implementation("no.nav:vault-jdbc:1.3.7")
    implementation("org.postgresql:postgresql:42.2.13")
    implementation("com.github.seratch:kotliquery:1.3.1")
    implementation("org.slf4j:slf4j-api:1.3.1")

    testImplementation("no.nav:kafka-embedded-env:2.5.0")
    constraints {
        testImplementation("org.apache.kafka:kafka_2.12:2.6.0")
        testImplementation("org.glassfish.jersey.core:jersey-server:2.31")
    }
    testImplementation("com.opentable.components:otj-pg-embedded:0.13.3")
    testImplementation("io.mockk:mockk:1.10.2")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "14"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "14"
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
                if (!file.exists())
                    it.copyTo(file)
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
        gradleVersion = "6.7"
    }
}
