package no.nav.helse.sparker

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CleanDatabaseTest {

    private val postgres = PostgreSQLContainer<Nothing>("postgres:13")
    private lateinit var dataSource: DataSource

    private lateinit var config: Map<String, String>

    @Test
    fun `Tømmer databasen hvis angitt`() {
        // Initielt oppsett av fagsystem-tabellen
        DataSourceBuilder(config).migrate()

        val dao = PostgresFagsystemIdDao(dataSource)
        dao.lagre("fagsystemId")
        assertTrue(dao.alleredeHåndtert("fagsystemId"))

        // migrering uten CLEAN_DATABASE tømmer ikke databasen
        DataSourceBuilder(config).migrate()
        assertTrue(dao.alleredeHåndtert("fagsystemId"))

        // migrering med CLEAN_DATABASE tømmer databasen
        DataSourceBuilder(config + ("CLEAN_DATABASE" to "true")).migrate()
        assertFalse(dao.alleredeHåndtert("fagsystemId"))
    }

    @BeforeAll
    fun setup() {
        postgres.start()
        config = mapOf(
            "DATABASE_JDBC_URL" to postgres.jdbcUrl,
            "DATABASE_USERNAME" to postgres.username,
            "DATABASE_PASSWORD" to postgres.password
        )
        dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = postgres.jdbcUrl
            username = postgres.username
            password = postgres.password
            maximumPoolSize = 3
            minimumIdle = 1
            idleTimeout = 10001
            connectionTimeout = 1000
            maxLifetime = 30001
        })
    }

    @AfterAll
    internal fun tearDown() {
        postgres.stop()
    }
}
