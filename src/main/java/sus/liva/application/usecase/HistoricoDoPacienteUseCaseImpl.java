package sus.liva.application.usecase;

import sus.liva.application.exception.NaoEncontradoException;
import sus.liva.application.port.in.AtendimentoDoPaciente;
import sus.liva.application.port.in.AtendimentosDoPaciente;
import sus.liva.application.port.in.Ator;
import sus.liva.application.port.in.ConsultaDoPaciente;
import sus.liva.application.port.in.ConsultasDoPaciente;
import sus.liva.application.port.in.FiltroDeAtendimentos;
import sus.liva.application.port.in.FiltroDeConsultas;
import sus.liva.application.port.in.HistoricoDoPaciente;
import sus.liva.application.port.in.HistoricoDoPacienteUseCase;
import sus.liva.application.port.in.Pagina;
import sus.liva.application.port.in.Papel;
import sus.liva.application.port.in.PedidoDePagina;
import sus.liva.application.port.in.PeriodoDasConsultas;
import sus.liva.application.port.in.RamoDoHistorico;
import sus.liva.application.port.in.ResumoDaTriagem;
import sus.liva.application.port.in.ResumoDoEncaminhamento;
import sus.liva.application.port.out.AgendamentoRepositoryPort;
import sus.liva.application.port.out.AuditoriaPort;
import sus.liva.application.port.out.EncaminhamentoRepositoryPort;
import sus.liva.application.port.out.PacienteRepositoryPort;
import sus.liva.application.port.out.ProfissionalRepositoryPort;
import sus.liva.application.port.out.TriagemRepositoryPort;
import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.agendamento.StatusAgendamento;
import sus.liva.domain.model.encaminhamento.Encaminhamento;
import sus.liva.domain.model.paciente.Paciente;
import sus.liva.domain.model.profissional.Especialidade;
import sus.liva.domain.model.profissional.Profissional;
import sus.liva.domain.model.triagem.Triagem;

