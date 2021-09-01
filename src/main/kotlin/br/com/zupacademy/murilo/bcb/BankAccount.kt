package br.com.zupacademy.murilo.bcb

import br.com.zupacademy.murilo.chave.TipoDeConta

data class BankAccount(
    val participant: String,
    val branch: String,
    val accountNumber: String,
    val accountType: AccountType
) {
    enum class AccountType() {
        CACC, SVGS;

        companion object {
            fun fromTipoDeConta(tipo: TipoDeConta): AccountType {
                return when (tipo) {
                    TipoDeConta.CONTA_CORRENTE -> CACC
                    TipoDeConta.CONTA_POUPANCA -> SVGS
                }
            }
        }
    }
}