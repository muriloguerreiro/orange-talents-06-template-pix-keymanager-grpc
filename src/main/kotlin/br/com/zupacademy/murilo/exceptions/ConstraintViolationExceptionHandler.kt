package br.com.zupacademy.murilo.exceptions

import io.grpc.Status
import javax.inject.Singleton
import javax.validation.ConstraintViolationException

@Singleton
class ConstraintViolationExceptionHandler : ExceptionHandler<ConstraintViolationException>{

    override fun handle(e: ConstraintViolationException): ExceptionHandler.StatusWithDetails {
        return ExceptionHandler.StatusWithDetails(
            Status.INVALID_ARGUMENT
                .withDescription("Chave Pix contém um ou mais dados inválidos")
                .withCause(e)
        )
    }

    override fun supports(e: Exception): Boolean {
        return e is ConstraintViolationException
    }
}