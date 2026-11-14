package sus.liva.application.usecase;

import sus.liva.application.exception.NaoEncontradoException;
import sus.liva.application.port.in.Ator;
import sus.liva.application.port.in.ConsultaEmRisco;
import sus.liva.application.port.in.FilaDeEsperaUseCase;
import sus.liva.application.port.in.PainelDeFaltas;
import sus.liva.application.port.in.Papel;
import sus.liva.application.port.in.PrevencaoDeFaltasUseCase;
import sus.liva.application.port.out.AgendamentoRepositoryPort;
import sus.liva.application.port.out.AuditoriaPort;
import sus.liva.application.port.out.AusenciaRepositoryPort;
import sus.liva.application.port.out.ConsultaComSinais;
import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.agendamento.StatusAgendamento;
import sus.liva.domain.model.ausencia.CanalDoMotivo;
import sus.liva.domain.model.ausencia.LigacaoRegistrada;
import sus.liva.domain.model.ausencia.MotivoDeAusencia;
import sus.liva.domain.model.ausencia.MotivoRegistrado;
import sus.liva.domain.model.ausencia.OcorrenciaDeAusencia;
import sus.liva.domain.model.ausencia.ResultadoDaLigacao;
import sus.liva.domain.model.ausencia.RiscoDeFalta;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PrevencaoDeFaltasUseCaseImpl implements PrevencaoDeFaltasUseCase {
    private static final int MINIMO_DE_SEMANAS = 4;
    private static final int MAXIMO_DE_SEMANAS = 26;
    private static final int DIAS_A_FRENTE = 7;
    private static final int LIMITE_DE_CONSULTAS = 500;
    private static final int LIMITE_EM_RISCO = 30;
    private static final Duration JANELA_DOS_MOTIVOS = Duration.ofDays(90);

    private final AusenciaRepositoryPort ausencias;
    private final AgendamentoRepositoryPort agendamentos;
    private final FilaDeEsperaUseCase fila;
    private final Auditor auditor;
    private final Clock clock;

    public PrevencaoDeFaltasUseCaseImpl(AusenciaRepositoryPort ausencias, AgendamentoRepositoryPort agendamentos, FilaDeEsperaUseCase fila,
                                        AuditoriaPort auditoria, Clock clock) {
        this.ausencias = ausencias;
        this.agendamentos = agendamentos;
        this.fila = fila;
        this.auditor = new Auditor(auditoria, clock);
        this.clock = clock;
    }

    @Override
    public PainelDeFaltas painel(Ator ator, int semanas) {
        Permissoes.exigir(ator, Papel.ADMINISTRADOR, Papel.RECEPCAO);

        int quantidade = Math.max(MINIMO_DE_SEMANAS, Math.min(MAXIMO_DE_SEMANAS, semanas));
        Instant agora = Instant.now(clock);
        LocalDateTime agoraLocal = LocalDateTime.now(clock);
        LocalDate segundaAtual = agoraLocal.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate primeira = segundaAtual.minusWeeks(quantidade);
        Instant desdeMotivos = agora.minus(JANELA_DOS_MOTIVOS);
        List<ConsultaEmRisco> emRisco = ausencias.proximasConsultas(agoraLocal, agoraLocal.plusDays(DIAS_A_FRENTE), LIMITE_DE_CONSULTAS)
                .stream()
                .map(PrevencaoDeFaltasUseCaseImpl::avaliar)
                .sorted(Comparator.comparingInt((ConsultaEmRisco c) -> c.risco().pontos()).reversed()
                        .thenComparing(ConsultaEmRisco::dataHora))
                .limit(LIMITE_EM_RISCO)
                .toList();

        return new PainelDeFaltas(agora, ausencias.semanas(primeira, quantidade), ausencias.motivos(desdeMotivos),
                ausencias.porEspecialidade(primeira.atStartOfDay(), agoraLocal), emRisco,
                ausencias.perguntasFeitas(desdeMotivos), ausencias.perguntasRespondidas(desdeMotivos));
    }

    @Override
    public void registrarMotivo(Ator ator, UUID agendamentoId, MotivoDeAusencia motivo) {
        Permissoes.exigir(ator, Papel.PACIENTE, Papel.CUIDADOR, Papel.RECEPCAO, Papel.ADMINISTRADOR);

        Agendamento a = agendamentos.buscarAgendamento(agendamentoId)
                .orElseThrow(() -> new NaoEncontradoException("Consulta não encontrada: " + agendamentoId));
        Permissoes.exigirPacienteProprio(ator, a.getPacienteId());

        OcorrenciaDeAusencia ocorrencia = switch (a.getStatus()) {
            case FALTOU -> OcorrenciaDeAusencia.FALTA;
            case CANCELADO, REAGENDADO -> OcorrenciaDeAusencia.CANCELAMENTO;
            default -> throw new IllegalStateException("Só é possível informar o motivo de uma consulta cancelada ou com falta");
        };

        Optional<MotivoRegistrado> anterior = ausencias.buscarMotivo(agendamentoId);
        ausencias.salvarMotivo(new MotivoRegistrado(agendamentoId, a.getPacienteId(), ocorrencia, motivo,
                ator.agePeloPaciente() ? CanalDoMotivo.PORTAL : CanalDoMotivo.EQUIPE,
                anterior.map(MotivoRegistrado::perguntadoEm).orElse(null), Instant.now(clock)));
        auditor.registrar(ator, "REGISTRAR_MOTIVO_DE_AUSENCIA", "CONSULTA", agendamentoId, a.getPacienteId());
    }

    @Override
    public void registrarLigacao(Ator ator, UUID agendamentoId, ResultadoDaLigacao resultado) {
        Permissoes.exigir(ator, Papel.ADMINISTRADOR, Papel.RECEPCAO);

        Agendamento a = agendamentos.buscarAgendamento(agendamentoId)
                .orElseThrow(() -> new NaoEncontradoException("Consulta não encontrada: " + agendamentoId));
        if (!a.getStatus().ocupaAgenda() || a.getStatus() == StatusAgendamento.EM_ANDAMENTO
                || !a.getDataHora().isAfter(LocalDateTime.now(clock)))
            throw new IllegalStateException("Só é possível registrar a ligação de uma consulta que ainda vai acontecer");

        if (resultado == ResultadoDaLigacao.CONFIRMOU && a.getStatus() != StatusAgendamento.CONFIRMADO)
            fila.confirmarPresenca(ator, agendamentoId);
        ausencias.salvarLigacao(new LigacaoRegistrada(UUID.randomUUID(), agendamentoId, resultado, ator.usuarioId(), Instant.now(clock)));
        auditor.registrar(ator, "REGISTRAR_LIGACAO_DE_PREVENCAO", "CONSULTA", agendamentoId, a.getPacienteId());
    }

    private static ConsultaEmRisco avaliar(ConsultaComSinais c) {
        return new ConsultaEmRisco(c.agendamentoId(), c.pacienteId(), c.nomePaciente(), c.telefone(), c.dataHora(), c.especialidade(),
                c.profissional(), c.unidade(), c.status(), RiscoDeFalta.avaliar(c.sinais()), c.ultimaLigacao());
    }
}
