package no.nav.helse.sparker

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*


fun mapTilEtterbetalingEvent(inputNode: JsonNode, gyldighetsdato: LocalDate, fagsystemId: String): Map<String, Any> =
    mapOf(
        "@id" to UUID.randomUUID(),
        "@event_name" to "Etterbetalingskandidat_v1",
        "@opprettet" to LocalDateTime.now(),
        "fagsystemId" to fagsystemId,
        "aktørId" to inputNode["aktørId"].asText(),
        "fødselsnummer" to inputNode["fødselsnummer"].asText(),
        "organisasjonsnummer" to inputNode["organisasjonsnummer"].asText(),
        "gyldighetsdato" to gyldighetsdato
    )

