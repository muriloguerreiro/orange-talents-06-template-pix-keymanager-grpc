package br.com.zupacademy.murilo.registro

import br.com.zupacademy.murilo.chave.ContaAssociada

data class DadosDaContaResponse(
    val tipo: String,
    val instituicao: InstituicaoResponse,
    val agencia: String,
    val numero: String,
    val titular: TitularResponse
) {

    fun toContaAssociada(): ContaAssociada {

        return ContaAssociada(
            instituicao = this.instituicao.nome,
            nomeDoTitular = this.titular.nome,
            cpfDoTitular = this.titular.cpf,
            agencia = this.agencia,
            numero = this.numero
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DadosDaContaResponse

        if (agencia != other.agencia) return false
        if (numero != other.numero) return false

        return true
    }

    override fun hashCode(): Int {
        var result = agencia.hashCode()
        result = 31 * result + numero.hashCode()
        return result
    }
}

data class InstituicaoResponse(val nome: String, val ispb: String)
data class TitularResponse(val nome: String, val cpf: String)
