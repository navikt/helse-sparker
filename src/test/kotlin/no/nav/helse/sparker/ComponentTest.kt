package no.nav.helse.sparker

import no.nav.common.KafkaEnvironment
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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

        val førsteFrværsdag = LocalDate.of(2020, 3, 1)

        repeat(42) { producer.send(ProducerRecord(topic, utbetaling())) }
        repeat(10) { producer.send(ProducerRecord(topic, utbetaling_ikke_støttet(førsteFrværsdag))) }
        repeat(10) { producer.send(ProducerRecord(topic, bareTull(førsteFrværsdag))) }
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
        val etterbetalingHåntdterer = EtterbetalingHåndterer(daoMock, kafkaConfig.topicName, LocalDate.now())

        finnUtbetalingerJob(kafkaConfig, LocalDate.now(), etterbetalingHåntdterer)

        val consumer = KafkaConsumer<String, String>(baseConfig().toConsumerConfig())
        consumer.assign(listOf(TopicPartition(topic, 0)))
        consumer.seekToBeginning(consumer.assignment())
        consumer.poll(Duration.ofMillis(100)).let {
            val testdata = 42 + 10 + 10
            val appdata = 1
            assertEquals(testdata + appdata, it.count())
            it.last().run {
                assertEquals("22027821111", key())
                mapOf(
                    "fagsystemId" to "YNQXJGM73ZHPBBTM7LVG5RJPYM",
                    "aktørId" to "1000000000091",
                    "fødselsnummer" to "22027821111",
                    "organisasjonsnummer" to "971555001",
                    "gyldighetsdato" to "2020-12-01",
                ).forEach { (key, value) ->
                    assertTrue(value().contains(""""$key":"$value"""))
                }
            }
        }
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

@Language("JSON")
private fun utbetaling_ikke_støttet(førsteFraværsdag: LocalDate) = """
    {
      "@event_name": "utbetalt",
      "opprettet": "2020-04-29T12:00:00",
      "aktørId": "aktørId",
      "fødselsnummer": "fnr",
      "førsteFraværsdag": "$førsteFraværsdag",
      "fagsystemId": "FAGSYSTEM_ID",
      "organisasjonsnummer": "1234"
    }
"""

@Language("JSON")
private fun utbetaling() = """{
    "aktørId": "1000000000091",
    "fødselsnummer": "22027821111",
    "organisasjonsnummer": "971555001",
    "hendelser": [
      "7cbe2ffb-b344-4691-8900-e6d4df0679ab",
      "924c3209-a0f1-48b9-a57f-d3d8962aaebc"
    ],
    "utbetalt": [
      {
        "mottaker": "971555001",
        "fagområde": "SPREF",
        "fagsystemId": "YNQXJGM73ZHPBBTM7LVG5RJPYM",
        "totalbeløp": 27421,
        "utbetalingslinjer": [
          {
            "fom": "2020-04-20",
            "tom": "2020-05-12",
            "dagsats": 1613,
            "beløp": 1613,
            "grad": 100,
            "sykedager": 17
          }
        ]
      },
      {
        "mottaker": "22027821111",
        "fagområde": "SP",
        "fagsystemId": "F35FQNCJAFC73DMKM2DW2XYUMA",
        "totalbeløp": 0,
        "utbetalingslinjer": []
      }
    ],
    "fom": "2020-04-20",
    "tom": "2020-05-12",
    "forbrukteSykedager": 140,
    "gjenståendeSykedager": 108,
    "opprettet": "2020-05-13T05:35:38.057426",
    "system_read_count": 0,
    "system_participating_services": [
      {
        "service": "spleis",
        "instance": "spleis-69b7cb4bf4-frxdw",
        "time": "2020-05-13T16:15:41.81766"
      }
    ],
    "@event_name": "utbetalt",
    "@id": "34840b34-f9c0-4153-928d-7bb4e8b379ce",
    "@opprettet": "2020-05-13T16:15:41.81768",
    "@forårsaket_av": {
      "event_name": "behov",
      "id": "e75856ca-3d5c-4987-b755-8df834e82331",
      "opprettet": "2020-05-13T16:15:41.667048"
    }
  }
"""

@Language("JSON")
private fun bareTull(førsteFraværsdag: LocalDate) = """
    {
      "type": "BareTull_v1",
      "opprettet": "2020-04-29T12:00:00",
      "aktørId": "aktørId",
      "fødselsnummer": "fnr",
      "førsteFraværsdag": "$førsteFraværsdag",
      "fagsystemId": "FAGSYSTEM_ID",
      "organisasjonsnummer": "1234"
    }
"""

fun Properties.toProducerConfig(): Properties = Properties().also {
    it.putAll(this)
    it[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
    it[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
}

fun Properties.toConsumerConfig(): Properties = Properties().also {
    it.putAll(this)
    it[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
    it[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
    it[ConsumerConfig.GROUP_ID_CONFIG] = "sparker-test"
    it[ConsumerConfig.CLIENT_ID_CONFIG] = "test"
}
