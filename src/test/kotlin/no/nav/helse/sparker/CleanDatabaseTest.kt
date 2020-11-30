package no.nav.helse.sparker

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CleanDatabaseTest {

    private lateinit var embeddedPostgres: EmbeddedPostgres
    private lateinit var dataSource: DataSource

    private lateinit var jdbcUrl: String
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
    fun setup(@TempDir postgresPath: Path) {
        embeddedPostgres = EmbeddedPostgres.builder()
            .setOverrideWorkingDirectory(postgresPath.toFile())
            .setDataDirectory(postgresPath.resolve("datadir"))
            .start()
        jdbcUrl = embeddedPostgres.getJdbcUrl("postgres", "postgres")
        config = mapOf(
            "DATABASE_JDBC_URL" to jdbcUrl,
        )
        dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = this@CleanDatabaseTest.jdbcUrl
            maximumPoolSize = 3
            minimumIdle = 1
            idleTimeout = 10001
            connectionTimeout = 1000
            maxLifetime = 30001
        })
    }

    @AfterAll
    internal fun tearDown() {
        embeddedPostgres.close()
    }
}
