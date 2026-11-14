package sus.liva.infrastructure.adapter.out.persistencia;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query(value = """
            select p from PacienteEntity p
            where (:documento <> '' and (p.cpf = :documento or p.cartaoSus = :documento))
               or (:nome <> '' and (lower(p.nomeCompleto) like concat('%', lower(:nome), '%')
                                    or lower(p.nomeSocial) like concat('%', lower(:nome), '%')))
            order by p.nomeCompleto""",
            countQuery = """
            select count(p) from PacienteEntity p
            where (:documento <> '' and (p.cpf = :documento or p.cartaoSus = :documento))
               or (:nome <> '' and (lower(p.nomeCompleto) like concat('%', lower(:nome), '%')
                                    or lower(p.nomeSocial) like concat('%', lower(:nome), '%')))""")
    Page<PacienteEntity> pesquisarPaginado(@Param("nome") String nome, @Param("documento") String documento, Pageable pagina);

    Page<PacienteEntity> findAllByOrderByCriadoEmDesc(Pageable pagina);
}
