package no.nav.helse.sparker

import com.fasterxml.jackson.databind.JsonNode
import net.logstash.logback.argument.StructuredArguments.keyValue
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import java.time.LocalDate

class EtterbetalingHåndterer(
    private val fagsystemIdDao: FagsystemIdDao,
    private val topic: String,
    private val gyldighetsdato: LocalDate
) {

    val sikkerlogg = LoggerFactory.getLogger("tjenestekall")
    val logger = LoggerFactory.getLogger(this.javaClass)

    internal fun håndter(node: JsonNode, producer: KafkaProducer<String, String>) {
        if (!node.has("utbetalt")) {
            sikkerlogg.info("Feltet 'utbetalt' mangler på denne", keyValue("utbetaltEvent", node))
            return
        }
        node["utbetalt"].filter { !it["utbetalingslinjer"].isEmpty }.forEach { utbetaling ->
            val fagsystemId = utbetaling["fagsystemId"].textValue()
            if (fagsystemIdDao.alleredeHåndtert(fagsystemId)) return
            producer.send(
                ProducerRecord(
                    topic,
                    node["fødselsnummer"].asText(),
                    objectMapper.writeValueAsString(mapTilEtterbetalingEvent(node, gyldighetsdato, fagsystemId))
                )
            ).get().let {
                fagsystemIdDao.lagre(fagsystemId)
            }
        }
    }
}


