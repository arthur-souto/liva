package sus.liva.application.usecase;

import sus.liva.application.port.in.ResultadoResposta;
import sus.liva.application.port.out.FilaDeEsperaRepositoryPort;
import sus.liva.application.port.out.RegistroMensagemPort;
import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.fila.CanalDeResposta;
import sus.liva.domain.model.fila.OfertaDeVaga;
import sus.liva.domain.model.fila.RespostaAOferta;
import sus.liva.domain.model.paciente.Paciente;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RespostaDeOfertaPeloWhatsApp {
    private static final Duration OFERTAS_RECENTES = Duration.ofDays(2);
    private static final Duration INTERVALO_ENTRE_AVISOS = Duration.ofMinutes(5);

    private final FilaDeEsperaRepositoryPort fila;
    private final AssistenteWhatsApp assistente;
    private final ReservaDeVaga reserva;
    private final ProcessarFilaUseCaseImpl processamento;
    private final NotificacaoAoPaciente notificacao;
    private final RegistroMensagemPort registros;
    private final ModeloMensagens modelos;
    private final Clock clock;

    public RespostaDeOfertaPeloWhatsApp(FilaDeEsperaRepositoryPort fila, AssistenteWhatsApp assistente, ReservaDeVaga reserva,
                                        ProcessarFilaUseCaseImpl processamento, NotificacaoAoPaciente notificacao,
                                        RegistroMensagemPort registros, ModeloMensagens modelos, Clock clock) {
        this.fila = fila;
        this.assistente = assistente;
        this.reserva = reserva;
        this.processamento = processamento;
        this.notificacao = notificacao;
        this.registros = registros;
        this.modelos = modelos;
        this.clock = clock;
    }

    public Optional<ResultadoResposta> responder(Telefone remetente, String texto) {
        Optional<RespostaAOferta> resposta = RespostaAOferta.interpretar(texto);
        if (resposta.isEmpty()) return Optional.empty();

        Map<UUID, Paciente> doTelefone = assistente.pacientesDoTelefone(remetente).stream()
                .collect(Collectors.toMap(Paciente::id, Function.identity(), (a, b) -> a));
        if (doTelefone.isEmpty()) return Optional.empty();

        Instant agora = Instant.now(clock);
        List<OfertaDeVaga> ativas = fila.ofertasAtivasDosPacientes(doTelefone.keySet()).stream().filter(o -> !o.vencida(agora)).toList();

        if (ativas.isEmpty()) {
            Optional<OfertaDeVaga> recente = fila.ultimaOfertaDosPacientes(doTelefone.keySet(), agora.minus(OFERTAS_RECENTES));
            if (recente.isEmpty()) return Optional.empty();

            Paciente p = doTelefone.get(recente.get().getPacienteId());
            if (recente.get().getConsultaNovaId() != null)
                return Optional.of(ResultadoResposta.ignorada("A vaga já foi garantida anteriormente"));

            avisar(remetente, modelos.ofertaIndisponivel(p));
            return Optional.of(ResultadoResposta.ignorada("Oferta de vaga encerrada"));
        }

        OfertaDeVaga oferta = ativas.getFirst();
        Paciente paciente = doTelefone.get(oferta.getPacienteId());

        if (resposta.get() == RespostaAOferta.RECUSAR) {
            ReservaDeVaga.Resultado r = reserva.recusar(oferta.getId(), assistente.ator(remetente, paciente), CanalDeResposta.WHATSAPP);
            avisar(remetente, r.desfecho() == ReservaDeVaga.Desfecho.RECUSADA ? modelos.ofertaRecusada(paciente)
                    : modelos.ofertaIndisponivel(paciente));
            ofertarEmSeguida();
            return Optional.of(ResultadoResposta.ignorada("Oferta de vaga recusada pelo WhatsApp"));
        }

        ReservaDeVaga.Resultado r = reserva.aceitar(oferta.getId(), assistente.ator(remetente, paciente), CanalDeResposta.WHATSAPP);
        if (r.garantiuAVaga()) return Optional.of(ResultadoResposta.aplicada(r.nova().getId(), r.nova().getStatus(), true));

        avisar(remetente, modelos.ofertaIndisponivel(paciente));
        ofertarEmSeguida();
        return Optional.of(ResultadoResposta.ignorada(r.motivo()));
    }

    private void avisar(Telefone destino, String texto) {
        if (!registros.enviadaDesde(destino, texto, Instant.now(clock).minus(INTERVALO_ENTRE_AVISOS)))
            notificacao.enviar(null, destino, texto);
    }

    private void ofertarEmSeguida() {
        processamento.ofertarEmSeguida();
    }
}
