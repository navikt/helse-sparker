package no.nav.helse.sparker

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.OffsetAndTimestamp
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.PartitionInfo
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

val objectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)


fun main() {
    val config = System.getenv().let { env ->
        KafkaConfig(
            topicName = env.getValue("KAFKA_RAPID_TOPIC"),
            bootstrapServers = env.getValue("KAFKA_BOOTSTRAP_SERVERS"),
            username = "/var/run/secrets/nais.io/service_user/username".readFile(),
            password = "/var/run/secrets/nais.io/service_user/password".readFile(),
            truststore = env["NAV_TRUSTSTORE_PATH"],
            truststorePassword = env["NAV_TRUSTSTORE_PASSWORD"]
        )
    }
    val startDate = LocalDate.of(2020, 5, 1)
    val fagsystemIdDao = FagsystemIdDaoMock()

    val etterbetalingHåntdterer = EtterbetalingHåndterer(fagsystemIdDao, config.topicName, startDate)
    finnUtbetalingerJob(config, startDate, etterbetalingHåntdterer)
}

internal fun finnUtbetalingerJob(config: KafkaConfig, startDate: LocalDate, etterbetalingHåntdterer: EtterbetalingHåndterer) {
    val logger = LoggerFactory.getLogger("no.nav.helse.sparker")

    val consumer = klargjørConsumer(config, startDate)
    val producer = KafkaProducer(config.producerConfig(), StringSerializer(), StringSerializer())


    var count = 0

    Thread.setDefaultUncaughtExceptionHandler { _, throwable -> logger.error(throwable.message, throwable) }
    while (true) {
        consumer.poll(Duration.ofMillis(100)).let { records ->
            if (records.isEmpty) {
                logger.info("Alle meldinger prosessert.")
                consumer.unsubscribe()
                consumer.close()
                producer.flush()
                producer.close()
                logger.info("Prosessert $count utbetalinger")
                return
            }
            records
                .map {
                    objectMapper.readTree(it.value())
                }
                .filter { node ->
                    node["type"]?.asText() == "SykepengerUtbetalt_v1"
                }
                .forEach { node ->
                    etterbetalingHåntdterer.håndter(node, producer)
                    count++
                }
        }
    }
}

internal fun klargjørConsumer(kafkaConfig: KafkaConfig, startDate: LocalDate): KafkaConsumer<String, String> {
    val consumer = KafkaConsumer(kafkaConfig.consumerConfig(), StringDeserializer(), StringDeserializer())

    // Get the list of partitions and transform PartitionInfo into TopicPartition
    val topicPartitions: List<TopicPartition> = consumer.partitionsFor(kafkaConfig.topicName)
        .map { info: PartitionInfo -> TopicPartition(kafkaConfig.topicName, info.partition()) }

    // Assign the consumer to these partitions
    consumer.assign(topicPartitions)

    val startTimeMillis = startDate.toMillis()
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
