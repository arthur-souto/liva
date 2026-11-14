package sus.liva.application.usecase;

import sus.liva.application.exception.NaoEncontradoException;
import sus.liva.application.port.in.Ator;
import sus.liva.application.port.in.HistoricoDoPaciente;
import sus.liva.application.port.in.HistoricoDoPacienteUseCase;
import sus.liva.application.port.in.Papel;
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
import sus.liva.domain.model.encaminhamento.Encaminhamento;
import sus.liva.domain.model.paciente.Paciente;
import sus.liva.domain.model.triagem.Triagem;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class HistoricoDoPacienteUseCaseImpl implements HistoricoDoPacienteUseCase {
    private final PacienteRepositoryPort pacientes;
    private final TriagemRepositoryPort triagens;
    private final EncaminhamentoRepositoryPort encaminhamentos;
    private final AgendamentoRepositoryPort agendamentos;
    private final AcessoAoRamo acesso;
    private final Auditor auditor;

    public HistoricoDoPacienteUseCaseImpl(PacienteRepositoryPort pacientes, TriagemRepositoryPort triagens,
                                          EncaminhamentoRepositoryPort encaminhamentos, AgendamentoRepositoryPort agendamentos,
                                          ProfissionalRepositoryPort profissionais, AuditoriaPort auditoria, Clock clock) {
        this.pacientes = pacientes;
        this.triagens = triagens;
        this.encaminhamentos = encaminhamentos;
        this.agendamentos = agendamentos;
        this.acesso = new AcessoAoRamo(profissionais);
        this.auditor = new Auditor(auditoria, clock);
    }

    @Override
    public HistoricoDoPaciente historico(Ator ator, UUID pacienteId) {
        Permissoes.exigir(ator, Papel.PACIENTE, Papel.RECEPCAO, Papel.CLINICO_GERAL, Papel.ESPECIALISTA, Papel.ADMINISTRADOR);
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
        return new HistoricoDoPaciente(paciente, ramos,
                consultas.stream().filter(c -> !c.doEncaminhamento()).toList(),
                ramos.stream().anyMatch(r -> r.triagem().status().concluida()));
    }

    @Override
    public boolean podeAcessarPortal(UUID pacienteId) {
        return triagens.possuiTriagemConcluida(pacienteId);
    }

    private RamoDoHistorico ramo(Ator ator, Triagem t, List<Encaminhamento> doRamo, List<Agendamento> todas) {
        List<UUID> ids = doRamo.stream().map(Encaminhamento::getId).toList();
        List<Agendamento> consultas = todas.stream().filter(c -> ids.contains(c.getEncaminhamentoId())).toList();
        boolean clinico = acesso.podeLerClinico(ator, t.getPacienteId(), doRamo, consultas);
        ResumoDaTriagem resumo = new ResumoDaTriagem(t.getId(), t.getStatus(), t.getChegadaEm(), t.getFinalizadaEm(),
                t.getPrioridade(), clinico ? t.getAvaliacao().queixaPrincipal() : null, t.getProfissionalId());
        List<ResumoDoEncaminhamento> resumos = doRamo.stream()
                .map(e -> new ResumoDoEncaminhamento(e.getId(), e.getEspecialidade(), e.getModalidade(), e.getPrioridade(),
                        e.getStatus(), e.getCriadoEm(), e.getProfissionalDestinoId(),
                        clinico ? e.getJustificativa() : null, clinico ? e.getDesfecho() : null, e.getVersao()))
                .toList();
        return new RamoDoHistorico(resumo, resumos, consultas, clinico);
    }
}
