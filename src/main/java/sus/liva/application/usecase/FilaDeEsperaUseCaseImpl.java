package sus.liva.application.usecase;

import sus.liva.application.exception.ConflitoException;
import sus.liva.application.exception.NaoEncontradoException;
import sus.liva.application.port.in.Ator;
import sus.liva.application.port.in.ConsultaSemConfirmacao;
import sus.liva.application.port.in.DetalheDaEntrada;
import sus.liva.application.port.in.FilaDeEsperaUseCase;
import sus.liva.application.port.in.FiltroDaFila;
import sus.liva.application.port.in.MinhaFila;
import sus.liva.application.port.in.PainelDaFila;
import sus.liva.application.port.in.Papel;
import sus.liva.application.port.in.PedidoDePagina;
import sus.liva.application.port.in.PosicaoNaFila;
import sus.liva.application.port.in.ResumoDoCiclo;
import sus.liva.application.port.out.AgendamentoRepositoryPort;
import sus.liva.application.port.out.AuditoriaPort;
import sus.liva.application.port.out.FilaDeEsperaRepositoryPort;
import sus.liva.application.port.out.PacienteRepositoryPort;
import sus.liva.application.port.out.ProfissionalRepositoryPort;
import sus.liva.application.port.out.TransacaoPort;
import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.fila.CanalDeResposta;
import sus.liva.domain.model.fila.DecisaoSemResposta;
import sus.liva.domain.model.fila.EntradaNaFila;
import sus.liva.domain.model.fila.EventoDaFila;
import sus.liva.domain.model.fila.PoliticaDaFila;
import sus.liva.domain.model.profissional.Profissional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class FilaDeEsperaUseCaseImpl implements FilaDeEsperaUseCase {
    private static final System.Logger LOG = System.getLogger(FilaDeEsperaUseCaseImpl.class.getName());
    private static final Duration RECENTES = Duration.ofDays(30);
    private static final int ITENS_DO_PAINEL = 20;

    private final FilaDeEsperaRepositoryPort fila;
    private final AgendamentoRepositoryPort agendamentos;
    private final PacienteRepositoryPort pacientes;
    private final ProfissionalRepositoryPort profissionais;
    private final ReservaDeVaga reserva;
    private final ProcessarFilaUseCaseImpl processamento;
    private final TransacaoPort transacao;
    private final PoliticaDaFila politica;
    private final Auditor auditor;
    private final DiarioDaFila diario;
    private final Clock clock;

    public FilaDeEsperaUseCaseImpl(FilaDeEsperaRepositoryPort fila, AgendamentoRepositoryPort agendamentos,
                                   PacienteRepositoryPort pacientes, ProfissionalRepositoryPort profissionais, ReservaDeVaga reserva,
                                   ProcessarFilaUseCaseImpl processamento, TransacaoPort transacao, PoliticaDaFila politica,
                                   AuditoriaPort auditoria, Clock clock) {
        this.fila = fila;
        this.agendamentos = agendamentos;
        this.pacientes = pacientes;
        this.profissionais = profissionais;
        this.reserva = reserva;
        this.processamento = processamento;
        this.transacao = transacao;
        this.politica = politica;
        this.auditor = new Auditor(auditoria, clock);
        this.diario = new DiarioDaFila(fila, clock);
        this.clock = clock;
    }

    @Override
    public MinhaFila minhaFila(Ator ator) {
        Permissoes.exigir(ator, Papel.PACIENTE, Papel.CUIDADOR);

        processamento.sincronizar(ator.pacienteId());
        Instant agora = Instant.now(clock);
        return new MinhaFila(agora, fila.posicoesDoPaciente(ator.pacienteId(), agora.minus(RECENTES)),
                fila.ofertasAtivasDoPaciente(ator.pacienteId()).stream().filter(o -> o.expiraEm().isAfter(agora)).toList());
    }

    @Override
    public Agendamento aceitar(Ator ator, UUID ofertaId) {
        Permissoes.exigir(ator, Papel.PACIENTE, Papel.CUIDADOR, Papel.RECEPCAO, Papel.ADMINISTRADOR);

        ReservaDeVaga.Resultado r = reserva.aceitar(ofertaId, ator, CanalDeResposta.PORTAL);
        auditor.registrar(ator, r.garantiuAVaga() ? "ACEITAR_OFERTA_DE_VAGA" : "TENTAR_ACEITAR_OFERTA_DE_VAGA", "OFERTA_DE_VAGA",
                ofertaId, r.oferta().getPacienteId());

        if (!r.garantiuAVaga()) {
            ofertarEmSeguida();
            throw new ConflitoException(r.motivo());
        }

        ofertarEmSeguida();
        return r.nova();
    }

    @Override
    public void recusar(Ator ator, UUID ofertaId) {
        Permissoes.exigir(ator, Papel.PACIENTE, Papel.CUIDADOR, Papel.RECEPCAO, Papel.ADMINISTRADOR);

        ReservaDeVaga.Resultado r = reserva.recusar(ofertaId, ator, CanalDeResposta.PORTAL);
        auditor.registrar(ator, "RECUSAR_OFERTA_DE_VAGA", "OFERTA_DE_VAGA", ofertaId, r.oferta().getPacienteId());
        ofertarEmSeguida();

        if (r.desfecho() != ReservaDeVaga.Desfecho.RECUSADA) throw new ConflitoException(r.motivo());
    }

    @Override
    public PainelDaFila painel(Ator ator, FiltroDaFila filtro, PedidoDePagina pagina) {
        Permissoes.exigir(ator, Papel.ADMINISTRADOR, Papel.RECEPCAO, Papel.ESPECIALISTA);

        FiltroDaFila efetivo = filtroDoAtor(ator, filtro);
        atualizarSemEnvios();

        Instant agora = Instant.now(clock);
        LocalDateTime agoraLocal = LocalDateTime.now(clock);
        List<ConsultaSemConfirmacao> semConfirmacao = fila.semConfirmacao(efetivo, agoraLocal, ITENS_DO_PAINEL).stream()
                .map(c -> new ConsultaSemConfirmacao(c.consultaId(), c.pacienteId(), c.paciente(), c.nomeEspecialidade(), c.profissional(),
                        c.unidade(), c.dataHora(), politica.confirmarAte(c.dataHora()), c.status(), c.decisao(), c.pedidoEnviado(),
                        c.pedidoEntregue()))
                .toList();

        return new PainelDaFila(agora, efetivo, ator.eh(Papel.ADMINISTRADOR, Papel.RECEPCAO),
                fila.indicadores(efetivo, agora.minus(RECENTES), agoraLocal.minus(RECENTES), agoraLocal),
                fila.fila(efetivo, pagina), fila.vagas(efetivo, ITENS_DO_PAINEL), fila.ofertas(efetivo, null, ITENS_DO_PAINEL),
                semConfirmacao, fila.movimentacoes(efetivo, null, ITENS_DO_PAINEL));
    }

    @Override
    public DetalheDaEntrada detalhar(Ator ator, UUID entradaId) {
        Permissoes.exigir(ator, Papel.ADMINISTRADOR, Papel.RECEPCAO, Papel.ESPECIALISTA);

        EntradaNaFila entrada = entrada(entradaId);
        if (ator.eh(Papel.ESPECIALISTA)) {
            Profissional p = profissionalDoAtor(ator);
            if (!entrada.disputa(p.id(), p.especialidade())) throw Permissoes.negado();
        }

        PosicaoNaFila posicao = fila.posicao(entradaId).orElseThrow();
        String nome = pacientes.buscarPaciente(entrada.getPacienteId()).map(p -> p.nomeSocial() != null && !p.nomeSocial().isBlank()
                ? p.nomeSocial() : p.nomeCompleto()).orElse("");

        auditor.registrar(ator, "CONSULTAR_FILA_DO_PACIENTE", "ENTRADA_FILA", entradaId, entrada.getPacienteId());
        return new DetalheDaEntrada(entrada.getPacienteId(), nome, posicao, fila.ofertas(FiltroDaFila.todas(), entradaId, 50),
                fila.movimentacoes(FiltroDaFila.todas(), entradaId, 100));
    }

    @Override
    public void pausar(Ator ator, UUID entradaId, String motivo) {
        Permissoes.exigir(ator, Papel.ADMINISTRADOR, Papel.RECEPCAO);

        EntradaNaFila e = transacao.executar(() -> {
            EntradaNaFila atual = fila.entradaParaAtualizar(entradaId)
                    .orElseThrow(() -> new NaoEncontradoException("Entrada na fila não encontrada: " + entradaId));
            atual.pausar(motivo, Instant.now(clock));
            fila.salvarEntrada(atual);
            diario.daEntrada(EventoDaFila.PAUSADA, atual, atual.getMotivo(), ator);
            return atual;
        });

        auditor.registrar(ator, "PAUSAR_ENTRADA_NA_FILA", "ENTRADA_FILA", entradaId, e.getPacienteId());
    }

    @Override
    public void retomar(Ator ator, UUID entradaId) {
        Permissoes.exigir(ator, Papel.ADMINISTRADOR, Papel.RECEPCAO);

        EntradaNaFila e = transacao.executar(() -> {
            EntradaNaFila atual = fila.entradaParaAtualizar(entradaId)
                    .orElseThrow(() -> new NaoEncontradoException("Entrada na fila não encontrada: " + entradaId));
            atual.retomar(Instant.now(clock));
            fila.salvarEntrada(atual);
            diario.daEntrada(EventoDaFila.RETOMADA, atual, "Retomada pela equipe com a data de entrada original", ator);
            return atual;
        });

        auditor.registrar(ator, "RETOMAR_ENTRADA_NA_FILA", "ENTRADA_FILA", entradaId, e.getPacienteId());
        ofertarEmSeguida();
    }

    @Override
    public Agendamento confirmarPresenca(Ator ator, UUID consultaId) {
        Permissoes.exigir(ator, Papel.ADMINISTRADOR, Papel.RECEPCAO);

        Agendamento confirmada = transacao.executar(() -> {
            Agendamento a = agendamentos.buscarAgendamentoParaAtualizar(consultaId)
                    .orElseThrow(() -> new NaoEncontradoException("Consulta não encontrada: " + consultaId));
            a.confirmarPresenca();
            agendamentos.salvarAgendamento(a);
            fila.substituirDecisao(consultaId, DecisaoSemResposta.CONFIRMADA_NA_RECEPCAO, Instant.now(clock), ator.usuarioId(),
                    ator.papel().name());
            diario.daConsulta(EventoDaFila.PRESENCA_CONFIRMADA_NA_RECEPCAO, consultaId, a.getPacienteId(),
                    "Presença confirmada pela equipe em contato com a pessoa", ator);
            return a;
        });

        auditor.registrar(ator, "CONFIRMAR_PRESENCA_NA_RECEPCAO", "CONSULTA", consultaId, confirmada.getPacienteId());
        return agendamentos.buscarAgendamento(consultaId).orElseThrow();
    }

    @Override
    public ResumoDoCiclo processarAgora(Ator ator) {
        Permissoes.exigir(ator, Papel.ADMINISTRADOR, Papel.RECEPCAO);
        return processamento.executarCiclo();
    }

    private FiltroDaFila filtroDoAtor(Ator ator, FiltroDaFila filtro) {
        if (!ator.eh(Papel.ESPECIALISTA)) return filtro == null ? FiltroDaFila.todas() : filtro;
        Profissional p = profissionalDoAtor(ator);
        return new FiltroDaFila(p.especialidade(), p.id(), null);
    }

    private Profissional profissionalDoAtor(Ator ator) {
        return profissionais.buscarProfissional(Permissoes.profissional(ator)).orElseThrow(Permissoes::negado);
    }

    private void atualizarSemEnvios() {
        try {
            processamento.atualizarSemEnvios();
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.WARNING, "Não foi possível atualizar a fila antes de montar o painel", e);
        }
    }

    private EntradaNaFila entrada(UUID id) {
        return fila.buscarEntrada(id).orElseThrow(() -> new NaoEncontradoException("Entrada na fila não encontrada: " + id));
    }

    private void ofertarEmSeguida() {
        processamento.ofertarEmSeguida();
    }
}
