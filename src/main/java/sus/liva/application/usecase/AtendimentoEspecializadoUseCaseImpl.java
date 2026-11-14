package sus.liva.application.usecase;

import sus.liva.application.exception.ConflitoException;
import sus.liva.application.exception.NaoEncontradoException;
import sus.liva.application.port.in.AtendimentoEspecializadoUseCase;
import sus.liva.application.port.in.Ator;
import sus.liva.application.port.in.ConsultaDaAgenda;
import sus.liva.application.port.in.Papel;
import sus.liva.application.port.out.AgendamentoRepositoryPort;
import sus.liva.application.port.out.AuditoriaPort;
import sus.liva.application.port.out.EncaminhamentoRepositoryPort;
import sus.liva.application.port.out.EvolucaoClinicaPort;
import sus.liva.application.port.out.PacienteRepositoryPort;
import sus.liva.application.port.out.TransacaoPort;
import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.agendamento.EvolucaoClinica;
import sus.liva.domain.model.agendamento.StatusAgendamento;
import sus.liva.domain.model.encaminhamento.Encaminhamento;
import sus.liva.domain.model.paciente.Paciente;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

public class AtendimentoEspecializadoUseCaseImpl implements AtendimentoEspecializadoUseCase {
    private static final int MAXIMO_DE_DIAS = 31;

    private final AgendamentoRepositoryPort agendamentos;
    private final EncaminhamentoRepositoryPort encaminhamentos;
    private final EvolucaoClinicaPort evolucoes;
    private final PacienteRepositoryPort pacientes;
    private final TransacaoPort transacao;
    private final Auditor auditor;
    private final Clock clock;
    private final PerguntaDeMotivo motivos;

    public AtendimentoEspecializadoUseCaseImpl(AgendamentoRepositoryPort agendamentos, EncaminhamentoRepositoryPort encaminhamentos,
                                               EvolucaoClinicaPort evolucoes, PacienteRepositoryPort pacientes,
                                               TransacaoPort transacao, AuditoriaPort auditoria, Clock clock) {
        this(agendamentos, encaminhamentos, evolucoes, pacientes, transacao, auditoria, clock, null);
    }

    public AtendimentoEspecializadoUseCaseImpl(AgendamentoRepositoryPort agendamentos, EncaminhamentoRepositoryPort encaminhamentos,
                                               EvolucaoClinicaPort evolucoes, PacienteRepositoryPort pacientes,
                                               TransacaoPort transacao, AuditoriaPort auditoria, Clock clock, PerguntaDeMotivo motivos) {
        this.agendamentos = agendamentos;
        this.encaminhamentos = encaminhamentos;
        this.evolucoes = evolucoes;
        this.pacientes = pacientes;
        this.transacao = transacao;
        this.auditor = new Auditor(auditoria, clock);
        this.clock = clock;
        this.motivos = motivos;
    }

    @Override
    public List<ConsultaDaAgenda> agenda(Ator ator, LocalDate inicio, LocalDate fim) {
        Permissoes.exigir(ator, Papel.ESPECIALISTA);

        UUID profissional = Permissoes.profissional(ator);
        if (fim.isBefore(inicio)) throw new IllegalArgumentException("A data final deve ser igual ou posterior à inicial");
        if (ChronoUnit.DAYS.between(inicio, fim) >= MAXIMO_DE_DIAS)
            throw new IllegalArgumentException("Consulte no máximo " + MAXIMO_DE_DIAS + " dias por vez");

        return agendamentos.consultasDoProfissional(profissional, inicio.atStartOfDay(), fim.plusDays(1).atStartOfDay()).stream()
                .filter(Agendamento::doEncaminhamento)
                .map(c -> new ConsultaDaAgenda(c,
                        pacientes.buscarPaciente(c.getPacienteId()).map(Paciente::nomeCompleto).orElse(null),
                        encaminhamentos.buscarEncaminhamento(c.getEncaminhamentoId()).orElse(null)))
                .toList();
    }

