package sus.liva.application.usecase;

import sus.liva.application.exception.ConflitoException;
import sus.liva.application.port.in.DispararLembreteUseCase;
import sus.liva.application.port.in.ProcessarFilaUseCase;
import sus.liva.application.port.in.ResumoDoCiclo;
import sus.liva.application.port.out.AgendamentoRepositoryPort;
import sus.liva.application.port.out.EncaminhamentoRepositoryPort;
import sus.liva.application.port.out.FilaDeEsperaRepositoryPort;
import sus.liva.application.port.out.ProfissionalRepositoryPort;
import sus.liva.application.port.out.TransacaoPort;
import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.agendamento.TipoLembrete;
import sus.liva.domain.model.fila.DecisaoSemResposta;
import sus.liva.domain.model.fila.EntradaNaFila;
import sus.liva.domain.model.fila.EventoDaFila;
import sus.liva.domain.model.fila.OfertaDeVaga;
import sus.liva.domain.model.fila.PoliticaDaFila;
import sus.liva.domain.model.fila.VagaLiberada;
import sus.liva.domain.model.profissional.Especialidade;
import sus.liva.domain.model.profissional.Profissional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.IntSupplier;

public class ProcessarFilaUseCaseImpl implements ProcessarFilaUseCase {
    private static final System.Logger LOG = System.getLogger(ProcessarFilaUseCaseImpl.class.getName());
    private static final int LOTE = 50;
    private static final Duration REENVIO = Duration.ofMinutes(10);
    private static final Duration ESPERA_PARA_RECUPERAR_ENVIO = Duration.ofMinutes(1);
    private static final String DESTINO_NO_PORTAL = "/portal/fila";

    private sealed interface Passo {
        record Fim() implements Passo {}
        record Seguir() implements Passo {}
        record Ofertou(OfertaDeVaga oferta) implements Passo {}
    }

    private final FilaDeEsperaRepositoryPort fila;
    private final AgendamentoRepositoryPort agendamentos;
    private final EncaminhamentoRepositoryPort encaminhamentos;
    private final ProfissionalRepositoryPort profissionais;
    private final DispararLembreteUseCase dispararLembrete;
    private final NotificacaoAoPaciente notificacao;
    private final LinksDeAcesso links;
    private final ModeloMensagens modelos;
    private final TransacaoPort transacao;
    private final PoliticaDaFila politica;
    private final DiarioDaFila diario;
    private final Clock clock;
    private final String urlDoPortal;
    private final boolean ofertaImediata;

    public ProcessarFilaUseCaseImpl(FilaDeEsperaRepositoryPort fila, AgendamentoRepositoryPort agendamentos,
                                    EncaminhamentoRepositoryPort encaminhamentos, ProfissionalRepositoryPort profissionais,
                                    DispararLembreteUseCase dispararLembrete, NotificacaoAoPaciente notificacao, LinksDeAcesso links,
                                    ModeloMensagens modelos, TransacaoPort transacao, PoliticaDaFila politica, Clock clock,
                                    String urlDoPortal) {
        this(fila, agendamentos, encaminhamentos, profissionais, dispararLembrete, notificacao, links, modelos, transacao, politica, clock,
                urlDoPortal, true);
    }

    public ProcessarFilaUseCaseImpl(FilaDeEsperaRepositoryPort fila, AgendamentoRepositoryPort agendamentos,
                                    EncaminhamentoRepositoryPort encaminhamentos, ProfissionalRepositoryPort profissionais,
                                    DispararLembreteUseCase dispararLembrete, NotificacaoAoPaciente notificacao, LinksDeAcesso links,
                                    ModeloMensagens modelos, TransacaoPort transacao, PoliticaDaFila politica, Clock clock,
                                    String urlDoPortal, boolean ofertaImediata) {
        this.ofertaImediata = ofertaImediata;
        this.fila = fila;
        this.agendamentos = agendamentos;
        this.encaminhamentos = encaminhamentos;
        this.profissionais = profissionais;
        this.dispararLembrete = dispararLembrete;
        this.notificacao = notificacao;
        this.links = links;
        this.modelos = modelos;
        this.transacao = transacao;
        this.politica = politica;
        this.diario = new DiarioDaFila(fila, clock);
        this.clock = clock;
        this.urlDoPortal = urlDoPortal.endsWith("/") ? urlDoPortal.substring(0, urlDoPortal.length() - 1) : urlDoPortal;
    }

