package br.com.zupacademy.murilo.registro

import br.com.zupacademy.murilo.bcb.BcbClient
import br.com.zupacademy.murilo.bcb.CreatePixKeyRequest
import br.com.zupacademy.murilo.chave.ChavePix
import br.com.zupacademy.murilo.chave.ChavePixRepository
import br.com.zupacademy.murilo.exceptions.ChavePixExistenteException
import br.com.zupacademy.murilo.itau.ItauClient
import io.micronaut.http.HttpStatus
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
    @Inject val itauClient: ItauClient,
    @Inject val bcbClient: BcbClient
) {

    private val LOGGER = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun registrar(@Valid novaChave: NovaChavePix): ChavePix {

        if (repository.existsByChave(novaChave.chave))
            throw ChavePixExistenteException("Chave Pix '${novaChave.chave}'existente")

        val response = itauClient.buscaContaPorTipo(novaChave.clienteId!!, novaChave.tipoDeConta!!.name)
        val conta = response.body()?.toContaAssociada() ?: throw IllegalStateException("Cliente não encontrado no Itaú")

        val chave = novaChave.converter(conta)

        val bcbRequest = CreatePixKeyRequest.fromChavePix(chave)
        val bcbResponse = bcbClient.registraChave(bcbRequest)
        if (bcbResponse.status != HttpStatus.CREATED)
            throw IllegalStateException("Erro ao registrar Chave Pix no BCB")

        chave.atualiza(bcbResponse.body()!!.key)
        repository.save(chave)

        return chave
    }
}
