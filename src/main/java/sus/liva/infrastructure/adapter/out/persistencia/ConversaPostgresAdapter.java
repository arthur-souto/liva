package sus.liva.infrastructure.adapter.out.persistencia;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sus.liva.application.port.out.Conversa;
import sus.liva.application.port.out.ConversaPort;
import sus.liva.application.port.out.EtapaDaConversa;
import sus.liva.domain.model.Telefone;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ConversaPostgresAdapter implements ConversaPort {
    private static final String SEPARADOR = ";";

    private final JdbcClient jdbc;

    ConversaPostgresAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Conversa> buscarConversa(Telefone telefone) {
        return jdbc.sql("select * from conversas_whatsapp where chave_telefone = :chave")
                .param("chave", telefone.chaveComparacao())
                .query((rs, i) -> new Conversa(Telefone.de(rs.getString("telefone")), rs.getObject("paciente_id", UUID.class),
                        EtapaDaConversa.valueOf(rs.getString("etapa")), opcoes(rs.getString("opcoes")),
                        rs.getTimestamp("atualizada_em").toInstant()))
                .optional();
    }

    @Override
    @Transactional
    public void salvarConversa(Conversa c) {
        jdbc.sql("""
                insert into conversas_whatsapp (chave_telefone, telefone, paciente_id, etapa, opcoes, atualizada_em)
                values (:chave, :telefone, :paciente, :etapa, :opcoes, :em)
                on conflict (chave_telefone) do update
                   set telefone = excluded.telefone, paciente_id = excluded.paciente_id, etapa = excluded.etapa,
                       opcoes = excluded.opcoes, atualizada_em = excluded.atualizada_em""")
                .param("chave", c.telefone().chaveComparacao())
                .param("telefone", c.telefone().e164())
                .param("paciente", c.pacienteId())
                .param("etapa", c.etapa().name())
                .param("opcoes", String.join(SEPARADOR, c.opcoes()))
                .param("em", Timestamp.from(c.atualizadaEm()))
                .update();
    }

    @Override
    @Transactional
    public void encerrarConversa(Telefone telefone) {
        jdbc.sql("delete from conversas_whatsapp where chave_telefone = :chave")
                .param("chave", telefone.chaveComparacao())
                .update();
    }

    private static List<String> opcoes(String valor) {
        if (valor == null || valor.isBlank()) return List.of();
        return Arrays.asList(valor.split(SEPARADOR));
    }
}
