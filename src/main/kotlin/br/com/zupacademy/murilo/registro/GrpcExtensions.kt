package br.com.zupacademy.murilo.registro

import br.com.zupacademy.murilo.RegistroChavePixRequest
import br.com.zupacademy.murilo.TipoDeChave.UNKNOWN_TIPO_CHAVE
import br.com.zupacademy.murilo.TipoDeConta.UNKNOWN_TIPO_CONTA
import br.com.zupacademy.murilo.chave.TipoDeChave
import br.com.zupacademy.murilo.chave.TipoDeConta

fun RegistroChavePixRequest.toNovaChavePix() : NovaChavePix {
    return NovaChavePix(
        clienteId = clienteId,
        tipo = when (tipoDeChave) {
            UNKNOWN_TIPO_CHAVE -> null
            else -> TipoDeChave.valueOf(tipoDeChave.name)
        },
        chave = chave,
        tipoDeConta = when (tipoDeConta) {
            UNKNOWN_TIPO_CONTA -> null
            else -> TipoDeConta.valueOf(tipoDeConta.name)
        }
    )
}