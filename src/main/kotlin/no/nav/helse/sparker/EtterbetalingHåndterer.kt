package no.nav.helse.sparker

import com.fasterxml.jackson.databind.JsonNode
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import java.time.LocalDate

class EtterbetalingHåndterer(
    private val fagsystemIdDao: FagsystemIdDao,
    private val topic: String,
    private val gyldighetsdato: LocalDate
) {

    val logger = LoggerFactory.getLogger(this.javaClass)

    internal fun håndter(node: JsonNode, producer: KafkaProducer<String, String>) {
        val fagsystemId = node["fagsystemId"].asText()
        if (fagsystemIdDao.alleredeHåndtert(fagsystemId)) return
        logger.info("Håndterer: $node")
        producer.send(ProducerRecord(topic, objectMapper.writeValueAsString(mapTilEtterbetalingEvent(node, gyldighetsdato)))).get().let { _ ->
            fagsystemIdDao.lagre(fagsystemId)
        }
    }
}