    @Override
    public ResumoDoCiclo executarCiclo() {
        int entradas = etapa("sincronizar entradas", () -> sincronizar(null));
        int vagas = etapa("registrar vagas", this::registrarVagas);
        int expiradas = etapa("expirar ofertas", this::expirarOfertas);
        int avisos = etapa("enviar avisos finais", this::enviarAvisosFinais);
        int prazos = etapa("tratar prazos de confirmação", this::tratarPrazosVencidos);

        vagas += etapa("registrar vagas", this::registrarVagas);
        int ofertas = etapa("ofertar vagas", this::ofertarVagas);
        etapa("reenviar ofertas", this::reenviarOfertas);
        return new ResumoDoCiclo(entradas, vagas, ofertas, expiradas, avisos, prazos);
    }

    public int sincronizar(UUID pacienteId) {
        return transacao.executar(() -> {
            Instant agora = Instant.now(clock);
            List<EntradaNaFila> novas = fila.incluirEntradasPendentes(pacienteId, agora);

            novas.forEach(e -> diario.daEntrada(EventoDaFila.ENTROU_NA_FILA, e,
                    "Encaminhamento liberado para agendamento (" + e.getNatureza().name().toLowerCase() + ")", null));
            fila.reativarEntradas(pacienteId, agora).forEach(e -> diario.daEntrada(EventoDaFila.RETOMADA, e,
                    "A consulta foi desmarcada; volta para a fila com a data de entrada original", null));
            fila.encerrarEntradasSemNecessidade(pacienteId, agora).forEach(e ->
                    diario.daEntrada(EventoDaFila.SAIU_DA_FILA, e, e.getMotivo(), null));
            return novas.size();
        });
    }

    public int atualizarSemEnvios() {
        return sincronizar(null) + registrarVagas() + expirarOfertas();
    }

