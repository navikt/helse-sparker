package no.nav.helse.sparker

import no.nav.common.KafkaEnvironment
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
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

    lateinit var consumer: KafkaConsumer<String, String>

    @BeforeAll
    fun `setup`() {
        embeddedKafkaEnvironment.start()
        producer = KafkaProducer(baseConfig().toProducerConfig())
        consumer = KafkaConsumer(baseConfig().toConsumerConfig())

        val (fom1, tom1) = LocalDate.of(2020, 3, 1) to LocalDate.of(2020, 3, 15)

        repeat(42) { producer.send(ProducerRecord(topic, vedtak(fom1, tom1))) }
        producer.flush()
    }

    @Test
    fun `it worke`() {
        val kafkaConfig = KafkaConfig(
            topicName = topic,
            bootstrapServers = embeddedKafkaEnvironment.brokersURL,
            username = "username",
            password = "password"
        )

        finnUtbetalingerJob(kafkaConfig, LocalDate.now())
    }

    @AfterAll
    fun `cleanup`() {
        embeddedKafkaEnvironment.close()
    }

    private fun baseConfig(): Properties = Properties().also {
        it.load(this::class.java.getResourceAsStream("/kafka_base.properties"))
        it.remove("security.protocol")
        it.remove("sasl.mechanism")
        it["bootstrap.servers"] = embeddedKafkaEnvironment.brokersURL
    }
}


private fun vedtak(fom: LocalDate, tom: LocalDate) = """
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

fun Properties.toConsumerConfig() = Properties().also {
    it.putAll(this)
    it[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
    it[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
}

fun Properties.toProducerConfig(): Properties = Properties().also {
    it.putAll(this)
    it[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
    it[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
}
