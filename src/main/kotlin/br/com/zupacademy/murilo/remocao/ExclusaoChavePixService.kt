package br.com.zupacademy.murilo.remocao

import br.com.zupacademy.murilo.bcb.BcbClient
import br.com.zupacademy.murilo.bcb.DeletePixKeyRequest
import br.com.zupacademy.murilo.chave.ChavePix
import br.com.zupacademy.murilo.chave.ChavePixRepository
import br.com.zupacademy.murilo.exceptions.ChavePixNaoEncontradaException
import io.micronaut.http.HttpStatus
import io.micronaut.validation.Validated
import org.slf4j.LoggerFactory
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.Valid

@Validated
@Singleton
class ExclusaoChavePixService(
    @Inject val repository: ChavePixRepository,
    @Inject val bcbClient: BcbClient
) {
    private val LOGGER = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun remover(@Valid request: ExclusaoChavePix): ChavePix {
        val buscaChave: Optional<ChavePix> = repository.findByIdAndClienteId(
            UUID.fromString(request.pixId),
            UUID.fromString(request.clienteId)
        )

        if (buscaChave.isEmpty) {
            LOGGER.error("Chave Pix não encontrada ou não pertence ao cliente informado")
            throw ChavePixNaoEncontradaException("Chave Pix não encontrada ou não pertence ao cliente informado")
        }

        repository.deleteById(UUID.fromString(request.pixId))

        val bcbRequest = DeletePixKeyRequest(buscaChave.get().chave, buscaChave.get().conta.instituicao)
        val bcbResponse = bcbClient.removeChave(key = buscaChave.get().chave, request = bcbRequest)
        if (bcbResponse.status != HttpStatus.OK) {
            throw IllegalStateException("Erro ao remover Chave Pix no BCB")
        }

        return buscaChave.get()
    }
}