import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class HistoricoDoPacienteUseCaseImpl implements HistoricoDoPacienteUseCase {
    private static final Set<StatusAgendamento> ENCERRADAS = Set.of(StatusAgendamento.CANCELADO, StatusAgendamento.COMPARECEU,
            StatusAgendamento.FALTOU);
    private static final String ATENDIMENTO = "Atendimento";

    private final PacienteRepositoryPort pacientes;
    private final TriagemRepositoryPort triagens;
    private final EncaminhamentoRepositoryPort encaminhamentos;
    private final AgendamentoRepositoryPort agendamentos;
    private final ProfissionalRepositoryPort profissionais;
    private final AcessoAoRamo acesso;
    private final Auditor auditor;
    private final Clock clock;

    public HistoricoDoPacienteUseCaseImpl(PacienteRepositoryPort pacientes, TriagemRepositoryPort triagens,
                                          EncaminhamentoRepositoryPort encaminhamentos, AgendamentoRepositoryPort agendamentos,
                                          ProfissionalRepositoryPort profissionais, AuditoriaPort auditoria, Clock clock) {
        this.pacientes = pacientes;
        this.triagens = triagens;
        this.encaminhamentos = encaminhamentos;
        this.agendamentos = agendamentos;
        this.profissionais = profissionais;
        this.acesso = new AcessoAoRamo(profissionais);
        this.auditor = new Auditor(auditoria, clock);
        this.clock = clock;
    }

    @Override
    public HistoricoDoPaciente historico(Ator ator, UUID pacienteId) {
        Leitura leitura = ler(ator, pacienteId);
        return new HistoricoDoPaciente(leitura.paciente(), leitura.ramos(),
                leitura.consultas().stream().filter(c -> !c.doEncaminhamento()).toList(),
                leitura.ramos().stream().anyMatch(r -> r.triagem().status().concluida()));
    }

    @Override
    public AtendimentosDoPaciente atendimentos(Ator ator, UUID pacienteId, FiltroDeAtendimentos filtro, PedidoDePagina pedido) {
        List<RamoDoHistorico> ramos = ler(ator, pacienteId).ramos();
        List<AtendimentoDoPaciente> numerados = IntStream.range(0, ramos.size())
                .mapToObj(i -> new AtendimentoDoPaciente(ramos.size() - i, ramos.get(i)))
                .filter(a -> atende(filtro, a.ramo()))
                .toList();
        List<String> especialidades = ramos.stream().flatMap(r -> r.encaminhamentos().stream())
                .map(ResumoDoEncaminhamento::especialidade).distinct().sorted().toList();
        List<Integer> anos = ramos.stream().map(r -> ano(r.triagem().chegadaEm())).distinct()
                .sorted(Comparator.reverseOrder()).toList();
        return new AtendimentosDoPaciente(Pagina.de(numerados, pedido), especialidades, anos);
    }

    @Override
    public ConsultasDoPaciente consultas(Ator ator, UUID pacienteId, FiltroDeConsultas filtro, PedidoDePagina pedido) {
        Leitura leitura = ler(ator, pacienteId);
        Map<UUID, ResumoDoEncaminhamento> porId = leitura.ramos().stream().flatMap(r -> r.encaminhamentos().stream())
                .collect(Collectors.toMap(ResumoDoEncaminhamento::id, e -> e));
        LocalDateTime agora = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);
        List<ConsultaDoPaciente> todas = leitura.consultas().stream().map(c -> consultaDoPaciente(c, porId)).toList();
        List<ConsultaDoPaciente> proximas = todas.stream().filter(c -> proxima(c.consulta(), agora))
                .sorted(Comparator.comparing(c -> c.consulta().getDataHora())).toList();
        List<ConsultaDoPaciente> anteriores = todas.stream().filter(c -> !proxima(c.consulta(), agora))
                .sorted(Comparator.comparing((ConsultaDoPaciente c) -> c.consulta().getDataHora()).reversed()).toList();
        List<ConsultaDoPaciente> doPeriodo = filtro.periodo() == PeriodoDasConsultas.ANTERIORES ? anteriores : proximas;

        String termo = normalizar(filtro.busca());
        List<ConsultaDoPaciente> filtradas = doPeriodo.stream()
                .filter(c -> termo.isEmpty() || normalizar(c.especialidade() + " " + c.profissional()).contains(termo))
                .filter(c -> vazio(filtro.especialidade()) || c.especialidade().equals(filtro.especialidade()))
                .filter(c -> filtro.situacao() == null || c.consulta().getStatus() == filtro.situacao())
                .filter(c -> filtro.ano() == null || c.consulta().getDataHora().getYear() == filtro.ano())
                .toList();
        return new ConsultasDoPaciente(Pagina.de(filtradas, pedido), proximas.size(), anteriores.size(),
                doPeriodo.stream().map(ConsultaDoPaciente::especialidade).distinct().sorted().toList(),
                doPeriodo.stream().map(c -> c.consulta().getStatus()).distinct().sorted().toList(),
                doPeriodo.stream().map(c -> c.consulta().getDataHora().getYear()).distinct().sorted(Comparator.reverseOrder()).toList());
    }

    @Override
    public boolean podeAcessarPortal(UUID pacienteId) {
        return triagens.possuiTriagemConcluida(pacienteId);
    }

    private record Leitura(Paciente paciente, List<RamoDoHistorico> ramos, List<Agendamento> consultas) {}

    private Leitura ler(Ator ator, UUID pacienteId) {
        Permissoes.exigir(ator, Papel.PACIENTE, Papel.CUIDADOR, Papel.RECEPCAO, Papel.CLINICO_GERAL, Papel.ESPECIALISTA, Papel.ADMINISTRADOR);
        Permissoes.exigirPacienteProprio(ator, pacienteId);
        Paciente paciente = pacientes.buscarPaciente(pacienteId)
                .orElseThrow(() -> new NaoEncontradoException("Paciente não encontrado: " + pacienteId));
        List<Agendamento> consultas = agendamentos.consultasDoPaciente(pacienteId);
        Map<UUID, List<Encaminhamento>> porTriagem = encaminhamentos.encaminhamentosDoPaciente(pacienteId).stream()
                .collect(Collectors.groupingBy(Encaminhamento::getTriagemId));

        List<RamoDoHistorico> ramos = triagens.triagensDoPaciente(pacienteId).stream()
                .map(t -> ramo(ator, t, porTriagem.getOrDefault(t.getId(), List.of()), consultas))
                .toList();
        if (ator.eh(Papel.ESPECIALISTA) && ramos.stream().noneMatch(RamoDoHistorico::acessoClinico)) throw Permissoes.negado();

        auditor.registrar(ator, "LER_HISTORICO", "PACIENTE", pacienteId, pacienteId);
        return new Leitura(paciente, ramos, consultas);
    }

    private boolean atende(FiltroDeAtendimentos filtro, RamoDoHistorico ramo) {
        return (vazio(filtro.especialidade()) || ramo.encaminhamentos().stream().anyMatch(e -> e.especialidade().equals(filtro.especialidade())))
                && (filtro.emAndamento() == null || filtro.emAndamento() == emAndamento(ramo))
                && (filtro.ano() == null || ano(ramo.triagem().chegadaEm()) == filtro.ano());
    }

    private static boolean emAndamento(RamoDoHistorico ramo) {
        return ramo.triagem().status().aberta() || ramo.encaminhamentos().stream().anyMatch(e -> !e.status().encerrado());
    }

    private static boolean proxima(Agendamento consulta, LocalDateTime agora) {
        if (consulta.getStatus() == StatusAgendamento.EM_ANDAMENTO) return true;
        return !ENCERRADAS.contains(consulta.getStatus()) && !consulta.getDataHora().isBefore(agora);
    }

    private ConsultaDoPaciente consultaDoPaciente(Agendamento consulta, Map<UUID, ResumoDoEncaminhamento> encaminhamentosPorId) {
        ResumoDoEncaminhamento encaminhamento = consulta.getEncaminhamentoId() == null ? null : encaminhamentosPorId.get(consulta.getEncaminhamentoId());
        String especialidade = encaminhamento == null ? ATENDIMENTO
                : profissionais.buscarEspecialidade(encaminhamento.especialidade()).map(Especialidade::nome).orElse(encaminhamento.especialidade());
        String profissional = Optional.ofNullable(consulta.getProfissionalId()).flatMap(profissionais::buscarProfissional)
                .map(Profissional::nome).orElse("");
        return new ConsultaDoPaciente(consulta, encaminhamento, especialidade, profissional);
    }

    private int ano(Instant instante) {
        return instante.atZone(clock.getZone()).getYear();
    }

    private static boolean vazio(String texto) {
        return texto == null || texto.isBlank();
    }

    private static String normalizar(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase().trim();
    }

    private RamoDoHistorico ramo(Ator ator, Triagem t, List<Encaminhamento> doRamo, List<Agendamento> todas) {
        List<UUID> ids = doRamo.stream().map(Encaminhamento::getId).toList();
        List<Agendamento> consultas = todas.stream().filter(c -> ids.contains(c.getEncaminhamentoId())).toList();
        boolean clinico = acesso.podeLerClinico(ator, t.getPacienteId(), doRamo, consultas);
        ResumoDaTriagem resumo = new ResumoDaTriagem(t.getId(), t.getStatus(), t.getChegadaEm(), t.getFinalizadaEm(),
                t.getPrioridade(), clinico ? t.getAvaliacao().queixaPrincipal() : null, t.getProfissionalId(),
                clinico ? t.getSinaisVitais() : null);
        List<ResumoDoEncaminhamento> resumos = doRamo.stream()
                .map(e -> new ResumoDoEncaminhamento(e.getId(), e.getEspecialidade(), e.getModalidade(), e.getPrioridade(),
                        e.getStatus(), e.getCriadoEm(), e.getProfissionalDestinoId(),
                        clinico ? e.getJustificativa() : null, clinico ? e.getDesfecho() : null, e.getVersao()))
                .toList();
        return new RamoDoHistorico(resumo, resumos, consultas, clinico);
    }
}
