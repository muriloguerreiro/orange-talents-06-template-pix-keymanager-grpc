package br.com.zupacademy.murilo.bcb

import br.com.zupacademy.murilo.chave.ChavePix
import br.com.zupacademy.murilo.chave.TipoDeChave

data class CreatePixKeyRequest(
    val keyType: String,
    val key: String,
    val bankAccount: BankAccount,
    val owner: Owner
){
    companion object {
        fun fromChavePix(chave: ChavePix): CreatePixKeyRequest {
            return CreatePixKeyRequest(
                keyType = when(chave.tipo) {
                    TipoDeChave.CPF -> KeyType.CPF.toString()
                    TipoDeChave.EMAIL -> KeyType.EMAIL.toString()
                    TipoDeChave.CELULAR -> KeyType.PHONE.toString()
                    TipoDeChave.ALEATORIA -> KeyType.RANDOM.toString()
                                           } ,
                key = chave.chave,
                bankAccount = BankAccount(
                    participant = chave.conta.instituicao,
                    branch = chave.conta.agencia,
                    accountNumber = chave.conta.numero,
                    accountType = BankAccount.AccountType.fromTipoDeConta(chave.tipoDeConta)
                ),
                owner = Owner(
                    type = OwnerType.NATURAL_PERSON,
                    name = chave.conta.nomeDoTitular,
                    taxIdNumber = chave.conta.cpfDoTitular
                )
            )
        }
    }
}