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
        val fagsystemId = node["fagsystemId"]?.asText()
        if (fagsystemId == null) {
            sikkerlogg.info("fagsystemId mangler på denne", keyValue("utbetaltEvent", node))
            return
        }
        if (fagsystemIdDao.alleredeHåndtert(fagsystemId)) return
        //producer.send(
        ProducerRecord<String, String>(topic, objectMapper.writeValueAsString(mapTilEtterbetalingEvent(node, gyldighetsdato)))
        fagsystemIdDao.lagre(fagsystemId)
        /*).get().let { _ ->
            fagsystemIdDao.lagre(fagsystemId)
        }*/
    }
}
