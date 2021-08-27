package br.com.zupacademy.murilo.remocao

import br.com.zupacademy.murilo.*
import br.com.zupacademy.murilo.chave.ChavePix
import br.com.zupacademy.murilo.exceptions.ErrorHandler
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import javax.inject.Singleton

@ErrorHandler
@Singleton
class RemocaoChaveEndpoint(@Inject private val service: ExclusaoChavePixService)
    : RemocaoPixKeymanagerGrpcServiceGrpc.RemocaoPixKeymanagerGrpcServiceImplBase(){

    override fun remover(
        request: RemocaoChavePixRequest,
        responseObserver: StreamObserver<RemocaoChavePixResponse>
    ) {
        val exclusaoChave = ExclusaoChavePix(request.clienteId,request.pixId)
        val chaveExcluida: ChavePix = service.remover(exclusaoChave)

        responseObserver.onNext(
            RemocaoChavePixResponse.newBuilder()
                .setClienteId(chaveExcluida.clienteId.toString())
                .setPixId(chaveExcluida.id.toString())
                .build())
        responseObserver.onCompleted()
    }

}