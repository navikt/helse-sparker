package no.nav.helse.sparker

import com.fasterxml.jackson.databind.JsonNode
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.LocalDate

class EtterbetalingHåntdterer(
    private val fagsystemIdDao: FagsystemIdDao,
    private val topic: String,
    private val gyldighetsdato: LocalDate
) {

    internal fun håndter(node: JsonNode, producer: KafkaProducer<String, String>) {
        if (fagsystemIdDao.alleredeHåndtert("fagsystemId")) return
        producer.send(ProducerRecord(topic,
            objectMapper.writeValueAsString(mapTilEtterbetalingEvent(node, gyldighetsdato)))) { _, _ ->
            fagsystemIdDao.lagre("fagsystemId")
        }
    }


}
