package no.nav.helse.sparker

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.testcontainers.containers.PostgreSQLContainer
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PostgresFagsystemIdDaoTest {

    private val postgres = PostgreSQLContainer<Nothing>("postgres:13")
    private lateinit var dataSource: DataSource
    private lateinit var flyway: Flyway
    private lateinit var fagsystemIdDao: PostgresFagsystemIdDao

    @BeforeAll
    internal fun setupAll() {
        postgres.start()

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
        postgres.stop()
    }

    @Test
    fun noeFunker(){
        assertFalse(fagsystemIdDao.alleredeHåndtert("TEST"))
        fagsystemIdDao.lagre("TEST")
        assertTrue(fagsystemIdDao.alleredeHåndtert("TEST"))
        assertFalse(fagsystemIdDao.alleredeHåndtert("XYZ"))
    }

}
