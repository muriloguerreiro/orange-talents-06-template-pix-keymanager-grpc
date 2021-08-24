package br.com.zupacademy.murilo.registro

import br.com.zupacademy.murilo.chave.ChavePix
import br.com.zupacademy.murilo.chave.ChavePixRepository
import br.com.zupacademy.murilo.exceptions.ChavePixExistenteException
import br.com.zupacademy.murilo.itau.ItauClient
import io.micronaut.validation.Validated
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.Valid

@Validated
@Singleton
class NovaChavePixService(
    @Inject val repository: ChavePixRepository,
    @Inject val itauClient: ItauClient
) {

    private val LOGGER = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun registrar(@Valid novaChave: NovaChavePix): ChavePix {

        if (repository.existsByChave(novaChave.chave))
            throw ChavePixExistenteException("Chave Pix '${novaChave.chave}'existente")

        val response = itauClient.buscaContaPorTipo(novaChave.clienteId!!, novaChave.tipoDeConta!!.name)
        val conta = response.body()?.toContaAssociada() ?: throw IllegalStateException("Cliente não encontrado no Itaú")

        val chave = novaChave.converter(conta)
        repository.save(chave)

        return chave
    }
}
