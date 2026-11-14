package sus.liva.infrastructure.adapter.out.persistencia;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import sus.liva.application.port.out.CodigoDoCuidador;
import sus.liva.application.port.out.CredencialDoCuidador;
import sus.liva.application.port.out.CuidadorRepositoryPort;
import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.cuidador.Cuidador;
import sus.liva.domain.model.cuidador.CuidadorVinculado;
import sus.liva.domain.model.cuidador.Parentesco;
import sus.liva.domain.model.cuidador.VinculoDeCuidado;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CuidadorPostgresAdapter implements CuidadorRepositoryPort {
    private static final RowMapper<Cuidador> CUIDADOR = (rs, i) -> new Cuidador(rs.getObject("id", UUID.class), rs.getString("nome"),
            rs.getString("cpf"), new Telefone(rs.getString("telefone")));

    private final JdbcClient jdbc;

    CuidadorPostgresAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Cuidador> buscarCuidador(UUID id) {
        return jdbc.sql("select * from cuidadores where id = :id").param("id", id).query(CUIDADOR).optional();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Cuidador> buscarCuidadorPorCpf(String cpf) {
        return jdbc.sql("select * from cuidadores where cpf = :cpf").param("cpf", cpf).query(CUIDADOR).optional();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Cuidador> buscarCuidadoresPorTelefone(Telefone telefone) {
        return jdbc.sql("select * from cuidadores where telefone = :telefone").param("telefone", telefone.e164()).query(CUIDADOR).list();
    }

    @Override
    @Transactional
    public void salvarCuidador(Cuidador c, Instant em) {
        jdbc.sql("""
                insert into cuidadores (id, nome, cpf, telefone, criado_em, atualizado_em)
                values (:id, :nome, :cpf, :telefone, :em, :em)
                on conflict (id) do update set nome = excluded.nome, telefone = excluded.telefone, atualizado_em = excluded.atualizado_em""")
                .param("id", c.id())
                .param("nome", c.nome())
                .param("cpf", c.cpf())
                .param("telefone", c.telefone().e164())
                .param("em", Timestamp.from(em))
                .update();
    }

    @Override
    @Transactional
    public void vincular(VinculoDeCuidado v) {
        jdbc.sql("""
                insert into vinculos_de_cuidado (cuidador_id, paciente_id, parentesco, ativo, autorizado_por, autorizado_em, revogado_em)
                values (:cuidador, :paciente, :parentesco, true, :por, :em, null)
                on conflict (cuidador_id, paciente_id) do update
                   set parentesco = excluded.parentesco, ativo = true, autorizado_por = excluded.autorizado_por,
                       autorizado_em = excluded.autorizado_em, revogado_em = null""")
                .param("cuidador", v.cuidadorId())
                .param("paciente", v.pacienteId())
                .param("parentesco", v.parentesco().name())
                .param("por", v.autorizadoPor())
                .param("em", Timestamp.from(v.autorizadoEm()))
                .update();
    }

    @Override
    @Transactional
    public void revogarVinculo(UUID cuidadorId, UUID pacienteId, Instant em) {
        jdbc.sql("update vinculos_de_cuidado set ativo = false, revogado_em = :em where cuidador_id = :cuidador and paciente_id = :paciente")
                .param("em", Timestamp.from(em))
                .param("cuidador", cuidadorId)
                .param("paciente", pacienteId)
                .update();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuidadorVinculado> cuidadoresDoPaciente(UUID pacienteId) {
        return jdbc.sql("""
                select c.*, v.parentesco, v.autorizado_em from vinculos_de_cuidado v join cuidadores c on c.id = v.cuidador_id
                where v.paciente_id = :paciente and v.ativo order by v.autorizado_em""")
                .param("paciente", pacienteId)
                .query((rs, i) -> new CuidadorVinculado(CUIDADOR.mapRow(rs, i), Parentesco.valueOf(rs.getString("parentesco")),
                        rs.getTimestamp("autorizado_em").toInstant()))
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> dependentesDe(UUID cuidadorId) {
        return jdbc.sql("select paciente_id from vinculos_de_cuidado where cuidador_id = :cuidador and ativo order by autorizado_em")
                .param("cuidador", cuidadorId)
                .query(UUID.class)
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean cuidaDe(UUID cuidadorId, UUID pacienteId) {
        return jdbc.sql("select count(*) from vinculos_de_cuidado where cuidador_id = :cuidador and paciente_id = :paciente and ativo")
                .param("cuidador", cuidadorId)
                .param("paciente", pacienteId)
                .query(Integer.class)
                .single() > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CredencialDoCuidador> credencial(UUID cuidadorId) {
        return jdbc.sql("select senha_hash, tentativas_falhas, bloqueado_ate from cuidadores where id = :id and senha_hash is not null")
                .param("id", cuidadorId)
                .query((rs, i) -> new CredencialDoCuidador(rs.getString("senha_hash"), rs.getInt("tentativas_falhas"),
                        instante(rs.getTimestamp("bloqueado_ate"))))
                .optional();
    }

    @Override
    @Transactional
    public void definirSenha(UUID cuidadorId, String senhaHash) {
        jdbc.sql("update cuidadores set senha_hash = :hash, tentativas_falhas = 0, bloqueado_ate = null where id = :id")
                .param("hash", senhaHash)
                .param("id", cuidadorId)
                .update();
    }

    @Override
    @Transactional
    public void registrarFalha(UUID cuidadorId, int limite, Instant bloquearAte) {
        jdbc.sql("""
                update cuidadores set tentativas_falhas = tentativas_falhas + 1,
                       bloqueado_ate = case when tentativas_falhas + 1 >= :limite then :ate else bloqueado_ate end
                where id = :id""")
                .param("limite", limite)
                .param("ate", Timestamp.from(bloquearAte))
                .param("id", cuidadorId)
                .update();
    }

    @Override
    @Transactional
    public void registrarAcesso(UUID cuidadorId) {
        jdbc.sql("update cuidadores set tentativas_falhas = 0, bloqueado_ate = null where id = :id").param("id", cuidadorId).update();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<CodigoDoCuidador> codigoParaAtualizar(UUID cuidadorId) {
        return jdbc.sql("""
                select codigo_hash, codigo_expira_em, codigo_enviado_em, codigo_tentativas from cuidadores
                where id = :id and codigo_hash is not null for update""")
                .param("id", cuidadorId)
                .query((rs, i) -> new CodigoDoCuidador(rs.getString("codigo_hash"), rs.getTimestamp("codigo_expira_em").toInstant(),
                        rs.getTimestamp("codigo_enviado_em").toInstant(), rs.getInt("codigo_tentativas")))
                .optional();
    }

    @Override
    @Transactional
    public void salvarCodigo(UUID cuidadorId, CodigoDoCuidador c) {
        jdbc.sql("""
                update cuidadores set codigo_hash = :hash, codigo_expira_em = :expira, codigo_enviado_em = :enviado,
                       codigo_tentativas = :tentativas where id = :id""")
                .param("hash", c.codigoHash())
                .param("expira", Timestamp.from(c.expiraEm()))
                .param("enviado", Timestamp.from(c.enviadoEm()))
                .param("tentativas", c.tentativas())
                .param("id", cuidadorId)
                .update();
    }

    @Override
    @Transactional
    public void removerCodigo(UUID cuidadorId) {
        jdbc.sql("""
                update cuidadores set codigo_hash = null, codigo_expira_em = null, codigo_enviado_em = null, codigo_tentativas = 0
                where id = :id""")
                .param("id", cuidadorId)
                .update();
    }

    private static Instant instante(Timestamp t) {
        return t == null ? null : t.toInstant();
    }
}
