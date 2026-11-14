package sus.liva.infrastructure.adapter.out.persistencia;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sus.liva.application.port.out.RemarcacaoPort;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Component
public class RemarcacaoPostgresAdapter implements RemarcacaoPort {
    private final JdbcClient jdbc;

    RemarcacaoPostgresAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public int contarDesde(UUID pacienteId, Instant desde) {
        return jdbc.sql("select count(*) from remarcacoes where paciente_id = :paciente and canal <> 'RECEPCAO' and remarcada_em >= :desde")
                .param("paciente", pacienteId)
                .param("desde", Timestamp.from(desde))
                .query(Integer.class)
                .single();
    }

    @Override
    @Transactional
    public void registrarRemarcacao(UUID pacienteId, UUID consultaAnterior, UUID consultaNova, String canal, Instant em) {
        jdbc.sql("""
                insert into remarcacoes (paciente_id, consulta_anterior_id, consulta_nova_id, canal, remarcada_em)
                values (:paciente, :anterior, :nova, :canal, :em)""")
                .param("paciente", pacienteId)
                .param("anterior", consultaAnterior)
                .param("nova", consultaNova)
                .param("canal", canal)
                .param("em", Timestamp.from(em))
                .update();
    }
}
