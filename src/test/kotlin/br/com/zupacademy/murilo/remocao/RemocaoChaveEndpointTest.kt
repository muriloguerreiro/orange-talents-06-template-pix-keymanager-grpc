package br.com.zupacademy.murilo.remocao

import br.com.zupacademy.murilo.RegistroChavePixRequest
import br.com.zupacademy.murilo.RemocaoChavePixRequest
import br.com.zupacademy.murilo.RemocaoPixKeymanagerGrpcServiceGrpc
import br.com.zupacademy.murilo.bcb.BcbClient
import br.com.zupacademy.murilo.bcb.DeletePixKeyRequest
import br.com.zupacademy.murilo.bcb.DeletePixKeyResponse
import br.com.zupacademy.murilo.chave.*
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.http.HttpResponse
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject

@MicronautTest(transactional = false)
internal class RemocaoChaveEndpointTest(
    val repository: ChavePixRepository,
    val grpcClient: RemocaoPixKeymanagerGrpcServiceGrpc.RemocaoPixKeymanagerGrpcServiceBlockingStub
) {
    val chave = ChavePix(
        clienteId = UUID.fromString("c56dfef4-7901-44fb-84e2-a2cefb157890"),
        tipo = TipoDeChave.EMAIL,
        chave = "rponte@gmail.com",
        tipoDeConta = TipoDeConta.CONTA_CORRENTE,
        conta = ContaAssociada(
            instituicao = "ITAÚ UNIBANCO S.A.",
            nomeDoTitular = "Rafael M Ponte",
            cpfDoTitular = "02467781054",
            agencia = "0001",
            numero = "291900")
    )

    @Inject
    lateinit var bcbClient: BcbClient

    @BeforeEach
    fun setup() {
        repository.deleteAll()
        repository.save(chave)
    }

    @Test
    fun `deve remover chave pix existente`() {
        Mockito.`when`(bcbClient.removeChave(
            key = "rponte@gmail.com",
            request = deletePixKeyRequest("rponte@gmail.com")
        )).thenReturn(HttpResponse.ok(
            deletePixKeyResponse("rponte@gmail.com")))

        val response = grpcClient.remover(
            RemocaoChavePixRequest.newBuilder()
                .setClienteId(chave.clienteId.toString())
                .setPixId(chave.id.toString())
                .build())

        with(response) {
            assertEquals(chave.id,UUID.fromString(pixId))
            assertEquals(chave.clienteId, UUID.fromString(clienteId))
        }
        assertEquals(0, repository.count())
    }

    @Test
    fun `nao deve remover chave pix se CLIENTE_ID não existe`(){
        val exception = assertThrows<StatusRuntimeException> {
            grpcClient.remover(
                RemocaoChavePixRequest.newBuilder()
                    .setClienteId("00000000-7901-44fb-84e2-a2cefb157890")
                    .setPixId(chave.id.toString())
                    .build())
        }

        with(exception) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Chave Pix não encontrada ou não pertence ao cliente informado", status.description)
            assertTrue(repository.existsById(chave.id))
        }
    }

    @Test
    fun `nao deve remover chave pix se PIX_ID não existe`() {
        val exception = assertThrows<StatusRuntimeException> {
            grpcClient.remover(
                RemocaoChavePixRequest.newBuilder()
                    .setClienteId("c56dfef4-7901-44fb-84e2-a2cefb157890")
                    .setPixId("00000000-0000-0000-0000-000000000000")
                    .build())
        }

        with(exception) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Chave Pix não encontrada ou não pertence ao cliente informado", status.description)
            assertTrue(repository.existsById(chave.id))
        }
    }

    @Test
    fun `nao deve remover chave pix se não pertence ao CLIENTE_ID informado`(){
        val exception = assertThrows<StatusRuntimeException> {
            grpcClient.remover(
                RemocaoChavePixRequest.newBuilder()
                    .setClienteId("5260263c-a3c1-4727-ae32-3bdb2538841b")
                    .setPixId(chave.id.toString())
                    .build())
        }

        with(exception) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Chave Pix não encontrada ou não pertence ao cliente informado", status.description)
            assertTrue(repository.existsById(chave.id))
        }
    }

    @Test
    fun `nao deve remover chave pix se remocao no BCB falhar`() {
        Mockito.`when`(bcbClient.removeChave(
            key = "rponte@gmail.com",
            request = deletePixKeyRequest("rponte@gmail.com")
        )).thenReturn(HttpResponse.badRequest())

        val exception = assertThrows<StatusRuntimeException> {
            grpcClient.remover(
                RemocaoChavePixRequest.newBuilder()
                    .setClienteId(chave.clienteId.toString())
                    .setPixId(chave.id.toString())
                    .build())
        }

        with(exception) {
            assertEquals(Status.FAILED_PRECONDITION.code, status.code)
            assertEquals("Erro ao remover Chave Pix no BCB", status.description)
        }
    }

    @MockBean(BcbClient::class)
    fun bcbClient(): BcbClient? {
        return Mockito.mock(BcbClient::class.java)
    }

    @Factory
    class Clients {
        @Bean
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): RemocaoPixKeymanagerGrpcServiceGrpc.RemocaoPixKeymanagerGrpcServiceBlockingStub {
            return RemocaoPixKeymanagerGrpcServiceGrpc.newBlockingStub(channel)
        }
    }

    private fun deletePixKeyRequest(key: String): DeletePixKeyRequest {

        return DeletePixKeyRequest(
            key = key,
            participant = "60701190"
        )
    }

    private fun deletePixKeyResponse(key: String): DeletePixKeyResponse {

        return DeletePixKeyResponse(
            key = key,
            participant = "60701190",
            deletedAt = LocalDateTime.now()
        )
    }
}