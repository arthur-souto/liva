package sus.liva.infrastructure.adapter.out.persistencia;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sus.liva.application.port.in.FaltasPorEspecialidade;
import sus.liva.application.port.in.MotivoContado;
import sus.liva.application.port.in.SemanaDeComparecimento;
import sus.liva.application.port.out.AusenciaRepositoryPort;
import sus.liva.application.port.out.ConsultaComSinais;
import sus.liva.domain.model.agendamento.StatusAgendamento;
import sus.liva.domain.model.ausencia.CanalDoMotivo;
import sus.liva.domain.model.ausencia.LigacaoRegistrada;
import sus.liva.domain.model.ausencia.MotivoDeAusencia;
import sus.liva.domain.model.ausencia.MotivoRegistrado;
import sus.liva.domain.model.ausencia.OcorrenciaDeAusencia;
import sus.liva.domain.model.ausencia.ResultadoDaLigacao;
import sus.liva.domain.model.ausencia.SinaisDeRisco;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class AusenciaPostgresAdapter implements AusenciaRepositoryPort {
    private static final RowMapper<MotivoRegistrado> MOTIVO = (rs, i) -> new MotivoRegistrado(
            rs.getObject("agendamento_id", UUID.class), rs.getObject("paciente_id", UUID.class),
            OcorrenciaDeAusencia.valueOf(rs.getString("ocorrencia")),
            rs.getString("motivo") == null ? null : MotivoDeAusencia.valueOf(rs.getString("motivo")),
            rs.getString("canal") == null ? null : CanalDoMotivo.valueOf(rs.getString("canal")),
            instante(rs, "perguntado_em"), instante(rs, "respondido_em"));

    private final JdbcClient jdbc;
    private final ZoneId fuso;

    AusenciaPostgresAdapter(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.fuso = clock.getZone();
    }

    @Override
    @Transactional
    public void salvarMotivo(MotivoRegistrado m) {
        jdbc.sql("""
                insert into motivos_de_ausencia (agendamento_id, paciente_id, ocorrencia, motivo, canal, perguntado_em, respondido_em)
                values (:agendamento, :paciente, :ocorrencia, :motivo, :canal, :perguntado, :respondido)
                on conflict (agendamento_id) do update set
                    ocorrencia = excluded.ocorrencia, motivo = excluded.motivo, canal = excluded.canal,
                    perguntado_em = coalesce(excluded.perguntado_em, motivos_de_ausencia.perguntado_em),
                    respondido_em = excluded.respondido_em""")
                .param("agendamento", m.agendamentoId())
                .param("paciente", m.pacienteId())
                .param("ocorrencia", m.ocorrencia().name())
                .param("motivo", m.motivo() == null ? null : m.motivo().name())
                .param("canal", m.canal() == null ? null : m.canal().name())
                .param("perguntado", timestamp(m.perguntadoEm()))
                .param("respondido", timestamp(m.respondidoEm()))
                .update();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MotivoRegistrado> buscarMotivo(UUID agendamentoId) {
        return jdbc.sql("select * from motivos_de_ausencia where agendamento_id = :id")
                .param("id", agendamentoId)
                .query(MOTIVO)
                .optional();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MotivoRegistrado> perguntaPendente(Collection<UUID> pacientes, Instant desde) {
        if (pacientes.isEmpty()) return Optional.empty();
        return jdbc.sql("""
                select * from motivos_de_ausencia
                where paciente_id in (:pacientes) and motivo is null and perguntado_em >= :desde
                order by perguntado_em desc
                limit 1""")
                .param("pacientes", pacientes)
                .param("desde", Timestamp.from(desde))
                .query(MOTIVO)
                .optional();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SemanaDeComparecimento> semanas(LocalDate primeiraSegunda, int quantidade) {
        return jdbc.sql("""
                with semanas as (
                    select (cast(:primeira as date) + n * 7) as inicio from generate_series(0, :quantidade - 1) n
                )
                select s.inicio,
                       (select count(*) from agendamentos a where a.status = 'COMPARECEU'
                          and a.data_hora >= s.inicio and a.data_hora < s.inicio + 7) as compareceram,
                       (select count(*) from agendamentos a where a.status = 'FALTOU'
                          and a.data_hora >= s.inicio and a.data_hora < s.inicio + 7) as faltaram,
                       (select count(*) from agendamentos a where a.status in ('CANCELADO', 'REAGENDADO')
                          and a.data_hora >= s.inicio and a.data_hora < s.inicio + 7) as cancelaram,
                       (select count(*) from vagas_liberadas v where v.status = 'PREENCHIDA'
                          and v.inicio >= s.inicio and v.inicio < s.inicio + 7) as reaproveitadas
                from semanas s
                order by s.inicio""")
                .param("primeira", primeiraSegunda)
                .param("quantidade", quantidade)
                .query((rs, i) -> new SemanaDeComparecimento(rs.getObject("inicio", LocalDate.class), rs.getLong("compareceram"),
                        rs.getLong("faltaram"), rs.getLong("cancelaram"), rs.getLong("reaproveitadas")))
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MotivoContado> motivos(Instant desde) {
        Map<MotivoDeAusencia, long[]> contagem = new EnumMap<>(MotivoDeAusencia.class);
        jdbc.sql("""
                select motivo,
                       count(*) filter (where ocorrencia = 'FALTA') as faltas,
                       count(*) filter (where ocorrencia = 'CANCELAMENTO') as cancelamentos
                from motivos_de_ausencia
                where motivo is not null and respondido_em >= :desde
                group by motivo""")
                .param("desde", Timestamp.from(desde))
                .query((ResultSet rs) -> {
                    contagem.put(MotivoDeAusencia.valueOf(rs.getString("motivo")),
                            new long[]{rs.getLong("faltas"), rs.getLong("cancelamentos")});
                });

        return Arrays.stream(MotivoDeAusencia.values())
                .map(m -> {
                    long[] c = contagem.getOrDefault(m, new long[2]);
                    return new MotivoContado(m, c[0], c[1]);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FaltasPorEspecialidade> porEspecialidade(LocalDateTime desde, LocalDateTime ate) {
        return jdbc.sql("""
                select esp.codigo, esp.nome,
                       count(*) filter (where a.status = 'COMPARECEU') as compareceram,
                       count(*) filter (where a.status = 'FALTOU') as faltaram
                from agendamentos a
                join profissionais p on p.id = a.profissional_id
                join especialidades esp on esp.codigo = p.especialidade_codigo
                where a.status in ('COMPARECEU', 'FALTOU') and a.data_hora >= :desde and a.data_hora <= :ate
                group by esp.codigo, esp.nome
                order by faltaram desc, esp.nome""")
                .param("desde", desde)
                .param("ate", ate)
                .query((rs, i) -> new FaltasPorEspecialidade(rs.getString("codigo"), rs.getString("nome"), rs.getLong("compareceram"),
                        rs.getLong("faltaram")))
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConsultaComSinais> proximasConsultas(LocalDateTime de, LocalDateTime ate, int limite) {
        return jdbc.sql("""
                select a.id, a.paciente_id, coalesce(nullif(pa.nome_social, ''), pa.nome_completo) as nome, pa.telefone, a.data_hora,
                       esp.nome as especialidade, p.nome as profissional, a.unidade, a.status,
                       (select count(*) from agendamentos x where x.paciente_id = a.paciente_id and x.status = 'FALTOU'
                          and x.data_hora >= cast(:de as timestamp) - interval '365 days' and x.data_hora < :de)::int as faltas,
                       (select count(*) from agendamentos x where x.paciente_id = a.paciente_id and x.status = 'COMPARECEU'
                          and x.data_hora >= cast(:de as timestamp) - interval '365 days' and x.data_hora < :de)::int as comparecimentos,
                       (select count(*) from agendamentos x where x.paciente_id = a.paciente_id and x.id <> a.id
                          and x.status in ('CANCELADO', 'REAGENDADO')
                          and x.data_hora >= cast(:de as timestamp) - interval '90 days')::int as cancelamentos,
                       exists (select 1 from registros_mensagem r where r.agendamento_id = a.id and r.sucesso
                               and r.tipo in ('PEDIDO_CONFIRMACAO', 'AVISO_FINAL_CONFIRMACAO')) as pedido_enviado,
                       exists (select 1 from registros_mensagem r where r.agendamento_id = a.id and not r.sucesso) as nao_entregue,
                       (select min(r.enviado_em) from registros_mensagem r where r.agendamento_id = a.id) as primeiro_envio,
                       not exists (select 1 from agendamentos x where x.paciente_id = a.paciente_id and x.status = 'COMPARECEU') as primeira,
                       l.id as ligacao_id, l.resultado as ligacao_resultado, l.usuario_id as ligacao_usuario,
                       l.registrada_em as ligacao_em
                from agendamentos a
                join pacientes pa on pa.id = a.paciente_id
                left join lateral (select * from ligacoes_de_prevencao lp where lp.agendamento_id = a.id
                                   order by lp.registrada_em desc limit 1) l on true
                left join profissionais p on p.id = a.profissional_id
                left join especialidades esp on esp.codigo = p.especialidade_codigo
                where a.status in ('AGENDADO', 'AGUARDANDO_RESPOSTA', 'CONFIRMADO') and a.data_hora >= :de and a.data_hora < :ate
                order by a.data_hora, a.id
                limit :limite""")
                .param("de", de)
                .param("ate", ate)
                .param("limite", limite)
                .query((rs, i) -> {
                    StatusAgendamento status = StatusAgendamento.valueOf(rs.getString("status"));
                    LocalDateTime dataHora = rs.getObject("data_hora", LocalDateTime.class);
                    Instant primeiroEnvio = instante(rs, "primeiro_envio");
                    Integer antecedencia = primeiroEnvio == null ? null
                            : (int) Duration.between(primeiroEnvio, dataHora.atZone(fuso).toInstant()).toDays();
                    SinaisDeRisco sinais = new SinaisDeRisco(rs.getInt("faltas"), rs.getInt("comparecimentos"), rs.getInt("cancelamentos"),
                            status == StatusAgendamento.CONFIRMADO,
                            status == StatusAgendamento.AGUARDANDO_RESPOSTA && rs.getBoolean("pedido_enviado"),
                            rs.getBoolean("nao_entregue"), antecedencia, rs.getBoolean("primeira"),
                            "NAO_ATENDEU".equals(rs.getString("ligacao_resultado")));
                    LigacaoRegistrada ligacao = rs.getObject("ligacao_id", UUID.class) == null ? null
                            : new LigacaoRegistrada(rs.getObject("ligacao_id", UUID.class), rs.getObject("id", UUID.class),
                                    ResultadoDaLigacao.valueOf(rs.getString("ligacao_resultado")),
                                    rs.getObject("ligacao_usuario", UUID.class), instante(rs, "ligacao_em"));

                    return new ConsultaComSinais(rs.getObject("id", UUID.class), rs.getObject("paciente_id", UUID.class),
                            rs.getString("nome"), rs.getString("telefone"), dataHora, rs.getString("especialidade"),
                            rs.getString("profissional"), rs.getString("unidade"), status, sinais, ligacao);
                })
                .list();
    }

    @Override
    @Transactional
    public void salvarLigacao(LigacaoRegistrada ligacao) {
        jdbc.sql("""
                insert into ligacoes_de_prevencao (id, agendamento_id, resultado, usuario_id, registrada_em)
                values (:id, :agendamento, :resultado, :usuario, :em)""")
                .param("id", ligacao.id())
                .param("agendamento", ligacao.agendamentoId())
                .param("resultado", ligacao.resultado().name())
                .param("usuario", ligacao.usuarioId())
                .param("em", Timestamp.from(ligacao.registradaEm()))
                .update();
    }

    @Override
    @Transactional(readOnly = true)
    public long perguntasFeitas(Instant desde) {
        return jdbc.sql("select count(*) from motivos_de_ausencia where perguntado_em >= :desde")
                .param("desde", Timestamp.from(desde))
                .query(Long.class)
                .single();
    }

    @Override
    @Transactional(readOnly = true)
    public long perguntasRespondidas(Instant desde) {
        return jdbc.sql("select count(*) from motivos_de_ausencia where perguntado_em >= :desde and motivo is not null")
                .param("desde", Timestamp.from(desde))
                .query(Long.class)
                .single();
    }

    private static Timestamp timestamp(Instant instante) {
        return instante == null ? null : Timestamp.from(instante);
    }

    private static Instant instante(ResultSet rs, String coluna) throws SQLException {
        Timestamp t = rs.getTimestamp(coluna);
        return t == null ? null : t.toInstant();
    }
}
