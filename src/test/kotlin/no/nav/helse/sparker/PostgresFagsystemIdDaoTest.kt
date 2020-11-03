package no.nav.helse.sparker

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.sql.Connection
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PostgresFagsystemIdDaoTest {

    private lateinit var embeddedPostgres: EmbeddedPostgres
    private lateinit var postgresConnection: Connection
    private lateinit var dataSource: DataSource
    private lateinit var flyway: Flyway
    private lateinit var fagsystemIdDao: PostgresFagsystemIdDao

    @BeforeAll
    internal fun setupAll(@TempDir postgresPath: Path) {
        embeddedPostgres = EmbeddedPostgres.builder()
            .setOverrideWorkingDirectory(postgresPath.toFile())
            .setDataDirectory(postgresPath.resolve("datadir"))
            .start()
        postgresConnection = embeddedPostgres.postgresDatabase.connection

        dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = embeddedPostgres.getJdbcUrl("postgres", "postgres")
            maximumPoolSize = 3
            minimumIdle = 1
            idleTimeout = 10001
            connectionTimeout = 1000
            maxLifetime = 30001
        })

        flyway = Flyway
            .configure()
            .dataSource(dataSource)
            .load()

        fagsystemIdDao = PostgresFagsystemIdDao(dataSource)
    }

    @BeforeEach
    internal fun setup() {
        flyway.clean()
        flyway.migrate()
    }

    @AfterAll
    internal fun tearDown() {
        postgresConnection.close()
        embeddedPostgres.close()
    }

    @Test
    fun noeFunker(){
        assertFalse(fagsystemIdDao.alleredeHåndtert("TEST"))
        fagsystemIdDao.lagre("TEST")
        assertTrue(fagsystemIdDao.alleredeHåndtert("TEST"))
        assertFalse(fagsystemIdDao.alleredeHåndtert("XYZ"))
    }

}