    @Override
    public Agendamento iniciar(Ator ator, UUID consultaId, long versao) {
        return alterar(ator, consultaId, versao, "INICIAR_CONSULTA", (c, e) -> {
            if (c.getDataHora().toLocalDate().isAfter(LocalDate.now(clock)))
                throw new IllegalStateException("Esta consulta está agendada para outro dia");
            c.iniciarAtendimento();
            e.consultaIniciada();
        });
    }

    @Override
    public Agendamento finalizar(Ator ator, UUID consultaId, String evolucao, boolean autorizarRetorno, long versao) {
        if (ValidacaoCadastral.vazio(evolucao)) throw new IllegalArgumentException("Registre a evolução clínica antes de finalizar");
        return alterar(ator, consultaId, versao, "FINALIZAR_CONSULTA", (c, e) -> {
            c.finalizarAtendimento();
            e.consultaFinalizada(autorizarRetorno);
            evolucoes.salvarEvolucao(new EvolucaoClinica(c.getId(), c.getProfissionalId(), evolucao.trim(), Instant.now(clock)));
        });
    }

    @Override
    public EvolucaoClinica registrarEvolucao(Ator ator, UUID consultaId, String texto) {
        Permissoes.exigir(ator, Papel.ESPECIALISTA);
        if (ValidacaoCadastral.vazio(texto)) throw new IllegalArgumentException("Escreva a evolução clínica");

        Agendamento consulta = agendamentos.buscarAgendamento(consultaId)
                .orElseThrow(() -> new NaoEncontradoException("Consulta não encontrada: " + consultaId));
        if (!consulta.doProfissional(ator.profissionalId())) throw Permissoes.negado();
        if (consulta.getStatus() != StatusAgendamento.EM_ANDAMENTO && consulta.getStatus() != StatusAgendamento.COMPARECEU)
            throw new IllegalStateException("Evoluções só podem ser registradas em consultas em andamento ou realizadas");

        EvolucaoClinica evolucao = new EvolucaoClinica(consultaId, ator.profissionalId(), texto.trim(), Instant.now(clock));
        evolucoes.salvarEvolucao(evolucao);
        auditor.registrar(ator, "REGISTRAR_EVOLUCAO", "CONSULTA", consultaId, consulta.getPacienteId());
        return evolucao;
    }

    @Override
    public Agendamento registrarFalta(Ator ator, UUID consultaId, long versao) {
        Agendamento faltou = alterar(ator, consultaId, versao, "REGISTRAR_FALTA", (c, e) -> {
            if (LocalDateTime.now(clock).isBefore(c.getDataHora()))
                throw new IllegalStateException("A falta só pode ser registrada após o horário da consulta");
            c.registrarFalta();
            e.consultaDesmarcada();
        });

        if (motivos != null) motivos.aposFalta(faltou);
        return faltou;
    }

    private Agendamento alterar(Ator ator, UUID consultaId, long versao, String acao, BiConsumer<Agendamento, Encaminhamento> alteracao) {
        Permissoes.exigir(ator, Papel.ESPECIALISTA);

        UUID profissional = Permissoes.profissional(ator);
        Agendamento alterada = transacao.executar(() -> {
            Agendamento c = agendamentos.buscarAgendamentoParaAtualizar(consultaId)
                    .orElseThrow(() -> new NaoEncontradoException("Consulta não encontrada: " + consultaId));
            if (!c.doProfissional(profissional)) throw Permissoes.negado();
            if (c.getVersao() != null && c.getVersao() != versao)
                throw new ConflitoException("A consulta foi alterada por outra pessoa. Recarregue antes de continuar.");

            Encaminhamento e = encaminhamentos.buscarEncaminhamentoParaAtualizar(c.getEncaminhamentoId())
                    .orElseThrow(() -> new NaoEncontradoException("Encaminhamento da consulta não encontrado"));
            alteracao.accept(c, e);
            agendamentos.salvarAgendamento(c);
            encaminhamentos.salvarEncaminhamento(e);
            return c;
        });

        auditor.registrar(ator, acao, "CONSULTA", consultaId, alterada.getPacienteId());
        return agendamentos.buscarAgendamento(consultaId).orElseThrow();
    }
}
