package sus.liva.application.usecase;

import sus.liva.application.exception.NaoEncontradoException;
import sus.liva.application.port.in.Ator;
import sus.liva.application.port.in.EncaminhamentoRecebido;
import sus.liva.application.port.in.EncaminhamentoUseCase;
import sus.liva.application.port.in.Papel;
import sus.liva.application.port.out.AgendamentoRepositoryPort;
import sus.liva.application.port.out.AuditoriaPort;
import sus.liva.application.port.out.EncaminhamentoRepositoryPort;
import sus.liva.application.port.out.PacienteRepositoryPort;
import sus.liva.application.port.out.ProfissionalRepositoryPort;
import sus.liva.application.port.out.TransacaoPort;
import sus.liva.domain.model.encaminhamento.Encaminhamento;
import sus.liva.domain.model.encaminhamento.Modalidade;
import sus.liva.domain.model.encaminhamento.StatusEncaminhamento;
import sus.liva.domain.model.paciente.Paciente;
import sus.liva.domain.model.profissional.Profissional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class EncaminhamentoUseCaseImpl implements EncaminhamentoUseCase {
    private final EncaminhamentoRepositoryPort encaminhamentos;
    private final AgendamentoRepositoryPort agendamentos;
    private final PacienteRepositoryPort pacientes;
    private final ProfissionalRepositoryPort profissionais;
    private final TransacaoPort transacao;
    private final AcessoAoRamo acesso;
    private final Auditor auditor;
    private final Clock clock;

    public EncaminhamentoUseCaseImpl(EncaminhamentoRepositoryPort encaminhamentos, AgendamentoRepositoryPort agendamentos,
                                     PacienteRepositoryPort pacientes, ProfissionalRepositoryPort profissionais,
                                     TransacaoPort transacao, AuditoriaPort auditoria, Clock clock) {
        this.encaminhamentos = encaminhamentos;
        this.agendamentos = agendamentos;
        this.pacientes = pacientes;
        this.profissionais = profissionais;
        this.transacao = transacao;
        this.acesso = new AcessoAoRamo(profissionais);
        this.auditor = new Auditor(auditoria, clock);
        this.clock = clock;
    }

    @Override
    public Encaminhamento receberNoPlantao(Ator ator, UUID encaminhamentoId) {
        Permissoes.exigir(ator, Papel.ESPECIALISTA);
        Profissional profissional = profissionalDo(ator);
        return alterar(ator, encaminhamentoId, "RECEBER_NO_PLANTAO", e -> {
            if (!e.destinadoA(profissional.id()) && !(e.getProfissionalDestinoId() == null && profissional.atende(e.getEspecialidade())))
                throw Permissoes.negado();
            e.receberNoPlantao(profissional.id(), Instant.now(clock));
        });
    }

    @Override
    public Encaminhamento autorizarRetorno(Ator ator, UUID encaminhamentoId) {
        return alterar(ator, encaminhamentoId, "AUTORIZAR_RETORNO", e -> {
            acesso.exigirEspecialistaDoEncaminhamento(ator, e, agendamentos.consultasDoEncaminhamento(e.getId()));
            e.autorizarRetorno();
        });
    }

    @Override
    public Encaminhamento concluir(Ator ator, UUID encaminhamentoId, String desfecho) {
        return alterar(ator, encaminhamentoId, "CONCLUIR_ENCAMINHAMENTO", e -> {
            acesso.exigirEspecialistaDoEncaminhamento(ator, e, agendamentos.consultasDoEncaminhamento(e.getId()));
            e.concluir(desfecho, Instant.now(clock));
        });
    }

    @Override
    public Encaminhamento cancelar(Ator ator, UUID encaminhamentoId, String motivo) {
        Permissoes.exigir(ator, Papel.CLINICO_GERAL, Papel.ESPECIALISTA);
        return alterar(ator, encaminhamentoId, "CANCELAR_ENCAMINHAMENTO", e -> {
            boolean autorizado = ator.eh(Papel.CLINICO_GERAL)
                    ? e.getCriadoPor().equals(ator.profissionalId())
                    : acesso.especialistaVinculado(ator.profissionalId(), List.of(e), agendamentos.consultasDoEncaminhamento(e.getId()));
            if (!autorizado) throw Permissoes.negado();
            e.cancelar(motivo, Instant.now(clock));
        });
    }

    @Override
    public List<EncaminhamentoRecebido> recebidos(Ator ator) {
        Permissoes.exigir(ator, Papel.ESPECIALISTA);
        Profissional profissional = profissionalDo(ator);
        return encaminhamentos.encaminhamentosAbertosDaEspecialidade(profissional.especialidade()).stream()
                .filter(e -> e.getProfissionalDestinoId() == null || e.destinadoA(profissional.id()))
                .filter(e -> e.getModalidade() == Modalidade.IMEDIATO || e.getStatus() != StatusEncaminhamento.PENDENTE)
                .sorted(Comparator.comparing(Encaminhamento::getPrioridade).thenComparing(Encaminhamento::getCriadoEm))
                .map(e -> new EncaminhamentoRecebido(e, pacientes.buscarPaciente(e.getPacienteId()).map(Paciente::nomeCompleto).orElse(null)))
                .toList();
    }

    private Encaminhamento alterar(Ator ator, UUID encaminhamentoId, String acao, Consumer<Encaminhamento> alteracao) {
        Encaminhamento alterado = transacao.executar(() -> {
            Encaminhamento e = encaminhamentos.buscarEncaminhamentoParaAtualizar(encaminhamentoId)
                    .orElseThrow(() -> new NaoEncontradoException("Encaminhamento não encontrado: " + encaminhamentoId));
            alteracao.accept(e);
            encaminhamentos.salvarEncaminhamento(e);
            return e;
        });
        auditor.registrar(ator, acao, "ENCAMINHAMENTO", encaminhamentoId, alterado.getPacienteId());
        return encaminhamentos.buscarEncaminhamento(encaminhamentoId).orElseThrow();
    }

    private Profissional profissionalDo(Ator ator) {
        return profissionais.buscarProfissional(Permissoes.profissional(ator))
                .orElseThrow(() -> new NaoEncontradoException("Profissional não encontrado"));
    }
}
