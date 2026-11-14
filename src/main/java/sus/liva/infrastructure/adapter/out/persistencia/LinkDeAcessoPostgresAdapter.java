package sus.liva.infrastructure.adapter.out.persistencia;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import sus.liva.application.port.out.LinkDeAcesso;
import sus.liva.application.port.out.LinkDeAcessoPort;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class LinkDeAcessoPostgresAdapter implements LinkDeAcessoPort {
    private final JdbcClient jdbc;

    LinkDeAcessoPostgresAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void salvarLink(LinkDeAcesso link) {
        jdbc.sql("""
                insert into links_de_acesso (token_hash, paciente_id, destino, criado_em, expira_em, usado_em)
                values (:hash, :paciente, :destino, :criado, :expira, null)""")
                .param("hash", link.tokenHash())
                .param("paciente", link.pacienteId())
                .param("destino", link.destino())
                .param("criado", Timestamp.from(link.criadoEm()))
                .param("expira", Timestamp.from(link.expiraEm()))
                .update();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<LinkDeAcesso> buscarParaUsar(String tokenHash) {
        return jdbc.sql("select * from links_de_acesso where token_hash = :hash for update")
                .param("hash", tokenHash)
                .query((rs, i) -> new LinkDeAcesso(rs.getString("token_hash"), rs.getObject("paciente_id", UUID.class),
                        rs.getString("destino"), rs.getTimestamp("criado_em").toInstant(), rs.getTimestamp("expira_em").toInstant(),
                        rs.getTimestamp("usado_em") == null ? null : rs.getTimestamp("usado_em").toInstant()))
                .optional();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void marcarUsado(String tokenHash, Instant em) {
        jdbc.sql("update links_de_acesso set usado_em = :em where token_hash = :hash")
                .param("em", Timestamp.from(em))
                .param("hash", tokenHash)
                .update();
    }
}
