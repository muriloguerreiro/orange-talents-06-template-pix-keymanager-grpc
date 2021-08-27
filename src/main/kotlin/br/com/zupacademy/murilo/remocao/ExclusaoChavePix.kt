package br.com.zupacademy.murilo.remocao

import br.com.zupacademy.murilo.validacao.ValidUUID
import io.micronaut.core.annotation.Introspected
import javax.validation.constraints.NotBlank

@Introspected
data class ExclusaoChavePix(
    @ValidUUID
    @field:NotBlank
    val clienteId: String?,

    @ValidUUID
    @field:NotBlank
    val pixId: String?,
)