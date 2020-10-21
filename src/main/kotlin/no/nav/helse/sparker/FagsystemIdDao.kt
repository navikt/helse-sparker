package no.nav.helse.sparker

interface FagsystemIdDao {

    fun alleredeHåndtert(id: String): Boolean

    fun lagre(fagsystemId: String)
}


class PostgresFagsystemIdDao : FagsystemIdDao {
    override fun alleredeHåndtert(id: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun lagre(fagsystemId: String) {
        TODO("Not yet implemented")
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
