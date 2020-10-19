package no.nav.helse.sparker

import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.OffsetAndTimestamp
import org.apache.kafka.common.PartitionInfo
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

fun main() {
    val config = System.getenv().let { env ->
        KafkaConfig(
            topicName = "helse-rapid-v1",
            bootstrapServers = env.getValue("KAFKA_BOOTSTRAP_SERVERS"),
            username = "/var/run/secrets/nais.io/service_user/username".readFile(),
            password = "/var/run/secrets/nais.io/service_user/password".readFile(),
            truststore = env["NAV_TRUSTSTORE_PATH"],
            truststorePassword = env["NAV_TRUSTSTORE_PASSWORD"]
        )
    }

    finnUtbetalingerJob(config, LocalDate.now())
}

internal fun finnUtbetalingerJob(kafkaConfig: KafkaConfig, startTime: LocalDate) {
    val logger = LoggerFactory.getLogger("no.nav.helse.sparker")
    Thread.setDefaultUncaughtExceptionHandler { _, throwable -> logger.error(throwable.message, throwable) }

    val consumer = klargjørConsumer(kafkaConfig, startTime)

    while (true) {
        consumer.poll(Duration.ofMillis(100)).let { records ->
            if (records.isEmpty) {
                logger.info("Alle meldinger prosessert.")
                return
            }
            println("Mottok ${records.count()} meldinger")
            records.forEach { record ->
                logger.info("Key: ${record.key()} Value: ${record.value()} Partition: ${record.partition()} Offset: ${record.offset()}")
            }
        }
    }
}

private fun klargjørConsumer(kafkaConfig: KafkaConfig, startTime: LocalDate): KafkaConsumer<String, String> {
    val consumer = KafkaConsumer(kafkaConfig.consumerConfig(), StringDeserializer(), StringDeserializer())

    // Get the list of partitions and transform PartitionInfo into TopicPartition
    val topicPartitions: List<TopicPartition> = consumer.partitionsFor(kafkaConfig.topicName)
        .map { info: PartitionInfo -> TopicPartition(kafkaConfig.topicName, info.partition()) }

    // Assign the consumer to these partitions
    consumer.assign(topicPartitions)

    val startTimeMillis = startTime.toMillis()
    // Look for offsets based on timestamp
    val partitionOffsets: Map<TopicPartition, OffsetAndTimestamp?> =
        consumer.offsetsForTimes(topicPartitions.associateBy({ it }, { startTimeMillis }))

    // Force the consumer to seek for those offsets
    partitionOffsets.forEach { (tp: TopicPartition, offsetAndTimestamp: OffsetAndTimestamp?) ->
        consumer.seek(tp, offsetAndTimestamp?.offset() ?: 0)
    }

    return consumer
}

private fun LocalDate.toMillis() = atStartOfDay(ZoneId.of("Europe/Oslo")).toEpochSecond()

private fun String.readFile() = File(this).readText(Charsets.UTF_8)
