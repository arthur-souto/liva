package sus.liva.application.usecase;

import sus.liva.application.port.in.ResultadoResposta;
import sus.liva.application.port.out.AusenciaRepositoryPort;
import sus.liva.application.port.out.PacienteRepositoryPort;
import sus.liva.application.port.out.RegistroMensagemPort;
import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.ausencia.CanalDoMotivo;
import sus.liva.domain.model.ausencia.MotivoDeAusencia;
import sus.liva.domain.model.ausencia.MotivoRegistrado;
import sus.liva.domain.model.ausencia.OcorrenciaDeAusencia;
import sus.liva.domain.model.paciente.Paciente;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class PerguntaDeMotivo {
    private static final System.Logger LOG = System.getLogger(PerguntaDeMotivo.class.getName());
    private static final Duration PRAZO_DA_RESPOSTA = Duration.ofDays(3);
    private static final Duration INTERVALO_ENTRE_ORIENTACOES = Duration.ofMinutes(5);

    private final AusenciaRepositoryPort ausencias;
    private final PacienteRepositoryPort pacientes;
    private final NotificacaoAoPaciente notificacao;
    private final ModeloMensagens modelos;
    private final LinksDeAcesso links;
    private final RegistroMensagemPort registros;
    private final Clock clock;

    public PerguntaDeMotivo(AusenciaRepositoryPort ausencias, PacienteRepositoryPort pacientes, NotificacaoAoPaciente notificacao,
                            ModeloMensagens modelos, LinksDeAcesso links, RegistroMensagemPort registros, Clock clock) {
        this.ausencias = ausencias;
        this.pacientes = pacientes;
        this.notificacao = notificacao;
        this.modelos = modelos;
        this.links = links;
        this.registros = registros;
        this.clock = clock;
    }

    public void aposFalta(Agendamento a) {
        try {
            if (ausencias.buscarMotivo(a.getId()).isPresent()) return;
            String link = a.doEncaminhamento() && links != null
                    ? links.gerar(a.getPacienteId(), "/portal/agendar/" + a.getEncaminhamentoId()) : null;
            long minutos = LinksDeAcesso.VALIDADE.toMinutes();
            Function<Paciente, String> texto = p -> modelos.perguntaAposFalta(p, a, link, minutos);
            boolean enviada = notificacao.notificar(a.getId(), a.getPacienteId(), texto, texto);
            ausencias.salvarMotivo(new MotivoRegistrado(a.getId(), a.getPacienteId(), OcorrenciaDeAusencia.FALTA, null, null,
                    enviada ? Instant.now(clock) : null, null));
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.WARNING, "Falha ao perguntar o motivo da falta da consulta " + a.getId(), e);
        }
    }

    public void aposCancelamento(Agendamento a, Telefone destino) {
        try {
            if (ausencias.buscarMotivo(a.getId()).isPresent()) return;
            Optional<Paciente> paciente = pacientes.buscarPaciente(a.getPacienteId());
            if (paciente.isEmpty()) return;
            boolean enviada = notificacao.enviar(a.getId(), destino, modelos.perguntaAposCancelamento(paciente.get()));
            ausencias.salvarMotivo(new MotivoRegistrado(a.getId(), a.getPacienteId(), OcorrenciaDeAusencia.CANCELAMENTO, null, null,
                    enviada ? Instant.now(clock) : null, null));
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.WARNING, "Falha ao perguntar o motivo do cancelamento da consulta " + a.getId(), e);
        }
    }

    public Optional<ResultadoResposta> responder(Telefone remetente, String texto, List<Paciente> doTelefone, boolean pediuMenu) {
        if (doTelefone.isEmpty() || pediuMenu) return Optional.empty();
        Instant agora = Instant.now(clock);
        Optional<MotivoRegistrado> pendente = ausencias.perguntaPendente(doTelefone.stream().map(Paciente::id).toList(),
                agora.minus(PRAZO_DA_RESPOSTA));
        if (pendente.isEmpty()) return Optional.empty();
        MotivoRegistrado p = pendente.get();
        Optional<MotivoDeAusencia> motivo = MotivoDeAusencia.interpretar(texto);
        if (motivo.isEmpty()) {
            String orientacao = modelos.naoEntendiMotivo();
            boolean enviada = !registros.enviadaDesde(remetente, orientacao, agora.minus(INTERVALO_ENTRE_ORIENTACOES))
                    && notificacao.enviar(p.agendamentoId(), remetente, orientacao);
            return Optional.of(new ResultadoResposta(false, p.agendamentoId(), null, enviada, "Motivo da ausência não reconhecido"));
        }
        ausencias.salvarMotivo(new MotivoRegistrado(p.agendamentoId(), p.pacienteId(), p.ocorrencia(), motivo.get(),
                CanalDoMotivo.WHATSAPP, p.perguntadoEm(), agora));
        boolean enviada = doTelefone.stream().filter(x -> x.id().equals(p.pacienteId())).findFirst()
                .map(x -> notificacao.enviar(p.agendamentoId(), remetente, modelos.motivoRegistrado(x, motivo.get())))
                .orElse(false);
        return Optional.of(new ResultadoResposta(false, p.agendamentoId(), null, enviada, "Motivo da ausência registrado"));
    }
}
