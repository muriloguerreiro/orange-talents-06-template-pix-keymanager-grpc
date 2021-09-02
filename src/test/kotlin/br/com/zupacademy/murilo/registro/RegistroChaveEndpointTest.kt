package br.com.zupacademy.murilo.registro

import br.com.zupacademy.murilo.RegistroChavePixRequest
import br.com.zupacademy.murilo.RegistroPixKeymanagerGrpcServiceGrpc
import br.com.zupacademy.murilo.bcb.*
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject
import br.com.zupacademy.murilo.TipoDeChave as TipoDeChaveStub
import br.com.zupacademy.murilo.TipoDeConta as TipoDeContaStub

@MicronautTest(transactional = false)
internal class RegistroChaveEndpointTest(
    val repository: ChavePixRepository,
    val grpcClient: RegistroPixKeymanagerGrpcServiceGrpc.RegistroPixKeymanagerGrpcServiceBlockingStub
) {

    @Inject
    lateinit var itauClient: ItauClient

    @Inject
    lateinit var bcbClient: BcbClient

    companion object {
        val randomId = UUID.randomUUID()
    }

    @BeforeEach
    fun setup() {
        repository.deleteAll()
    }

    @Test
    fun `deve registrar nova chave pix`() {
        Mockito.`when`(itauClient.buscaContaPorTipo(clienteId = "c56dfef4-7901-44fb-84e2-a2cefb157890", tipo = "CONTA_CORRENTE"))
            .thenReturn(HttpResponse.ok(dadosDaContaResponse()))

        Mockito.`when`(bcbClient.registraChave(createPixKeyRequest(
            keyType = TipoDeChaveStub.EMAIL.toString(),
            key = "alefh@gmail.com"
        ))).thenReturn(HttpResponse.created(createPixKeyResponse(
            keyType = TipoDeChaveStub.EMAIL.toString(),
            key = "alefh@gmail.com"
        )))

        val response = grpcClient.registrar(RegistroChavePixRequest.newBuilder()
            .setClienteId("c56dfef4-7901-44fb-84e2-a2cefb157890")
            .setTipoDeChave(TipoDeChaveStub.EMAIL)
            .setChave("alefh@gmail.com")
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

    @Test
    fun `nao deve registrar chave pix se registro no BCB falhar`() {
        Mockito.`when`(itauClient.buscaContaPorTipo(clienteId = "c56dfef4-7901-44fb-84e2-a2cefb157890", tipo = "CONTA_CORRENTE"))
            .thenReturn(HttpResponse.ok(dadosDaContaResponse()))

        Mockito.`when`(bcbClient.registraChave(createPixKeyRequest(
            keyType = TipoDeChaveStub.EMAIL.toString(),
            key = "alefh@gmail.com"
        ))).thenReturn(HttpResponse.badRequest())

        val exception = assertThrows<StatusRuntimeException> {
            grpcClient.registrar(RegistroChavePixRequest.newBuilder()
                .setClienteId("c56dfef4-7901-44fb-84e2-a2cefb157890")
                .setTipoDeChave(TipoDeChaveStub.EMAIL)
                .setChave("alefh@gmail.com")
                .setTipoDeConta(TipoDeContaStub.CONTA_CORRENTE)
                .build())
        }

        with(exception) {
            assertEquals(Status.FAILED_PRECONDITION.code, status.code)
            assertEquals("Erro ao registrar Chave Pix no BCB", status.description)
        }
    }

    @MockBean(ItauClient::class)
    fun itauClient(): ItauClient? {
        return Mockito.mock(ItauClient::class.java)
    }

    @MockBean(BcbClient::class)
    fun bcbClient(): BcbClient? {
        return Mockito.mock(BcbClient::class.java)
    }

    @Factory
    class Clients {
        @Bean
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): RegistroPixKeymanagerGrpcServiceGrpc.RegistroPixKeymanagerGrpcServiceBlockingStub {
            return RegistroPixKeymanagerGrpcServiceGrpc.newBlockingStub(channel)
        }
    }

    private fun dadosDaContaResponse(): DadosDaContaResponse {

        return DadosDaContaResponse(
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
        )
    }

    private fun createPixKeyRequest(keyType: String, key: String): CreatePixKeyRequest {

        return CreatePixKeyRequest(
            keyType = keyType,
            key = key,
            bankAccount = BankAccount(
                participant = "60701190",
                branch = "0001",
                accountType = BankAccount.AccountType.CACC,
                accountNumber = "483201"
            ),
            owner = Owner(
                type = OwnerType.NATURAL_PERSON,
                name = "Alefh Silva",
                taxIdNumber = "83082363083"
            )
        )
    }

    private fun createPixKeyResponse(keyType: String, key: String): CreatePixKeyResponse {

        return CreatePixKeyResponse(
            keyType = keyType,
            key = key,
            bankAccount = BankAccount(
                participant = "60701190",
                branch = "0001",
                accountType = BankAccount.AccountType.CACC,
                accountNumber = "483201"
            ),
            owner = Owner(
                type = OwnerType.NATURAL_PERSON,
                name = "Alefh Silva",
                taxIdNumber = "83082363083"
            ),
            createdAt = LocalDateTime.now()
        )
    }
}