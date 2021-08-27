package br.com.zupacademy.murilo.registro

import br.com.zupacademy.murilo.RegistroChavePixRequest
import br.com.zupacademy.murilo.RegistroPixKeymanagerGrpcServiceGrpc
import br.com.zupacademy.murilo.TipoDeChave as TipoDeChaveStub
import br.com.zupacademy.murilo.TipoDeConta as TipoDeContaStub
import br.com.zupacademy.murilo.chave.ChavePix
import br.com.zupacademy.murilo.chave.ChavePixRepository
import br.com.zupacademy.murilo.chave.ContaAssociada
import br.com.zupacademy.murilo.chave.*
import br.com.zupacademy.murilo.itau.ItauClient
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
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.awt.image.TileObserver
import java.util.*
import javax.inject.Inject

@MicronautTest(transactional = false)
internal class RegistroChaveEndpointTest(
    val repository: ChavePixRepository,
    val grpcClient: RegistroPixKeymanagerGrpcServiceGrpc.RegistroPixKeymanagerGrpcServiceBlockingStub
) {

    @Inject
    lateinit var itauClient: ItauClient;

    companion object {
        val randomId = UUID.randomUUID()
    }

    @BeforeEach
    fun setup() {
        repository.deleteAll()
    }

    @Test
    fun `deve registrar nova chave pix`() {
        Mockito.`when`(itauClient.buscaContaPorTipo(clienteId = randomId.toString(), tipo = "CONTA_CORRENTE"))
            .thenReturn(HttpResponse.ok(
                DadosDaContaResponse(
                    tipo = "CONTA_CORRENTE",
                    instituicao = InstituicaoResponse(
                        nome = "ITAÚ UNIBANCO S.A.",
                        ispb = "60701190"
                    ),
                    agencia = "0001",
                    numero = "483201",
                    titular = TitularResponse(
                        nome = "Alefh Silva",
                        cpf = "83082363083"
                    )
                )))

        val response = grpcClient.registrar(RegistroChavePixRequest.newBuilder()
            .setClienteId(randomId.toString())
            .setTipoDeChave(TipoDeChaveStub.EMAIL)
            .setChave("rponte@gmail.com")
            .setTipoDeConta(TipoDeContaStub.CONTA_CORRENTE)
            .build())

        with(response) {
            assertNotNull(pixId)
        }
    }

    @Test
    fun `nao deve registrar chave pix se CHAVE já existente`() {
        repository.save(
            ChavePix(
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
        )

        val exception = assertThrows<StatusRuntimeException> {
            grpcClient.registrar(RegistroChavePixRequest.newBuilder()
                .setClienteId("c56dfef4-7901-44fb-84e2-a2cefb157890")
                .setTipoDeChave(TipoDeChaveStub.EMAIL)
                .setChave("rponte@gmail.com")
                .setTipoDeConta(TipoDeContaStub.CONTA_CORRENTE)
                .build())
        }

        with(exception) {
            assertEquals(Status.ALREADY_EXISTS.code, status.code)
            assertEquals("Chave Pix 'rponte@gmail.com'existente", status.description)
        }
    }

    @Test
    fun `nao deve registrar chave pix se CONTA não encontrada`() {
        Mockito.`when`(itauClient.buscaContaPorTipo(clienteId = "c56dfef4-7901-44fb-84e2-a2cefb157898", tipo = "CONTA_CORRENTE"))
            .thenReturn(HttpResponse.notFound())

        val exception = assertThrows<StatusRuntimeException> {
            grpcClient.registrar(RegistroChavePixRequest.newBuilder()
                .setClienteId("c56dfef4-7901-44fb-84e2-a2cefb157898")
                .setTipoDeChave(TipoDeChaveStub.EMAIL)
                .setChave("rponte@gmail.com")
                .setTipoDeConta(TipoDeContaStub.CONTA_CORRENTE)
                .build())
        }

        with(exception) {
            assertEquals(Status.FAILED_PRECONDITION.code, status.code)
            assertEquals("Cliente não encontrado no Itaú", status.description)
        }
    }

    @Test
    fun `nao deve registrar chave pix se CLIENTE ID inválido`() {
        val exception = assertThrows<StatusRuntimeException> {
            grpcClient.registrar(RegistroChavePixRequest.newBuilder()
                .setTipoDeChave(TipoDeChaveStub.EMAIL)
                .setChave("rponte@gmail.com")
                .setTipoDeConta(TipoDeContaStub.CONTA_CORRENTE)
                .build())
        }

        with(exception) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Chave Pix contém um ou mais dados inválidos", status.description)
        }
    }

    @Test
    fun `nao deve registrar chave pix se TIPO DE CHAVE inválido`() {
        val exception = assertThrows<StatusRuntimeException> {
            grpcClient.registrar(RegistroChavePixRequest.newBuilder()
                .setClienteId("c56dfef4-7901-44fb-84e2-a2cefb157890")
                .setChave("rponte@gmail.com")
                .setTipoDeConta(TipoDeContaStub.CONTA_CORRENTE)
                .build())
        }

        with(exception) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Chave Pix contém um ou mais dados inválidos", status.description)
        }
    }

    @Test
    fun `nao deve registrar chave pix se chave CPF inválida`() {
        val exception = assertThrows<StatusRuntimeException> {
            grpcClient.registrar(RegistroChavePixRequest.newBuilder()
                .setClienteId("c56dfef4-7901-44fb-84e2-a2cefb157890")
                .setTipoDeChave(TipoDeChaveStub.CPF)
                .setChave("inválido")
                .setTipoDeConta(TipoDeContaStub.CONTA_CORRENTE)
                .build())
        }

        with(exception) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Chave Pix contém um ou mais dados inválidos", status.description)
        }
    }

    @Test
    fun `nao deve registrar chave pix se chave EMAIL inválida`() {
        val exception = assertThrows<StatusRuntimeException> {
            grpcClient.registrar(RegistroChavePixRequest.newBuilder()
                .setClienteId("c56dfef4-7901-44fb-84e2-a2cefb157890")
                .setTipoDeChave(TipoDeChaveStub.EMAIL)
                .setChave("inválido")
                .setTipoDeConta(TipoDeContaStub.CONTA_CORRENTE)
                .build())
        }

        with(exception) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Chave Pix contém um ou mais dados inválidos", status.description)
        }
    }

    @Test
    fun `nao deve registrar chave pix se chave CELULAR inválida`() {
        val exception = assertThrows<StatusRuntimeException> {
            grpcClient.registrar(RegistroChavePixRequest.newBuilder()
                .setClienteId("c56dfef4-7901-44fb-84e2-a2cefb157890")
                .setTipoDeChave(TipoDeChaveStub.CELULAR)
                .setChave("inválido")
                .setTipoDeConta(TipoDeContaStub.CONTA_CORRENTE)
                .build())
        }

        with(exception) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Chave Pix contém um ou mais dados inválidos", status.description)
        }
    }

    @Test
    fun `nao deve registrar chave pix se chave ALEATORIA inválida`() {
        val exception = assertThrows<StatusRuntimeException> {
            grpcClient.registrar(RegistroChavePixRequest.newBuilder()
                .setClienteId("c56dfef4-7901-44fb-84e2-a2cefb157890")
                .setTipoDeChave(TipoDeChaveStub.ALEATORIA)
                .setChave("inválido")
                .setTipoDeConta(TipoDeContaStub.CONTA_CORRENTE)
                .build())
        }

        with(exception) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Chave Pix contém um ou mais dados inválidos", status.description)
        }
    }

    @Test
    fun `nao deve registrar chave pix se TIPO DE CONTA inválido`() {
        val exception = assertThrows<StatusRuntimeException> {
            grpcClient.registrar(RegistroChavePixRequest.newBuilder()
                .setClienteId("c56dfef4-7901-44fb-84e2-a2cefb157890")
                .setTipoDeChave(TipoDeChaveStub.EMAIL)
                .setChave("rponte@gmail.com")
                .build())
        }

        with(exception) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Chave Pix contém um ou mais dados inválidos", status.description)
        }
    }


    @MockBean(ItauClient::class)
    fun itauClient(): ItauClient? {
        return Mockito.mock(ItauClient::class.java)
    }

    @Factory
    class Clients {
        @Bean
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): RegistroPixKeymanagerGrpcServiceGrpc.RegistroPixKeymanagerGrpcServiceBlockingStub {
            return RegistroPixKeymanagerGrpcServiceGrpc.newBlockingStub(channel)
        }
    }
}