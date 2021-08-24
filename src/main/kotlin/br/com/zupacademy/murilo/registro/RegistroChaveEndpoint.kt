package br.com.zupacademy.murilo.registro

import br.com.zupacademy.murilo.RegistroChavePixRequest
import br.com.zupacademy.murilo.RegistroChavePixResponse
import br.com.zupacademy.murilo.RegistroPixKeymanagerGrpcServiceGrpc
import br.com.zupacademy.murilo.exceptions.ErrorHandler
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import javax.inject.Singleton

@ErrorHandler
@Singleton
class RegistroChaveEndpoint(@Inject private val service: NovaChavePixService)
    : RegistroPixKeymanagerGrpcServiceGrpc.RegistroPixKeymanagerGrpcServiceImplBase() {

    override fun registrar(
        request: RegistroChavePixRequest,
        responseObserver: StreamObserver<RegistroChavePixResponse>
    ) {

        val novaChave = request.toNovaChavePix()
        val chaveCriada = service.registrar(novaChave)

        responseObserver.onNext(RegistroChavePixResponse.newBuilder()
            .setPixId(chaveCriada.id.toString())
            .build())
        responseObserver.onCompleted()
    }

}