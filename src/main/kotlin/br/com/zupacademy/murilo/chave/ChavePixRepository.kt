package br.com.zupacademy.murilo.chave

import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.*

@Repository
interface ChavePixRepository : JpaRepository<ChavePix, UUID> {
    fun existsByChave(chave: String?): Boolean
    fun findByIdAndClienteId(pixId: UUID?, clienteId: UUID?): Optional<ChavePix>
}