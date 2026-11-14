package sus.liva.infrastructure.adapter.out.persistencia;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface PacienteJpaRepository extends JpaRepository<PacienteEntity, UUID> {
    Optional<PacienteEntity> findByCpf(String cpf);
    Optional<PacienteEntity> findByCartaoSus(String cartaoSus);
    List<PacienteEntity> findByTelefone(String telefone);

    @Query("""
            select p from PacienteEntity p
            where (:documento <> '' and (p.cpf = :documento or p.cartaoSus = :documento))
               or (:nome <> '' and lower(p.nomeCompleto) like concat(lower(:nome), '%'))
            order by p.nomeCompleto""")
    List<PacienteEntity> pesquisar(@Param("nome") String nome, @Param("documento") String documento, Limit limite);
}
