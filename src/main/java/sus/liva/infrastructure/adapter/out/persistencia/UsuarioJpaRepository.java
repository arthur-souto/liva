package sus.liva.infrastructure.adapter.out.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, UUID> {
    @Query("select u from UsuarioEntity u where lower(u.login) = lower(:login)")
    Optional<UsuarioEntity> buscarPorLogin(@Param("login") String login);

    Optional<UsuarioEntity> findByPacienteId(UUID pacienteId);

    @Modifying
    @Query(value = """
            update usuarios
               set tentativas_falhas = case when tentativas_falhas + 1 >= :limite then 0 else tentativas_falhas + 1 end,
                   bloqueado_ate = case when tentativas_falhas + 1 >= :limite then :ate else bloqueado_ate end
             where id = :id""", nativeQuery = true)
    void registrarFalha(@Param("id") UUID id, @Param("limite") int limite, @Param("ate") Instant ate);

    @Modifying
    @Query(value = "update usuarios set tentativas_falhas = 0, bloqueado_ate = null where id = :id", nativeQuery = true)
    void registrarAcesso(@Param("id") UUID id);
}
