package no.nav.helse.sparker

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import javax.sql.DataSource

interface FagsystemIdDao {

    fun alleredeHåndtert(id: String): Boolean

    fun lagre(fagsystemId: String)
}


class PostgresFagsystemIdDao(
    private val dataSource: DataSource
) : FagsystemIdDao {

    private val table = "etterbetaling_kandidat"

    override fun alleredeHåndtert(fagsystemId: String) = requireNotNull(using(sessionOf(dataSource)) { session ->
        val query = """
                SELECT EXISTS (
                    SELECT 1
                    FROM $table
                    WHERE fagsystem_id = ?
                )
            """
        session.run(queryOf(query, fagsystemId).map { it.boolean(1) }.asSingle)
    })


    override fun lagre(fagsystemId: String) {
        (using(sessionOf(dataSource)) { session ->
            val query = """
                INSERT INTO $table
                (fagsystem_id)
                values
                ($fagsystemId)
            """
            session.run(queryOf(query).asUpdate)
        })
    }
}

class FagsystemIdDaoMock : FagsystemIdDao {

    val lagredeIder = mutableListOf<String>()

    override fun alleredeHåndtert(id: String): Boolean =
        lagredeIder.any {
            it == id
        }

    override fun lagre(fagsystemId: String) {
        lagredeIder.add(fagsystemId)
    }
}
