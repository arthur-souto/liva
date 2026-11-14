package sus.liva.infrastructure.adapter.out.persistencia;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import sus.liva.application.exception.ConflitoException;
import sus.liva.application.port.in.ConsultaSemConfirmacao;
import sus.liva.application.port.in.FiltroDaFila;
import sus.liva.application.port.in.IndicadoresDaFila;
import sus.liva.application.port.in.MovimentacaoDaFila;
import sus.liva.application.port.in.OfertaNoPainel;
import sus.liva.application.port.in.OfertaParaPaciente;
import sus.liva.application.port.in.PacienteNaFila;
import sus.liva.application.port.in.Pagina;
import sus.liva.application.port.in.PedidoDePagina;
import sus.liva.application.port.in.PosicaoNaFila;
import sus.liva.application.port.in.VagaNoPainel;
import sus.liva.application.port.out.FilaDeEsperaRepositoryPort;
import sus.liva.application.port.out.Movimentacao;
import sus.liva.domain.model.agendamento.NaturezaConsulta;
import sus.liva.domain.model.agendamento.StatusAgendamento;
import sus.liva.domain.model.agendamento.TipoLembrete;
import sus.liva.domain.model.fila.CanalDeResposta;
import sus.liva.domain.model.fila.DecisaoSemResposta;
import sus.liva.domain.model.fila.EntradaNaFila;
import sus.liva.domain.model.fila.EventoDaFila;
import sus.liva.domain.model.fila.OfertaDeVaga;
import sus.liva.domain.model.fila.OrigemDaVaga;
import sus.liva.domain.model.fila.StatusEntrada;
import sus.liva.domain.model.fila.StatusOferta;
import sus.liva.domain.model.fila.StatusVaga;
import sus.liva.domain.model.fila.VagaLiberada;
import sus.liva.domain.model.triagem.Prioridade;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class FilaDeEsperaPostgresAdapter implements FilaDeEsperaRepositoryPort {
    private static final String OCUPA_AGENDA = lista(Arrays.stream(StatusAgendamento.values()).filter(StatusAgendamento::ocupaAgenda).map(Enum::name));
    private static final String RESERVADA = lista(Arrays.stream(StatusAgendamento.values())
            .filter(s -> s.ocupaAgenda() && s != StatusAgendamento.EM_ANDAMENTO).map(Enum::name));
    private static final String OFERTA_ATIVA = lista(Arrays.stream(StatusOferta.values()).filter(StatusOferta::ativa).map(Enum::name));
    private static final String AGUARDA_VAGA = "('AGUARDANDO_AGENDAMENTO', 'AGENDADO')";
    private static final Map<String, String> CONFLITOS = Map.of(
            "uk_oferta_ativa_por_vaga", "Esta vaga já está sendo oferecida a outra pessoa da fila.",
            "uk_oferta_ativa_por_entrada", "Esta pessoa já tem uma oferta de vaga em aberto.",
            "uk_oferta_por_vaga_e_entrada", "Esta vaga já foi oferecida a esta pessoa.");

    private static final String FAIXA_DA_CONSULTA = "tsrange(a.data_hora, a.data_hora + a.duracao_minutos * interval '1 minute')";
    private static final String CONCORRENTE = """
            f.status = 'AGUARDANDO' and f.especialidade_codigo = e.especialidade_codigo
            and (e.profissional_id is null or f.profissional_id is null or f.profissional_id = e.profissional_id)""";
    private static final String ANTES = "(%s < %s or (%s = %s and (f.entrou_em < e.entrou_em or (f.entrou_em = e.entrou_em and f.id < e.id))))"
            .formatted(ordem("f"), ordem("e"), ordem("f"), ordem("e"));
    private static final String POSICAO = """
            select e.*, esp.nome as nome_especialidade, prof.nome as nome_profissional, t.chegada_em as triagem_em,
                   coalesce(nullif(pa.nome_social, ''), pa.nome_completo) as nome_paciente,
                   (select count(*) from triagens t2 where t2.paciente_id = e.paciente_id
                       and (t2.chegada_em < t.chegada_em or (t2.chegada_em = t.chegada_em and t2.id <= t.id)))::int as triagem_numero,
                   (select min(a.data_hora) from agendamentos a where a.encaminhamento_id = e.encaminhamento_id and a.status in %s) as consulta_atual,
                   case when e.status = 'AGUARDANDO' then (1 + (select count(*) from entradas_fila f where %s and %s))::int end as posicao,
                   case when e.status = 'AGUARDANDO' then (select count(*) from entradas_fila f where %s)::int end as total,
                   case when e.status = 'AGUARDANDO' then (select count(*) from entradas_fila f where %s and %s < %s)::int end as mais_urgentes,
                   (select count(*) / 4.0 from entradas_fila f where f.status = 'ATENDIDA' and f.especialidade_codigo = e.especialidade_codigo
                       and (e.profissional_id is null or f.profissional_id is null or f.profissional_id = e.profissional_id)
                       and f.atualizada_em >= now() - interval '28 days')::float8 as vagas_por_semana
            from entradas_fila e
            join especialidades esp on esp.codigo = e.especialidade_codigo
            join triagens t on t.id = e.triagem_id
            join pacientes pa on pa.id = e.paciente_id
            left join profissionais prof on prof.id = e.profissional_id
            """.formatted(RESERVADA, CONCORRENTE, ANTES, CONCORRENTE, CONCORRENTE, ordem("f"), ordem("e"));
    private static final String FILTRO_DA_ENTRADA = """
             and (cast(:especialidade as varchar) is null or e.especialidade_codigo = :especialidade)
             and (cast(:profissional as uuid) is null or exists (select 1 from profissionais px where px.id = :profissional
                  and px.especialidade_codigo = e.especialidade_codigo and (e.profissional_id is null or e.profissional_id = px.id)))
             and (cast(:unidade as varchar) is null or exists (select 1 from profissionais pu where pu.unidade = :unidade and pu.ativo
                  and pu.especialidade_codigo = e.especialidade_codigo and (e.profissional_id is null or e.profissional_id = pu.id)))
            """;
    private static final String FILTRO_DA_VAGA = """
             and (cast(:especialidade as varchar) is null or v.especialidade_codigo = :especialidade)
             and (cast(:profissional as uuid) is null or v.profissional_id = :profissional)
             and (cast(:unidade as varchar) is null or v.unidade = :unidade)
            """;
    private static final String FILTRO_DA_CONSULTA = """
             and (cast(:especialidade as varchar) is null or pa.especialidade_codigo = :especialidade)
             and (cast(:profissional as uuid) is null or a.profissional_id = :profissional)
             and (cast(:unidade as varchar) is null or pa.unidade = :unidade)
            """;
    private static final String OFERTA_DETALHADA = """
            select o.*, v.inicio, v.duracao_minutos, v.unidade, v.endereco, esp.nome as nome_especialidade, p.nome as nome_profissional,
                   ant.data_hora as antecipa_de, coalesce(nullif(pa.nome_social, ''), pa.nome_completo) as nome_paciente
            from ofertas_de_vaga o
            join vagas_liberadas v on v.id = o.vaga_id
            join especialidades esp on esp.codigo = v.especialidade_codigo
            join profissionais p on p.id = v.profissional_id
            join pacientes pa on pa.id = o.paciente_id
            left join agendamentos ant on ant.id = o.consulta_anterior_id
            """;

    private static final RowMapper<EntradaNaFila> ENTRADA = (rs, i) -> new EntradaNaFila(
            rs.getObject("id", UUID.class), rs.getObject("encaminhamento_id", UUID.class), rs.getObject("triagem_id", UUID.class),
            rs.getObject("paciente_id", UUID.class), rs.getInt("ciclo"), rs.getString("especialidade_codigo"),
            rs.getObject("profissional_id", UUID.class), Prioridade.valueOf(rs.getString("prioridade")),
            NaturezaConsulta.valueOf(rs.getString("natureza")), StatusEntrada.valueOf(rs.getString("status")),
            instante(rs, "entrou_em"), rs.getString("motivo"), instante(rs, "atualizada_em"));
    private static final RowMapper<VagaLiberada> VAGA = (rs, i) -> new VagaLiberada(
            rs.getObject("id", UUID.class), rs.getObject("agendamento_origem_id", UUID.class), rs.getObject("profissional_id", UUID.class),
            rs.getString("especialidade_codigo"), rs.getObject("inicio", LocalDateTime.class), rs.getInt("duracao_minutos"),
            rs.getString("unidade"), rs.getString("endereco"), OrigemDaVaga.valueOf(rs.getString("origem")),
            StatusVaga.valueOf(rs.getString("status")), rs.getString("motivo"), instante(rs, "liberada_em"),
            instante(rs, "proxima_tentativa_em"), instante(rs, "encerrada_em"));
    private static final RowMapper<OfertaDeVaga> OFERTA = (rs, i) -> new OfertaDeVaga(
            rs.getObject("id", UUID.class), rs.getObject("vaga_id", UUID.class), rs.getObject("entrada_id", UUID.class),
            rs.getObject("paciente_id", UUID.class), rs.getObject("consulta_anterior_id", UUID.class),
            StatusOferta.valueOf(rs.getString("status")), instante(rs, "criada_em"), instante(rs, "expira_em"),
            instante(rs, "enviada_em"), rs.getString("id_externo"), instante(rs, "respondida_em"),
            enumOuNulo(rs.getString("canal_resposta"), CanalDeResposta::valueOf), rs.getString("motivo"),
            rs.getObject("consulta_nova_id", UUID.class));
    private static final RowMapper<PosicaoNaFila> POSICAO_NA_FILA = (rs, i) -> new PosicaoNaFila(
            rs.getObject("id", UUID.class), rs.getObject("encaminhamento_id", UUID.class), rs.getObject("triagem_id", UUID.class),
            rs.getInt("triagem_numero"), instante(rs, "triagem_em"), rs.getString("especialidade_codigo"),
            rs.getString("nome_especialidade"), rs.getString("nome_profissional"), Prioridade.valueOf(rs.getString("prioridade")),
            NaturezaConsulta.valueOf(rs.getString("natureza")), StatusEntrada.valueOf(rs.getString("status")),
            rs.getObject("posicao", Integer.class), rs.getObject("total", Integer.class), instante(rs, "entrou_em"),
            rs.getObject("consulta_atual", LocalDateTime.class), rs.getString("motivo"), instante(rs, "atualizada_em"),
            rs.getObject("mais_urgentes", Integer.class), rs.getObject("vagas_por_semana", Double.class));
    private static final RowMapper<OfertaParaPaciente> OFERTA_PARA_PACIENTE = (rs, i) -> {
        LocalDateTime inicio = rs.getObject("inicio", LocalDateTime.class);
        return new OfertaParaPaciente(rs.getObject("id", UUID.class), rs.getObject("entrada_id", UUID.class),
                rs.getString("nome_especialidade"), rs.getString("nome_profissional"), rs.getString("unidade"), rs.getString("endereco"),
                inicio, inicio.plusMinutes(rs.getInt("duracao_minutos")), instante(rs, "expira_em"),
                rs.getObject("antecipa_de", LocalDateTime.class), StatusOferta.valueOf(rs.getString("status")));
    };
    private static final RowMapper<OfertaNoPainel> OFERTA_NO_PAINEL = (rs, i) -> new OfertaNoPainel(
            rs.getObject("id", UUID.class), rs.getObject("entrada_id", UUID.class), rs.getObject("paciente_id", UUID.class),
            rs.getString("nome_paciente"), rs.getString("nome_especialidade"), rs.getString("nome_profissional"),
            rs.getObject("inicio", LocalDateTime.class), StatusOferta.valueOf(rs.getString("status")), instante(rs, "criada_em"),
            instante(rs, "expira_em"), instante(rs, "enviada_em"), instante(rs, "respondida_em"),
            enumOuNulo(rs.getString("canal_resposta"), CanalDeResposta::valueOf), rs.getString("motivo"));

    private final JdbcClient jdbc;

    FilaDeEsperaPostgresAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public List<EntradaNaFila> incluirEntradasPendentes(UUID pacienteId, Instant agora) {
        return jdbc.sql("""
                insert into entradas_fila (id, encaminhamento_id, triagem_id, paciente_id, ciclo, especialidade_codigo, profissional_id,
                                           prioridade, natureza, status, entrou_em, atualizada_em)
                select gen_random_uuid(), enc.id, enc.triagem_id, enc.paciente_id, c.ciclo, enc.especialidade_codigo,
                       enc.profissional_destino_id, enc.prioridade, case when c.ciclo = 0 then 'INICIAL' else 'RETORNO' end, 'AGUARDANDO',
                       case when c.ciclo = 0 then coalesce(t.finalizada_em, :agora) else :agora end, :agora
                from encaminhamentos enc
                join triagens t on t.id = enc.triagem_id and t.status = 'FINALIZADA'
                cross join lateral (select count(*)::int as ciclo from agendamentos a
                                    where a.encaminhamento_id = enc.id and a.status = 'COMPARECEU') c
                where enc.modalidade = 'ELETIVO' and enc.status in %s
                  and (cast(:paciente as uuid) is null or enc.paciente_id = :paciente)
                on conflict (encaminhamento_id, ciclo) do nothing
                returning *""".formatted(AGUARDA_VAGA))
                .param("agora", Timestamp.from(agora))
                .param("paciente", pacienteId)
                .query(ENTRADA)
                .list();
    }

    @Override
    @Transactional
    public List<EntradaNaFila> encerrarEntradasSemNecessidade(UUID pacienteId, Instant agora) {
        return jdbc.sql("""
                update entradas_fila e
                   set status = case when enc.status in ('EM_ATENDIMENTO', 'CONCLUIDO') or c.ciclo > e.ciclo then 'ATENDIDA' else 'ENCERRADA' end,
                       motivo = case when c.ciclo > e.ciclo then 'Consulta realizada'
                                     when enc.status = 'EM_ATENDIMENTO' then 'Encaminhamento em atendimento'
                                     when enc.status = 'CONCLUIDO' then 'Encaminhamento concluído'
                                     else 'Encaminhamento cancelado' end,
                       atualizada_em = :agora
                from encaminhamentos enc,
                     lateral (select count(*)::int as ciclo from agendamentos a where a.encaminhamento_id = enc.id and a.status = 'COMPARECEU') c
                where enc.id = e.encaminhamento_id and e.status in ('AGUARDANDO', 'PAUSADA')
                  and (enc.status not in %s or c.ciclo > e.ciclo)
                  and (cast(:paciente as uuid) is null or e.paciente_id = :paciente)
                returning e.*""".formatted(AGUARDA_VAGA))
                .param("agora", Timestamp.from(agora))
                .param("paciente", pacienteId)
                .query(ENTRADA)
                .list();
    }

    @Override
    @Transactional
    public List<EntradaNaFila> reativarEntradas(UUID pacienteId, Instant agora) {
        return jdbc.sql("""
                update entradas_fila e set status = 'AGUARDANDO', motivo = null, atualizada_em = :agora
                from encaminhamentos enc,
                     lateral (select count(*)::int as ciclo from agendamentos a where a.encaminhamento_id = enc.id and a.status = 'COMPARECEU') c
                where enc.id = e.encaminhamento_id and e.status = 'ATENDIDA' and enc.status = 'AGUARDANDO_AGENDAMENTO'
                  and c.ciclo = e.ciclo
                  and (cast(:paciente as uuid) is null or e.paciente_id = :paciente)
                returning e.*""")
                .param("agora", Timestamp.from(agora))
                .param("paciente", pacienteId)
                .query(ENTRADA)
                .list();
    }

    @Override
    @Transactional
    public List<VagaLiberada> registrarVagasLiberadas(LocalDateTime aPartirDe, Instant agora) {
        return jdbc.sql("""
                insert into vagas_liberadas (id, agendamento_origem_id, profissional_id, especialidade_codigo, inicio, duracao_minutos,
                                             unidade, endereco, origem, status, liberada_em, proxima_tentativa_em)
                select gen_random_uuid(), a.id, a.profissional_id, p.especialidade_codigo, a.data_hora, a.duracao_minutos, p.unidade,
                       p.endereco,
                       case when exists (select 1 from confirmacoes_de_presenca c where c.agendamento_id = a.id and c.decisao = 'LIBERADA')
                                 then 'SEM_CONFIRMACAO'
                            when exists (select 1 from ofertas_de_vaga o where o.consulta_anterior_id = a.id and o.status = 'ACEITA')
                                 then 'ANTECIPACAO'
                            else 'CANCELAMENTO' end,
                       'ABERTA', :agora, :agora
                from agendamentos a
                join profissionais p on p.id = a.profissional_id and p.especialidade_codigo is not null and p.ativo
                where a.status in ('CANCELADO', 'REAGENDADO') and a.profissional_id is not null and a.data_hora > :apartir
                  and not exists (select 1 from vagas_liberadas v where v.agendamento_origem_id = a.id)
                  and not exists (select 1 from agendamentos o where o.profissional_id = a.profissional_id and o.status in %s
                                  and tsrange(o.data_hora, o.data_hora + o.duracao_minutos * interval '1 minute') && %s)
                on conflict do nothing
                returning *""".formatted(OCUPA_AGENDA, FAIXA_DA_CONSULTA))
                .param("agora", Timestamp.from(agora))
                .param("apartir", aPartirDe)
                .query(VAGA)
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EntradaNaFila> buscarEntrada(UUID id) {
        return jdbc.sql("select * from entradas_fila where id = :id").param("id", id).query(ENTRADA).optional();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<EntradaNaFila> entradaParaAtualizar(UUID id) {
        return jdbc.sql("select * from entradas_fila where id = :id for update").param("id", id).query(ENTRADA).optional();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VagaLiberada> buscarVaga(UUID id) {
        return jdbc.sql("select * from vagas_liberadas where id = :id").param("id", id).query(VAGA).optional();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<VagaLiberada> vagaParaAtualizar(UUID id) {
        return jdbc.sql("select * from vagas_liberadas where id = :id for update").param("id", id).query(VAGA).optional();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<VagaLiberada> proximaVagaParaOfertar(Instant agora) {
        return jdbc.sql("""
                select v.* from vagas_liberadas v
                where v.status = 'ABERTA' and v.proxima_tentativa_em <= :agora
                  and not exists (select 1 from ofertas_de_vaga o where o.vaga_id = v.id and o.status in %s)
                order by v.inicio, v.id
                limit 1
                for update skip locked""".formatted(OFERTA_ATIVA))
                .param("agora", Timestamp.from(agora))
                .query(VAGA)
                .optional();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OfertaDeVaga> buscarOferta(UUID id) {
        return jdbc.sql("select * from ofertas_de_vaga where id = :id").param("id", id).query(OFERTA).optional();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<OfertaDeVaga> ofertaParaAtualizar(UUID id) {
        return jdbc.sql("select * from ofertas_de_vaga where id = :id for update").param("id", id).query(OFERTA).optional();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public List<OfertaDeVaga> ofertasVencidasParaAtualizar(Instant agora, int limite) {
        return jdbc.sql("""
                select * from ofertas_de_vaga where status in %s and expira_em <= :agora
                order by expira_em, id limit :limite for update skip locked""".formatted(OFERTA_ATIVA))
                .param("agora", Timestamp.from(agora))
                .param("limite", limite)
                .query(OFERTA)
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OfertaDeVaga> ofertasSemEnvio(Instant agora, Instant criadasAte, Instant tentadasAte, int limite) {
        return jdbc.sql("""
                select * from ofertas_de_vaga
                where status = 'PENDENTE' and expira_em > :agora and criada_em <= :criadas
                  and (envio_tentado_em is null or envio_tentado_em <= :tentadas)
                order by criada_em limit :limite""")
                .param("agora", Timestamp.from(agora))
                .param("criadas", Timestamp.from(criadasAte))
                .param("tentadas", Timestamp.from(tentadasAte))
                .param("limite", limite)
                .query(OFERTA)
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OfertaDeVaga> ofertasAtivasDosPacientes(Collection<UUID> pacientes) {
        if (pacientes.isEmpty()) return List.of();
        return jdbc.sql("select * from ofertas_de_vaga where paciente_id in (:pacientes) and status in %s order by expira_em, id"
                        .formatted(OFERTA_ATIVA))
                .param("pacientes", pacientes)
                .query(OFERTA)
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OfertaDeVaga> ultimaOfertaDosPacientes(Collection<UUID> pacientes, Instant desde) {
        if (pacientes.isEmpty()) return Optional.empty();
        return jdbc.sql("""
                select * from ofertas_de_vaga where paciente_id in (:pacientes) and criada_em >= :desde
                order by criada_em desc, id limit 1""")
                .param("pacientes", pacientes)
                .param("desde", Timestamp.from(desde))
                .query(OFERTA)
                .optional();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EntradaNaFila> proximaElegivel(VagaLiberada vaga) {
        return jdbc.sql("""
                select e.* from entradas_fila e
                join encaminhamentos enc on enc.id = e.encaminhamento_id
                where e.status = 'AGUARDANDO' and e.especialidade_codigo = :especialidade
                  and (e.profissional_id is null or e.profissional_id = :profissional)
                  and enc.modalidade = 'ELETIVO' and enc.status in %s
                  and not exists (select 1 from ofertas_de_vaga o where o.entrada_id = e.id and (o.vaga_id = :vaga or o.status in %s))
                  and not exists (select 1 from agendamentos origem where origem.id = :origem
                                  and origem.encaminhamento_id = e.encaminhamento_id)
                  and not exists (select 1 from agendamentos a where a.encaminhamento_id = e.encaminhamento_id and a.status in %s
                                  and a.data_hora <= :inicio)
                  and not exists (select 1 from agendamentos a where a.paciente_id = e.paciente_id and a.status in %s
                                  and a.encaminhamento_id is distinct from e.encaminhamento_id
                                  and %s && tsrange(:inicio, :fim))
                order by %s, e.entrou_em, e.id
                limit 1""".formatted(AGUARDA_VAGA, OFERTA_ATIVA, OCUPA_AGENDA, OCUPA_AGENDA, FAIXA_DA_CONSULTA, ordem("e")))
                .param("especialidade", vaga.getEspecialidade())
                .param("profissional", vaga.getProfissionalId())
                .param("vaga", vaga.getId())
                .param("origem", vaga.getAgendamentoOrigemId())
                .param("inicio", vaga.getInicio())
                .param("fim", vaga.getFim())
                .query(ENTRADA)
                .optional();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> consultaQueOcupaAgenda(UUID encaminhamentoId) {
        return jdbc.sql("select id from agendamentos where encaminhamento_id = :id and status in %s order by data_hora limit 1"
                        .formatted(RESERVADA))
                .param("id", encaminhamentoId)
                .query(UUID.class)
                .optional();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean horarioOcupado(UUID profissionalId, LocalDateTime inicio, LocalDateTime fim) {
        return jdbc.sql("select exists (select 1 from agendamentos a where a.profissional_id = :profissional and a.status in %s and %s && tsrange(:inicio, :fim))"
                        .formatted(OCUPA_AGENDA, FAIXA_DA_CONSULTA))
                .param("profissional", profissionalId)
                .param("inicio", inicio)
                .param("fim", fim)
                .query(Boolean.class)
                .single();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean pacienteOcupado(UUID pacienteId, LocalDateTime inicio, LocalDateTime fim, UUID excetoConsulta) {
        return jdbc.sql("""
                select exists (select 1 from agendamentos a where a.paciente_id = :paciente and a.status in %s
                               and a.id is distinct from cast(:exceto as uuid) and %s && tsrange(:inicio, :fim))"""
                        .formatted(OCUPA_AGENDA, FAIXA_DA_CONSULTA))
                .param("paciente", pacienteId)
                .param("exceto", excetoConsulta)
                .param("inicio", inicio)
                .param("fim", fim)
                .query(Boolean.class)
                .single();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean horarioEmOfertaParaOutro(UUID profissionalId, LocalDateTime inicio, LocalDateTime fim, UUID pacienteId) {
        return !vagasEmOfertaParaOutros(profissionalId, inicio, fim, pacienteId).isEmpty();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VagaLiberada> vagasEmOfertaParaOutros(UUID profissionalId, LocalDateTime de, LocalDateTime ate, UUID pacienteId) {
        return jdbc.sql("""
                select v.* from vagas_liberadas v
                where v.profissional_id = :profissional and v.status = 'ABERTA'
                  and tsrange(v.inicio, v.inicio + v.duracao_minutos * interval '1 minute') && tsrange(:de, :ate)
                  and exists (select 1 from ofertas_de_vaga o where o.vaga_id = v.id and o.status in %s and o.paciente_id <> :paciente)"""
                        .formatted(OFERTA_ATIVA))
                .param("profissional", profissionalId)
                .param("de", de)
                .param("ate", ate)
                .param("paciente", pacienteId)
                .query(VAGA)
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public int ofertasDaVaga(UUID vagaId) {
        return jdbc.sql("select count(*) from ofertas_de_vaga where vaga_id = :vaga").param("vaga", vagaId).query(Integer.class).single();
    }

    @Override
    @Transactional
    public void salvarEntrada(EntradaNaFila e) {
        jdbc.sql("""
                insert into entradas_fila (id, encaminhamento_id, triagem_id, paciente_id, ciclo, especialidade_codigo, profissional_id,
                                           prioridade, natureza, status, entrou_em, motivo, atualizada_em)
                values (:id, :encaminhamento, :triagem, :paciente, :ciclo, :especialidade, :profissional, :prioridade, :natureza, :status,
                        :entrou, :motivo, :atualizada)
                on conflict (id) do update set status = excluded.status, motivo = excluded.motivo, atualizada_em = excluded.atualizada_em""")
                .param("id", e.getId())
                .param("encaminhamento", e.getEncaminhamentoId())
                .param("triagem", e.getTriagemId())
                .param("paciente", e.getPacienteId())
                .param("ciclo", e.getCiclo())
                .param("especialidade", e.getEspecialidade())
                .param("profissional", e.getProfissionalId())
                .param("prioridade", e.getPrioridade().name())
                .param("natureza", e.getNatureza().name())
                .param("status", e.getStatus().name())
                .param("entrou", Timestamp.from(e.getEntrouEm()))
                .param("motivo", e.getMotivo())
                .param("atualizada", Timestamp.from(e.getAtualizadaEm()))
                .update();
    }

    @Override
    @Transactional
    public void salvarVaga(VagaLiberada v) {
        jdbc.sql("""
                insert into vagas_liberadas (id, agendamento_origem_id, profissional_id, especialidade_codigo, inicio, duracao_minutos,
                                             unidade, endereco, origem, status, motivo, liberada_em, proxima_tentativa_em, encerrada_em)
                values (:id, :origem_id, :profissional, :especialidade, :inicio, :duracao, :unidade, :endereco, :origem, :status, :motivo,
                        :liberada, :proxima, :encerrada)
                on conflict (id) do update set status = excluded.status, motivo = excluded.motivo,
                    proxima_tentativa_em = excluded.proxima_tentativa_em, encerrada_em = excluded.encerrada_em""")
                .param("id", v.getId())
                .param("origem_id", v.getAgendamentoOrigemId())
                .param("profissional", v.getProfissionalId())
                .param("especialidade", v.getEspecialidade())
                .param("inicio", v.getInicio())
                .param("duracao", v.getDuracaoMinutos())
                .param("unidade", v.getUnidade())
                .param("endereco", v.getEndereco())
                .param("origem", v.getOrigem().name())
                .param("status", v.getStatus().name())
                .param("motivo", v.getMotivo())
                .param("liberada", Timestamp.from(v.getLiberadaEm()))
                .param("proxima", Timestamp.from(v.getProximaTentativaEm()))
                .param("encerrada", timestamp(v.getEncerradaEm()))
                .update();
    }

    @Override
    @Transactional
    public void salvarOferta(OfertaDeVaga o) {
        try {
            jdbc.sql("""
                    insert into ofertas_de_vaga (id, vaga_id, entrada_id, paciente_id, consulta_anterior_id, status, criada_em, expira_em,
                                                 enviada_em, id_externo, respondida_em, canal_resposta, motivo, consulta_nova_id)
                    values (:id, :vaga, :entrada, :paciente, :anterior, :status, :criada, :expira, :enviada, :externo, :respondida, :canal,
                            :motivo, :nova)
                    on conflict (id) do update set status = excluded.status, enviada_em = excluded.enviada_em,
                        id_externo = excluded.id_externo, respondida_em = excluded.respondida_em,
                        canal_resposta = excluded.canal_resposta, motivo = excluded.motivo, consulta_nova_id = excluded.consulta_nova_id""")
                    .param("id", o.getId())
                    .param("vaga", o.getVagaId())
                    .param("entrada", o.getEntradaId())
                    .param("paciente", o.getPacienteId())
                    .param("anterior", o.getConsultaAnteriorId())
                    .param("status", o.getStatus().name())
                    .param("criada", Timestamp.from(o.getCriadaEm()))
                    .param("expira", Timestamp.from(o.getExpiraEm()))
                    .param("enviada", timestamp(o.getEnviadaEm()))
                    .param("externo", o.getIdExterno())
                    .param("respondida", timestamp(o.getRespondidaEm()))
                    .param("canal", o.getCanal() == null ? null : o.getCanal().name())
                    .param("motivo", o.getMotivo())
                    .param("nova", o.getConsultaNovaId())
                    .update();
        } catch (DuplicateKeyException e) {
            String detalhe = String.valueOf(e.getMostSpecificCause().getMessage());
            throw CONFLITOS.entrySet().stream()
                    .filter(c -> detalhe.contains(c.getKey()))
                    .findFirst()
                    .<RuntimeException>map(c -> new ConflitoException(c.getValue()))
                    .orElse(e);
        }
    }

    @Override
    @Transactional
    public boolean reivindicarEnvio(UUID ofertaId, Instant agora, Instant tentadasAte) {
        return jdbc.sql("""
                update ofertas_de_vaga set envio_tentado_em = :agora
                where id = :id and status = 'PENDENTE' and (envio_tentado_em is null or envio_tentado_em <= :tentadas)""")
                .param("agora", Timestamp.from(agora))
                .param("id", ofertaId)
                .param("tentadas", Timestamp.from(tentadasAte))
                .update() == 1;
    }

    @Override
    @Transactional
    public boolean reivindicar(String chave, Instant agora) {
        return jdbc.sql("insert into tarefas_executadas (chave, executada_em) values (:chave, :agora) on conflict (chave) do nothing")
                .param("chave", chave)
                .param("agora", Timestamp.from(agora))
                .update() == 1;
    }

    @Override
    @Transactional
    public void registrar(Movimentacao m) {
        jdbc.sql("""
                insert into movimentacoes_fila (entrada_id, oferta_id, vaga_id, agendamento_id, paciente_id, evento, detalhe, ator_id,
                                                papel, registrada_em)
                values (:entrada, :oferta, :vaga, :agendamento, :paciente, :evento, :detalhe, :ator, :papel, :em)""")
                .param("entrada", m.entradaId())
                .param("oferta", m.ofertaId())
                .param("vaga", m.vagaId())
                .param("agendamento", m.agendamentoId())
                .param("paciente", m.pacienteId())
                .param("evento", m.evento().name())
                .param("detalhe", m.detalhe() == null || m.detalhe().length() <= 500 ? m.detalhe() : m.detalhe().substring(0, 500))
                .param("ator", m.atorId())
                .param("papel", m.papel())
                .param("em", Timestamp.from(m.registradaEm()))
                .update();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> consultasParaAvisoFinal(LocalDateTime de, LocalDateTime ate, int limite) {
        return jdbc.sql("""
                select a.id from agendamentos a
                where a.status = 'AGUARDANDO_RESPOSTA' and a.data_hora > :de and a.data_hora <= :ate
                  and not exists (select 1 from confirmacoes_de_presenca c where c.agendamento_id = a.id)
                  and not exists (select 1 from registros_mensagem r where r.agendamento_id = a.id and r.tipo = :tipo and r.sucesso)
                order by a.data_hora limit :limite""")
                .param("de", de)
                .param("ate", ate)
                .param("tipo", TipoLembrete.AVISO_FINAL_CONFIRMACAO.name())
                .param("limite", limite)
                .query(UUID.class)
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> consultasComPrazoVencido(LocalDateTime agora, LocalDateTime limite, int quantidade) {
        return jdbc.sql("""
                select a.id from agendamentos a
                where a.status = 'AGUARDANDO_RESPOSTA' and a.data_hora > :agora and a.data_hora <= :limite
                  and not exists (select 1 from confirmacoes_de_presenca c where c.agendamento_id = a.id)
                order by a.data_hora limit :quantidade""")
                .param("agora", agora)
                .param("limite", limite)
                .param("quantidade", quantidade)
                .query(UUID.class)
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean pedidoDeConfirmacaoEntregue(UUID agendamentoId, boolean exigirEntrega) {
        return jdbc.sql("""
                select exists (select 1 from registros_mensagem r where r.agendamento_id = :id and r.sucesso
                               and r.tipo in (:tipos) and (not :exigir or r.entregue_em is not null))""")
                .param("id", agendamentoId)
                .param("tipos", List.of(TipoLembrete.PEDIDO_CONFIRMACAO.name(), TipoLembrete.AVISO_FINAL_CONFIRMACAO.name()))
                .param("exigir", exigirEntrega)
                .query(Boolean.class)
                .single();
    }

    @Override
    @Transactional
    public boolean registrarDecisao(UUID agendamentoId, DecisaoSemResposta decisao, Instant em, UUID atorId, String papel) {
        return jdbc.sql("""
                insert into confirmacoes_de_presenca (agendamento_id, decisao, decidida_em, ator_id, papel)
                values (:id, :decisao, :em, :ator, :papel) on conflict (agendamento_id) do nothing""")
                .param("id", agendamentoId)
                .param("decisao", decisao.name())
                .param("em", Timestamp.from(em))
                .param("ator", atorId)
                .param("papel", papel)
                .update() == 1;
    }

    @Override
    @Transactional
    public void substituirDecisao(UUID agendamentoId, DecisaoSemResposta decisao, Instant em, UUID atorId, String papel) {
        jdbc.sql("""
                insert into confirmacoes_de_presenca (agendamento_id, decisao, decidida_em, ator_id, papel)
                values (:id, :decisao, :em, :ator, :papel)
                on conflict (agendamento_id) do update set decisao = excluded.decisao, decidida_em = excluded.decidida_em,
                    ator_id = excluded.ator_id, papel = excluded.papel""")
                .param("id", agendamentoId)
                .param("decisao", decisao.name())
                .param("em", Timestamp.from(em))
                .param("ator", atorId)
                .param("papel", papel)
                .update();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PosicaoNaFila> posicoesDoPaciente(UUID pacienteId, Instant recentesDesde) {
        return jdbc.sql(POSICAO + """
                 where e.paciente_id = :paciente and (e.status in ('AGUARDANDO', 'PAUSADA') or e.atualizada_em >= :desde)
                 order by t.chegada_em desc, e.entrou_em, e.id""")
                .param("paciente", pacienteId)
                .param("desde", Timestamp.from(recentesDesde))
                .query(POSICAO_NA_FILA)
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OfertaParaPaciente> ofertasAtivasDoPaciente(UUID pacienteId) {
        return jdbc.sql(OFERTA_DETALHADA + " where o.paciente_id = :paciente and o.status in %s order by o.expira_em".formatted(OFERTA_ATIVA))
                .param("paciente", pacienteId)
                .query(OFERTA_PARA_PACIENTE)
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PosicaoNaFila> posicao(UUID entradaId) {
        return jdbc.sql(POSICAO + " where e.id = :id").param("id", entradaId).query(POSICAO_NA_FILA).optional();
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<PacienteNaFila> fila(FiltroDaFila filtro, PedidoDePagina pedido) {
        String onde = " where e.status in ('AGUARDANDO', 'PAUSADA')" + FILTRO_DA_ENTRADA;
        long total = filtrar(jdbc.sql("select count(*) from entradas_fila e" + onde), filtro).query(Long.class).single();
        int ultima = (int) Math.max(1, (total + pedido.tamanho() - 1) / pedido.tamanho());
        int numero = Math.min(pedido.numero(), ultima);
        List<PacienteNaFila> itens = filtrar(jdbc.sql(POSICAO + onde + """
                 order by case e.status when 'AGUARDANDO' then 0 else 1 end, %s, e.entrou_em, e.id
                 limit :limite offset :deslocamento""".formatted(ordem("e"))), filtro)
                .param("limite", pedido.tamanho())
                .param("deslocamento", (numero - 1) * pedido.tamanho())
                .query((rs, i) -> new PacienteNaFila(rs.getObject("paciente_id", UUID.class), rs.getString("nome_paciente"),
                        POSICAO_NA_FILA.mapRow(rs, i), null))
                .list();
        Map<UUID, OfertaParaPaciente> ofertas = itens.isEmpty() ? Map.of() : jdbc.sql(OFERTA_DETALHADA
                        + " where o.entrada_id in (:entradas) and o.status in %s".formatted(OFERTA_ATIVA))
                .param("entradas", itens.stream().map(p -> p.posicao().entradaId()).toList())
                .query(OFERTA_PARA_PACIENTE)
                .list().stream()
                .collect(Collectors.toMap(OfertaParaPaciente::entradaId, Function.identity(), (a, b) -> a));
        return new Pagina<>(itens.stream().map(p -> new PacienteNaFila(p.pacienteId(), p.nomePaciente(), p.posicao(),
                ofertas.get(p.posicao().entradaId()))).toList(), numero, pedido.tamanho(), total);
    }

    @Override
    @Transactional(readOnly = true)
    public IndicadoresDaFila indicadores(FiltroDaFila filtro, Instant desde, LocalDateTime desdeLocal, LocalDateTime agora) {
        return filtrar(jdbc.sql("""
                select
                  (select count(*) from entradas_fila e where e.status = 'AGUARDANDO' %1$s) as aguardando,
                  (select count(*) from entradas_fila e where e.status = 'PAUSADA' %1$s) as pausadas,
                  (select count(*) from vagas_liberadas v where v.status = 'ABERTA' %2$s) as vagas_abertas,
                  (select count(*) from ofertas_de_vaga o join vagas_liberadas v on v.id = o.vaga_id where o.status in %4$s %2$s) as ofertas_ativas,
                  (select count(*) from agendamentos a left join profissionais pa on pa.id = a.profissional_id
                   where a.status = 'AGUARDANDO_RESPOSTA' and a.data_hora > :agora %3$s) as aguardando_confirmacao,
                  (select count(*) from agendamentos a left join profissionais pa on pa.id = a.profissional_id
                   join confirmacoes_de_presenca c on c.agendamento_id = a.id and c.decisao in ('MANTIDA_SEM_RESPOSTA', 'SEM_CONTATO')
                   where a.status = 'AGUARDANDO_RESPOSTA' and a.data_hora > :agora %3$s) as sem_resposta,
                  (select count(*) from vagas_liberadas v where v.status = 'PREENCHIDA' and v.encerrada_em >= :desde %2$s) as reaproveitadas,
                  (select count(*) from ofertas_de_vaga o join vagas_liberadas v on v.id = o.vaga_id
                   where o.status = 'RECUSADA' and o.criada_em >= :desde %2$s) as recusadas,
                  (select count(*) from ofertas_de_vaga o join vagas_liberadas v on v.id = o.vaga_id
                   where o.status = 'EXPIRADA' and o.criada_em >= :desde %2$s) as expiradas,
                  (select count(*) from agendamentos a left join profissionais pa on pa.id = a.profissional_id
                   where exists (select 1 from registros_mensagem r where r.agendamento_id = a.id and r.tipo = 'PEDIDO_CONFIRMACAO'
                                 and r.sucesso and r.enviado_em >= :desde)
                     and (exists (select 1 from mensagens_recebidas m where m.agendamento_id = a.id and m.resposta = 'CONFIRMAR')
                          or exists (select 1 from confirmacoes_de_presenca c where c.agendamento_id = a.id
                                     and c.decisao = 'CONFIRMADA_NA_RECEPCAO')) %3$s) as confirmadas,
                  (select count(*) from agendamentos a left join profissionais pa on pa.id = a.profissional_id
                   where exists (select 1 from registros_mensagem r where r.agendamento_id = a.id and r.tipo = 'PEDIDO_CONFIRMACAO'
                                 and r.sucesso and r.enviado_em >= :desde) %3$s) as pedidos,
                  (select count(*) from agendamentos a left join profissionais pa on pa.id = a.profissional_id
                   where a.status = 'COMPARECEU' and a.data_hora >= :desde_local and a.data_hora <= :agora %3$s) as compareceram,
                  (select count(*) from agendamentos a left join profissionais pa on pa.id = a.profissional_id
                   where a.status = 'FALTOU' and a.data_hora >= :desde_local and a.data_hora <= :agora %3$s) as faltaram
                """.formatted(FILTRO_DA_ENTRADA, FILTRO_DA_VAGA, FILTRO_DA_CONSULTA, OFERTA_ATIVA)), filtro)
                .param("agora", agora)
                .param("desde", Timestamp.from(desde))
                .param("desde_local", desdeLocal)
                .query((rs, i) -> new IndicadoresDaFila(rs.getLong("aguardando"), rs.getLong("pausadas"), rs.getLong("vagas_abertas"),
                        rs.getLong("ofertas_ativas"), rs.getLong("aguardando_confirmacao"), rs.getLong("sem_resposta"),
                        rs.getLong("reaproveitadas"), rs.getLong("recusadas"), rs.getLong("expiradas"), rs.getLong("confirmadas"),
                        rs.getLong("pedidos"), rs.getLong("compareceram"), rs.getLong("faltaram"), desde))
                .single();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VagaNoPainel> vagas(FiltroDaFila filtro, int limite) {
        return filtrar(jdbc.sql("""
                select v.*, esp.nome as nome_especialidade, p.nome as nome_profissional,
                       (select count(*) from ofertas_de_vaga o where o.vaga_id = v.id)::int as ofertas
                from vagas_liberadas v
                join especialidades esp on esp.codigo = v.especialidade_codigo
                join profissionais p on p.id = v.profissional_id
                where true %s
                order by case v.status when 'ABERTA' then 0 else 1 end, coalesce(v.encerrada_em, v.liberada_em) desc, v.id
                limit :limite""".formatted(FILTRO_DA_VAGA)), filtro)
                .param("limite", limite)
                .query((rs, i) -> new VagaNoPainel(rs.getObject("id", UUID.class), rs.getString("nome_especialidade"),
                        rs.getString("nome_profissional"), rs.getString("unidade"), rs.getObject("inicio", LocalDateTime.class),
                        rs.getInt("duracao_minutos"), OrigemDaVaga.valueOf(rs.getString("origem")), StatusVaga.valueOf(rs.getString("status")),
                        rs.getString("motivo"), rs.getInt("ofertas"), instante(rs, "liberada_em")))
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OfertaNoPainel> ofertas(FiltroDaFila filtro, UUID entradaId, int limite) {
        return filtrar(jdbc.sql(OFERTA_DETALHADA + """
                 where (cast(:entrada as uuid) is null or o.entrada_id = :entrada) %s
                 order by case when o.status in %s then 0 else 1 end, o.criada_em desc, o.id
                 limit :limite""".formatted(FILTRO_DA_VAGA, OFERTA_ATIVA)), filtro)
                .param("entrada", entradaId)
                .param("limite", limite)
                .query(OFERTA_NO_PAINEL)
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConsultaSemConfirmacao> semConfirmacao(FiltroDaFila filtro, LocalDateTime agora, int limite) {
        return filtrar(jdbc.sql("""
                select a.id, a.paciente_id, a.data_hora, a.unidade, a.status, c.decisao,
                       coalesce(nullif(pac.nome_social, ''), pac.nome_completo) as nome_paciente, pa.nome as nome_profissional,
                       esp.nome as nome_especialidade,
                       exists (select 1 from registros_mensagem r where r.agendamento_id = a.id and r.sucesso
                               and r.tipo in ('PEDIDO_CONFIRMACAO', 'AVISO_FINAL_CONFIRMACAO')) as enviado,
                       exists (select 1 from registros_mensagem r where r.agendamento_id = a.id and r.sucesso and r.entregue_em is not null
                               and r.tipo in ('PEDIDO_CONFIRMACAO', 'AVISO_FINAL_CONFIRMACAO')) as entregue
                from agendamentos a
                join pacientes pac on pac.id = a.paciente_id
                left join profissionais pa on pa.id = a.profissional_id
                left join especialidades esp on esp.codigo = pa.especialidade_codigo
                left join confirmacoes_de_presenca c on c.agendamento_id = a.id
                where a.status = 'AGUARDANDO_RESPOSTA' and a.data_hora > :agora %s
                order by case when c.decisao is null then 1 else 0 end, a.data_hora, a.id
                limit :limite""".formatted(FILTRO_DA_CONSULTA)), filtro)
                .param("agora", agora)
                .param("limite", limite)
                .query((rs, i) -> new ConsultaSemConfirmacao(rs.getObject("id", UUID.class), rs.getObject("paciente_id", UUID.class),
                        rs.getString("nome_paciente"), rs.getString("nome_especialidade"), rs.getString("nome_profissional"),
                        rs.getString("unidade"), rs.getObject("data_hora", LocalDateTime.class), null,
                        StatusAgendamento.valueOf(rs.getString("status")),
                        enumOuNulo(rs.getString("decisao"), DecisaoSemResposta::valueOf), rs.getBoolean("enviado"), rs.getBoolean("entregue")))
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovimentacaoDaFila> movimentacoes(FiltroDaFila filtro, UUID entradaId, int limite) {
        return filtrar(jdbc.sql("""
                select m.*, coalesce(nullif(pac.nome_social, ''), pac.nome_completo) as nome_paciente
                from movimentacoes_fila m
                left join pacientes pac on pac.id = m.paciente_id
                left join entradas_fila e on e.id = m.entrada_id
                left join vagas_liberadas v on v.id = m.vaga_id
                left join agendamentos a on a.id = m.agendamento_id
                left join profissionais pa on pa.id = coalesce(v.profissional_id, a.profissional_id)
                where (cast(:entrada as uuid) is null or m.entrada_id = :entrada)
                  and (cast(:especialidade as varchar) is null
                       or coalesce(e.especialidade_codigo, v.especialidade_codigo, pa.especialidade_codigo) = :especialidade)
                  and (cast(:profissional as uuid) is null or pa.id = :profissional
                       or (pa.id is null and e.id is not null and (e.profissional_id is null or e.profissional_id = :profissional)))
                  and (cast(:unidade as varchar) is null or pa.unidade = :unidade or (pa.id is null and e.id is not null))
                order by m.registrada_em desc, m.id desc
                limit :limite"""), filtro)
                .param("entrada", entradaId)
                .param("limite", limite)
                .query((rs, i) -> new MovimentacaoDaFila(rs.getLong("id"), EventoDaFila.valueOf(rs.getString("evento")), rs.getString("detalhe"),
                        rs.getObject("entrada_id", UUID.class), rs.getObject("oferta_id", UUID.class), rs.getObject("vaga_id", UUID.class),
                        rs.getObject("agendamento_id", UUID.class), rs.getObject("paciente_id", UUID.class), rs.getString("nome_paciente"),
                        rs.getString("papel"), instante(rs, "registrada_em")))
                .list();
    }

    private static JdbcClient.StatementSpec filtrar(JdbcClient.StatementSpec consulta, FiltroDaFila filtro) {
        FiltroDaFila f = filtro == null ? FiltroDaFila.todas() : filtro;
        return consulta.param("especialidade", vazioComoNulo(f.especialidade()))
                .param("profissional", f.profissionalId())
                .param("unidade", vazioComoNulo(f.unidade()));
    }

    private static String ordem(String alias) {
        return "case %s.prioridade %s end".formatted(alias, Arrays.stream(Prioridade.values())
                .map(p -> "when '%s' then %d".formatted(p.name(), p.ordinal()))
                .collect(Collectors.joining(" ")));
    }

    private static String lista(java.util.stream.Stream<String> valores) {
        return valores.map(v -> "'" + v + "'").collect(Collectors.joining(", ", "(", ")"));
    }

    private static String vazioComoNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor;
    }

    private static <E> E enumOuNulo(String valor, Function<String, E> conversor) {
        return valor == null ? null : conversor.apply(valor);
    }

    private static Instant instante(ResultSet rs, String coluna) throws SQLException {
        Timestamp t = rs.getTimestamp(coluna);
        return t == null ? null : t.toInstant();
    }

    private static Timestamp timestamp(Instant instante) {
        return instante == null ? null : Timestamp.from(instante);
    }
}
