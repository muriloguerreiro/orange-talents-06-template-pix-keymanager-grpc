package br.com.zupacademy.murilo.remocao

import br.com.zupacademy.murilo.RemocaoChavePixRequest
import br.com.zupacademy.murilo.RemocaoPixKeymanagerGrpcServiceGrpc
import br.com.zupacademy.murilo.chave.*
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*

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

    @BeforeEach
    fun setup() {
        repository.deleteAll()
        repository.save(chave)
    }

    @Test
    fun `deve remover chave pix existente`() {
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

    @Factory
    class Clients {
        @Bean
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): RemocaoPixKeymanagerGrpcServiceGrpc.RemocaoPixKeymanagerGrpcServiceBlockingStub {
            return RemocaoPixKeymanagerGrpcServiceGrpc.newBlockingStub(channel)
        }
    }
}