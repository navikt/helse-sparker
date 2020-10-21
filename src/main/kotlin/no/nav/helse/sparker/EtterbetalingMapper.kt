package no.nav.helse.sparker

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate
import java.util.*


fun mapTilEtterbetalingEvent(inputNode: JsonNode, gyldighetsdato: LocalDate): Map<String, Any> =
    mapOf(
        "@id" to UUID.randomUUID(),
        "@event_name" to "Etterbetalingskandidat_v1",
        "@opprettet" to LocalDate.now(),
        "fagsystemId" to inputNode["fagsystemId"].asText(),
        "aktørId" to inputNode["aktørId"].asText(),
        "fødselsnummer" to inputNode["fødselsnummer"].asText(),
        "organisasjonsnummer" to inputNode["organisasjonsnummer"].asText(),
        "gyldighetsdato" to gyldighetsdato
    )

