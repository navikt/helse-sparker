package no.nav.helse.sparker

import no.nav.common.KafkaEnvironment
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import java.time.Duration
import java.time.LocalDate
import java.util.*
import kotlin.collections.set

@TestInstance(Lifecycle.PER_CLASS)
internal class ComponentTest {

    private lateinit var producer: KafkaProducer<String, String>
    private val topic = "test-topic"
    private val topicInfos = listOf(
        KafkaEnvironment.TopicInfo(topic, partitions = 1)
    )
    private val embeddedKafkaEnvironment = KafkaEnvironment(
        autoStart = false,
        noOfBrokers = 1,
        topicInfos = topicInfos,
        withSchemaRegistry = false,
        withSecurity = false
    )


    @BeforeAll
    fun `setup`() {
        embeddedKafkaEnvironment.start()
        producer = KafkaProducer(baseConfig().toProducerConfig())

        val (fom1, tom1) = LocalDate.of(2020, 3, 1) to LocalDate.of(2020, 3, 15)

        repeat(42) { producer.send(ProducerRecord(topic, utbetaling(fom1, tom1))) }
        repeat(10) { producer.send(ProducerRecord(topic, bareTull(fom1, tom1))) }
        producer.flush()
        producer.close()
    }

    @Test
    fun `it worke`() {
        val kafkaConfig = KafkaConfig(
            topicName = topic,
            bootstrapServers = embeddedKafkaEnvironment.brokersURL,
            username = "username",
            password = "password"
        )
        val daoMock = FagsystemIdDaoMock()
        val etterbetalingHåntdterer = EtterbetalingHåntdterer(daoMock, kafkaConfig.topicName, LocalDate.now())

        finnUtbetalingerJob(kafkaConfig, LocalDate.now(), etterbetalingHåntdterer)

        val consumer = KafkaConsumer(kafkaConfig.consumerConfig(), StringDeserializer(), StringDeserializer())
        assertEquals(consumer.poll(Duration.ofMillis(100)).count(), 53)

    }

    @AfterAll
    fun `cleanup`() {
        embeddedKafkaEnvironment.tearDown()
    }

    private fun baseConfig(): Properties = Properties().also {
        it.load(this::class.java.getResourceAsStream("/kafka_base.properties"))
        it.remove("security.protocol")
        it.remove("sasl.mechanism")
        it["bootstrap.servers"] = embeddedKafkaEnvironment.brokersURL
    }
}


private fun utbetaling(fom: LocalDate, tom: LocalDate) = """
    {
      "type": "SykepengerUtbetalt_v1",
      "opprettet": "2020-04-29T12:00:00",
      "aktørId": "aktørId",
      "fødselsnummer": "fnr",
      "førsteStønadsdag": "$fom",
      "sisteStønadsdag": "$tom",
      "førsteFraværsdag": "$fom",
      "forbrukteStønadsdager": 123
    }
"""

private fun bareTull(fom: LocalDate, tom: LocalDate) = """
    {
      "type": "BareTull_v1",
      "opprettet": "2020-04-29T12:00:00",
      "aktørId": "aktørId",
      "fødselsnummer": "fnr",
      "førsteStønadsdag": "$fom",
      "sisteStønadsdag": "$tom",
      "førsteFraværsdag": "$fom",
      "forbrukteStønadsdager": 123
    }
"""

fun Properties.toProducerConfig(): Properties = Properties().also {
    it.putAll(this)
    it[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
    it[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
}
