package no.nav.helse.sparker


import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import java.io.File


fun main() {
    val env = System.getenv()
    if ("true" == env["CRON_JOB_MODE"]?.toLowerCase()) return avstemmingJob(env)
    rapidApp(env)
}

private fun rapidApp(env: Map<String, String>) {
    val dataSourceBuilder = DataSourceBuilder(env)
    val dataSource = dataSourceBuilder.getDataSource()

    RapidApplication.create(env).apply {
    }.apply {
        register(object : RapidsConnection.StatusListener {
            override fun onStartup(rapidsConnection: RapidsConnection) {
                dataSourceBuilder.migrate()
            }

            override fun onShutdown(rapidsConnection: RapidsConnection) {
            }
        })
    }.start()
}

private fun avstemmingJob(env: Map<String, String>) {
    val log = LoggerFactory.getLogger("no.nav.helse.sparker")
    Thread.setDefaultUncaughtExceptionHandler { _, throwable -> log.error(throwable.message, throwable) }
    val dataSourceBuilder = DataSourceBuilder(env)
    val dataSource = dataSourceBuilder.getDataSource()
    val kafkaConfig = KafkaConfig(
        bootstrapServers = env.getValue("KAFKA_BOOTSTRAP_SERVERS"),
        username = "/var/run/secrets/nais.io/service_user/username".readFile(),
        password = "/var/run/secrets/nais.io/service_user/password".readFile(),
        truststore = env["NAV_TRUSTSTORE_PATH"],
        truststorePassword = env["NAV_TRUSTSTORE_PASSWORD"]
    )
    val strings = StringSerializer()

    KafkaProducer(kafkaConfig.producerConfig(), strings, strings).use { producer ->
        producer.flush()
    }
}


private fun String.readFile() = File(this).readText(Charsets.UTF_8)