    public int ofertarEmSeguida() {
        if (!ofertaImediata) return 0;
        try {
            return ofertarVagas();
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.WARNING, "Não foi possível oferecer as vagas em seguida; o ciclo automático continua", e);
            return 0;
        }
    }

    public int ofertarVagas() {
        int ofertadas = 0;
        for (int i = 0; i < LOTE; i++) {
            Passo passo;
            try {
                passo = transacao.executar(this::ofertarProximaVaga);
            } catch (ConflitoException e) {
                LOG.log(System.Logger.Level.INFO, "Oferta concorrente descartada: " + e.getMessage());
                continue;
            }

            if (passo instanceof Passo.Fim) break;

            if (passo instanceof Passo.Ofertou(OfertaDeVaga oferta)) {
                ofertadas++;
                enviar(oferta);
            }
        }

        return ofertadas;
    }

    private int registrarVagas() {
        return transacao.executar(() -> {
            List<VagaLiberada> vagas = fila.registrarVagasLiberadas(LocalDateTime.now(clock).plus(politica.antecedenciaMinima()),
                    Instant.now(clock));
            vagas.forEach(v -> diario.daVaga(EventoDaFila.VAGA_LIBERADA, v, switch (v.getOrigem()) {
                case CANCELAMENTO -> "Consulta cancelada ou remarcada";
                case SEM_CONFIRMACAO -> "Consulta liberada por falta de confirmação";
                case ANTECIPACAO -> "Horário liberado por uma antecipação aceita";
            }));

            return vagas.size();
        });
    }

    private int expirarOfertas() {
        return transacao.executar(() -> {
            Instant agora = Instant.now(clock);
            List<OfertaDeVaga> vencidas = fila.ofertasVencidasParaAtualizar(agora, LOTE);

            for (OfertaDeVaga o : vencidas) {
                o.expirar(agora);
                fila.salvarOferta(o);
                diario.daOferta(EventoDaFila.OFERTA_EXPIRADA, o, null,
                        "Sem resposta no prazo. A pessoa continua na fila na mesma posição.", null);
            }

            return vencidas.size();
        });
    }

    private int enviarAvisosFinais() {
        LocalDateTime agora = LocalDateTime.now(clock);
        LocalDateTime de = agora.plus(politica.prazoDeConfirmacao());
        int enviados = 0;

        for (UUID id : fila.consultasParaAvisoFinal(de, de.plus(politica.avisoFinal()), LOTE)) {
            Instant tentativa = Instant.now(clock);
            if (!fila.reivindicar("AVISO_FINAL:" + id + ":" + tentativa.getEpochSecond() / REENVIO.toSeconds(), tentativa)) continue;

            try {
                enviados += dispararLembrete.disparar(id, TipoLembrete.AVISO_FINAL_CONFIRMACAO, false).envios().stream()
                        .anyMatch(r -> r.sucesso()) ? 1 : 0;
            } catch (RuntimeException e) {
                LOG.log(System.Logger.Level.WARNING, "Falha ao enviar o aviso final da consulta " + id, e);
            }
        }

        return enviados;
    }

    private int tratarPrazosVencidos() {
        LocalDateTime agora = LocalDateTime.now(clock);
        int tratados = 0;

        for (UUID id : fila.consultasComPrazoVencido(agora, agora.plus(politica.prazoDeConfirmacao()), LOTE)) {
            boolean entregue = fila.pedidoDeConfirmacaoEntregue(id, politica.exigirEntrega());
            if (politica.liberaSemResposta(entregue)) {
                Optional<Agendamento> liberada = transacao.executar(() -> liberar(id));
                liberada.ifPresent(a -> notificacao.notificar(a.getId(), a.getPacienteId(),
                        p -> modelos.consultaLiberadaSemConfirmacao(p, a)));
                tratados += liberada.isPresent() ? 1 : 0;
            } else {
                DecisaoSemResposta decisao = entregue ? DecisaoSemResposta.MANTIDA_SEM_RESPOSTA : DecisaoSemResposta.SEM_CONTATO;
                tratados += transacao.executar(() -> manter(id, decisao)) ? 1 : 0;
            }
        }

        return tratados;
    }

    private boolean manter(UUID id, DecisaoSemResposta decisao) {
        if (!fila.registrarDecisao(id, decisao, Instant.now(clock), null, null)) return false;

        Agendamento a = agendamentos.buscarAgendamento(id).orElseThrow();
        if (decisao == DecisaoSemResposta.SEM_CONTATO)
            diario.daConsulta(EventoDaFila.SEM_CONTATO, id, a.getPacienteId(),
                    "Prazo de confirmação vencido sem entrega confirmada do WhatsApp. A consulta foi mantida para contato da recepção.", null);
        else
            diario.daConsulta(EventoDaFila.PRAZO_SEM_RESPOSTA, id, a.getPacienteId(),
                    "Prazo de confirmação vencido sem resposta. A política da unidade mantém a consulta.", null);

        return true;
    }

    private Optional<Agendamento> liberar(UUID id) {
        Agendamento a = agendamentos.buscarAgendamentoParaAtualizar(id).orElseThrow();
        if (!a.aguardandoResposta() || !a.getDataHora().isAfter(LocalDateTime.now(clock))) return Optional.empty();
        if (!fila.registrarDecisao(id, DecisaoSemResposta.LIBERADA, Instant.now(clock), null, null)) return Optional.empty();

        a.cancelar();
        agendamentos.salvarAgendamento(a);

        if (a.doEncaminhamento())
            encaminhamentos.buscarEncaminhamentoParaAtualizar(a.getEncaminhamentoId()).ifPresent(e -> {
                e.consultaDesmarcada();
                encaminhamentos.salvarEncaminhamento(e);
            });

        diario.daConsulta(EventoDaFila.CONSULTA_LIBERADA, id, a.getPacienteId(),
                "Prazo de confirmação vencido com o pedido entregue e sem resposta. O encaminhamento segue válido e a pessoa continua na fila.",
                null);
        return Optional.of(a);
    }

    private Passo ofertarProximaVaga() {
        Instant agora = Instant.now(clock);
        Optional<VagaLiberada> encontrada = fila.proximaVagaParaOfertar(agora);
        if (encontrada.isEmpty()) return new Passo.Fim();

        VagaLiberada v = encontrada.get();
        LocalDateTime agoraLocal = LocalDateTime.now(clock);

        if (!politica.aindaDaTempoDeOfertar(v.getInicio(), agoraLocal))
            return encerrar(v, "Horário próximo demais para ser oferecido", agora);

        if (fila.horarioOcupado(v.getProfissionalId(), v.getInicio(), v.getFim())) {
            v.indisponivel("O horário foi ocupado na agenda", agora);
            fila.salvarVaga(v);
            diario.daVaga(EventoDaFila.VAGA_INDISPONIVEL, v, v.getMotivo());
            return new Passo.Seguir();
        }

        boolean naAgenda = profissionais.buscarProfissional(v.getProfissionalId())
                .filter(p -> p.atende(v.getEspecialidade()))
                .map(p -> profissionais.disponibilidadesDe(p.id()).stream().anyMatch(d -> d.comporta(v.getInicio())))
                .orElse(false);
        if (!naAgenda) {
            v.indisponivel("O horário saiu da agenda do profissional", agora);
            fila.salvarVaga(v);
            diario.daVaga(EventoDaFila.VAGA_INDISPONIVEL, v, v.getMotivo());
            return new Passo.Seguir();
        }

        if (fila.ofertasDaVaga(v.getId()) >= politica.ofertasPorVaga())
            return encerrar(v, "Limite de " + politica.ofertasPorVaga() + " ofertas atingido", agora);

        Optional<EntradaNaFila> proxima = fila.proximaElegivel(v);
        if (proxima.isEmpty()) {
            v.tentarDepois(agora.plus(politica.esperaSemCandidato()));
            fila.salvarVaga(v);
            return new Passo.Seguir();
        }

        EntradaNaFila e = proxima.get();
        OfertaDeVaga oferta = OfertaDeVaga.nova(UUID.randomUUID(), v, e, fila.consultaQueOcupaAgenda(e.getEncaminhamentoId()).orElse(null),
                agora, politica.expiracaoDaOferta(agora, v.getInicio(), clock.getZone()));

        fila.salvarOferta(oferta);
        diario.daOferta(EventoDaFila.OFERTA_CRIADA, oferta, null, "Próxima pessoa elegível pela prioridade e pela data de entrada", null);
        return new Passo.Ofertou(oferta);
    }

    private Passo encerrar(VagaLiberada v, String motivo, Instant agora) {
        v.encerrar(motivo, agora);
        fila.salvarVaga(v);
        diario.daVaga(EventoDaFila.VAGA_ENCERRADA, v, motivo);
        return new Passo.Seguir();
    }

    private int reenviarOfertas() {
        Instant agora = Instant.now(clock);
        List<OfertaDeVaga> pendentes = fila.ofertasSemEnvio(agora, agora.minus(ESPERA_PARA_RECUPERAR_ENVIO), agora.minus(REENVIO), LOTE);

        pendentes.forEach(this::enviar);
        return pendentes.size();
    }

    private void enviar(OfertaDeVaga oferta) {
        Instant agora = Instant.now(clock);
        if (!fila.reivindicarEnvio(oferta.getId(), agora, agora.minus(REENVIO))) return;

        try {
            VagaLiberada v = fila.buscarVaga(oferta.getVagaId()).orElseThrow();
            Profissional profissional = profissionais.buscarProfissional(v.getProfissionalId()).orElseThrow();
            String especialidade = profissionais.buscarEspecialidade(v.getEspecialidade()).map(Especialidade::nome).orElse(v.getEspecialidade());
            LocalDateTime antecipaDe = Optional.ofNullable(oferta.getConsultaAnteriorId())
                    .flatMap(agendamentos::buscarAgendamento).map(Agendamento::getDataHora).orElse(null);
            LocalDateTime respondaAte = LocalDateTime.ofInstant(oferta.getExpiraEm(), clock.getZone());
            boolean enviada = notificacao.notificar(oferta.getConsultaAnteriorId(), oferta.getPacienteId(),
                    p -> modelos.ofertaDeVaga(p, especialidade, profissional.nome(), v.getUnidade(), v.getEndereco(), v.getInicio(),
                            respondaAte, antecipaDe, links.gerar(p.id(), DESTINO_NO_PORTAL)),
                    p -> modelos.ofertaDeVaga(p, especialidade, profissional.nome(), v.getUnidade(), v.getEndereco(), v.getInicio(),
                            respondaAte, antecipaDe, urlDoPortal + DESTINO_NO_PORTAL));

            transacao.executar(() -> {
                fila.ofertaParaAtualizar(oferta.getId()).filter(OfertaDeVaga::ativa).ifPresent(o -> {
                    if (enviada) {
                        o.enviada(null, Instant.now(clock));
                        fila.salvarOferta(o);
                        diario.daOferta(EventoDaFila.OFERTA_ENVIADA, o, null, "Oferta enviada pelo WhatsApp", null);
                    } else {
                        diario.daOferta(EventoDaFila.OFERTA_NAO_ENVIADA, o, null,
                                "WhatsApp indisponível ou sem consentimento. A oferta segue visível no portal e o envio será tentado de novo.",
                                null);
                    }
                });
                return null;
            });
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.WARNING, "Falha ao enviar a oferta " + oferta.getId(), e);
        }
    }

    private int etapa(String nome, IntSupplier execucao) {
        try {
            return execucao.getAsInt();
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.WARNING, "Fila de espera: falha ao " + nome, e);
            return 0;
        }
    }
}
