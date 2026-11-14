package sus.liva.application.usecase;

import sus.liva.application.exception.AcessoNegadoException;
import sus.liva.application.exception.ConflitoException;
import sus.liva.application.exception.NaoEncontradoException;
import sus.liva.application.port.in.Ator;
import sus.liva.application.port.out.AgendamentoRepositoryPort;
import sus.liva.application.port.out.EncaminhamentoRepositoryPort;
import sus.liva.application.port.out.FilaDeEsperaRepositoryPort;
import sus.liva.application.port.out.ProfissionalRepositoryPort;
import sus.liva.application.port.out.TransacaoPort;
import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.agendamento.StatusAgendamento;
import sus.liva.domain.model.encaminhamento.Encaminhamento;
import sus.liva.domain.model.fila.CanalDeResposta;
import sus.liva.domain.model.fila.EntradaNaFila;
import sus.liva.domain.model.fila.EventoDaFila;
import sus.liva.domain.model.fila.OfertaDeVaga;
import sus.liva.domain.model.fila.StatusOferta;
import sus.liva.domain.model.fila.VagaLiberada;
import sus.liva.domain.model.profissional.Profissional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class ReservaDeVaga {
    public enum Desfecho { ACEITA, JA_ACEITA, RECUSADA, INDISPONIVEL, EXPIRADA }

    public record Resultado(Desfecho desfecho, OfertaDeVaga oferta, Agendamento nova, LocalDateTime anterior, String motivo) {
        public boolean garantiuAVaga() {
            return desfecho == Desfecho.ACEITA || desfecho == Desfecho.JA_ACEITA;
        }
    }

    private static final String INDISPONIVEL = "Esta vaga não está mais disponível. Você continua na fila na mesma posição.";

    private final FilaDeEsperaRepositoryPort fila;
    private final AgendamentoRepositoryPort agendamentos;
    private final EncaminhamentoRepositoryPort encaminhamentos;
    private final ProfissionalRepositoryPort profissionais;
    private final TransacaoPort transacao;
    private final NotificacaoAoPaciente notificacao;
    private final ModeloMensagens modelos;
    private final DiarioDaFila diario;
    private final Clock clock;

    public ReservaDeVaga(FilaDeEsperaRepositoryPort fila, AgendamentoRepositoryPort agendamentos,
                         EncaminhamentoRepositoryPort encaminhamentos, ProfissionalRepositoryPort profissionais,
                         TransacaoPort transacao, NotificacaoAoPaciente notificacao, ModeloMensagens modelos, Clock clock) {
        this.fila = fila;
        this.agendamentos = agendamentos;
        this.encaminhamentos = encaminhamentos;
        this.profissionais = profissionais;
        this.transacao = transacao;
        this.notificacao = notificacao;
        this.modelos = modelos;
        this.diario = new DiarioDaFila(fila, clock);
        this.clock = clock;
    }

    public Resultado aceitar(UUID ofertaId, Ator ator, CanalDeResposta canal) {
        Resultado resultado;
        try {
            resultado = transacao.executar(() -> reservar(ofertaId, ator, canal));
        } catch (ConflitoException e) {
            resultado = transacao.executar(() -> descartar(ofertaId, ator, "O horário deixou de estar livre na agenda"));
        }

        if (resultado.desfecho() == Desfecho.ACEITA) {
            Resultado r = resultado;
            notificacao.notificar(r.nova().getId(), r.nova().getPacienteId(), p -> modelos.vagaConfirmada(p, r.nova(), r.anterior()));
        }

        return resultado;
    }

    public Resultado recusar(UUID ofertaId, Ator ator, CanalDeResposta canal) {
        return transacao.executar(() -> {
            OfertaDeVaga o = ofertaDoAtor(ofertaId, ator);
            Instant agora = Instant.now(clock);
            if (!o.ativa()) return new Resultado(Desfecho.INDISPONIVEL, o, null, null, INDISPONIVEL);
            if (o.vencida(agora)) return expirar(o, ator);

            o.recusar(canal, agora);
            fila.salvarOferta(o);
            diario.daOferta(EventoDaFila.OFERTA_RECUSADA, o, null, "Recusada pelo " + canal.name().toLowerCase()
                    + ". A pessoa continua na fila na mesma posição.", ator);
            return new Resultado(Desfecho.RECUSADA, o, null, null, null);
        });
    }

    private Resultado reservar(UUID ofertaId, Ator ator, CanalDeResposta canal) {
        OfertaDeVaga o = ofertaDoAtor(ofertaId, ator);
        Instant agora = Instant.now(clock);
        if (o.getStatus() == StatusOferta.ACEITA)
            return new Resultado(Desfecho.JA_ACEITA, o, agendamentos.buscarAgendamento(o.getConsultaNovaId()).orElse(null), null, null);
        if (!o.ativa()) return new Resultado(Desfecho.INDISPONIVEL, o, null, null, INDISPONIVEL);
        if (o.vencida(agora)) return expirar(o, ator);

        VagaLiberada v = fila.vagaParaAtualizar(o.getVagaId()).orElseThrow();
        EntradaNaFila e = fila.entradaParaAtualizar(o.getEntradaId()).orElseThrow();
        LocalDateTime agoraLocal = LocalDateTime.now(clock);
        if (!v.aberta()) return cancelar(o, null, "A vaga já foi preenchida ou encerrada", ator);
        if (!e.podeReceberOferta()) return cancelar(o, null, "A entrada na fila não está aguardando vaga", ator);
        if (!v.getInicio().isAfter(agoraLocal)) return cancelar(o, v, "O horário já passou", ator);

        Optional<Profissional> profissional = profissionais.buscarProfissional(v.getProfissionalId())
                .filter(p -> p.atende(v.getEspecialidade()))
                .filter(p -> profissionais.disponibilidadesDe(p.id()).stream().anyMatch(d -> d.comporta(v.getInicio())));
        if (profissional.isEmpty()) return cancelar(o, v, "O horário saiu da agenda do profissional", ator);
        if (fila.horarioOcupado(v.getProfissionalId(), v.getInicio(), v.getFim()))
            return cancelar(o, v, "O horário foi ocupado na agenda", ator);

        Agendamento atual = fila.consultaQueOcupaAgenda(e.getEncaminhamentoId())
                .flatMap(agendamentos::buscarAgendamentoParaAtualizar)
                .filter(a -> a.getStatus().ocupaAgenda() && a.getStatus() != StatusAgendamento.EM_ANDAMENTO)
                .orElse(null);
        if (atual != null && !atual.getDataHora().isAfter(v.getInicio()))
            return cancelar(o, null, "A consulta atual já é antes deste horário", ator);
        if (fila.pacienteOcupado(o.getPacienteId(), v.getInicio(), v.getFim(), atual == null ? null : atual.getId()))
            return cancelar(o, null, "A pessoa já tem outra consulta neste horário", ator);

        Encaminhamento encaminhamento = encaminhamentos.buscarEncaminhamentoParaAtualizar(e.getEncaminhamentoId()).orElseThrow();
        if (atual == null && !encaminhamento.permiteAgendamento())
            return cancelar(o, null, "O encaminhamento não está aguardando agendamento", ator);

        if (atual != null) {
            atual.cancelar();
            agendamentos.salvarAgendamento(atual);
        } else {
            encaminhamento.consultaAgendada();
            encaminhamentos.salvarEncaminhamento(encaminhamento);
        }

        Agendamento nova = Agendamento.consulta(UUID.randomUUID(), encaminhamento, profissional.get(), v.getInicio(),
                v.getDuracaoMinutos(), atual != null ? atual.getNatureza() : e.getNatureza(), "oferta-" + o.getId());
        nova.confirmarPresenca();
        agendamentos.salvarAgendamento(nova);

        o.aceitar(nova.getId(), canal, agora);
        fila.salvarOferta(o);
        v.preencher(agora);
        fila.salvarVaga(v);
        e.vagaObtida(agora);
        fila.salvarEntrada(e);
        diario.daOferta(EventoDaFila.OFERTA_ACEITA, o, nova.getId(), "Aceita pelo " + canal.name().toLowerCase(), ator);

        if (atual != null)
            diario.daOferta(EventoDaFila.CONSULTA_ANTECIPADA, o, atual.getId(),
                    "Consulta antecipada; o horário anterior foi liberado para a fila", ator);

        return new Resultado(Desfecho.ACEITA, o, nova, atual == null ? null : atual.getDataHora(), null);
    }

    private Resultado descartar(UUID ofertaId, Ator ator, String motivo) {
        OfertaDeVaga o = fila.ofertaParaAtualizar(ofertaId).orElseThrow();
        if (o.getStatus() == StatusOferta.ACEITA)
            return new Resultado(Desfecho.JA_ACEITA, o, agendamentos.buscarAgendamento(o.getConsultaNovaId()).orElse(null), null, null);
        if (!o.ativa()) return new Resultado(Desfecho.INDISPONIVEL, o, null, null, INDISPONIVEL);

        VagaLiberada v = fila.vagaParaAtualizar(o.getVagaId()).orElseThrow();
        return cancelar(o, v.aberta() && fila.horarioOcupado(v.getProfissionalId(), v.getInicio(), v.getFim()) ? v : null, motivo, ator);
    }

    private Resultado cancelar(OfertaDeVaga o, VagaLiberada vagaPerdida, String motivo, Ator ator) {
        Instant agora = Instant.now(clock);
        o.cancelar(motivo);
        fila.salvarOferta(o);
        diario.daOferta(EventoDaFila.OFERTA_CANCELADA, o, null, motivo, ator);

        if (vagaPerdida != null && vagaPerdida.aberta()) {
            vagaPerdida.indisponivel(motivo, agora);
            fila.salvarVaga(vagaPerdida);
            diario.daVaga(EventoDaFila.VAGA_INDISPONIVEL, vagaPerdida, motivo);
        }

        return new Resultado(Desfecho.INDISPONIVEL, o, null, null, INDISPONIVEL);
    }

    private Resultado expirar(OfertaDeVaga o, Ator ator) {
        o.expirar(Instant.now(clock));
        fila.salvarOferta(o);
        diario.daOferta(EventoDaFila.OFERTA_EXPIRADA, o, null, "Resposta recebida depois do prazo", ator);
        return new Resultado(Desfecho.EXPIRADA, o, null, null,
                "O prazo para responder esta oferta terminou. Você continua na fila na mesma posição.");
    }

    private OfertaDeVaga ofertaDoAtor(UUID ofertaId, Ator ator) {
        OfertaDeVaga o = fila.ofertaParaAtualizar(ofertaId)
                .orElseThrow(() -> new NaoEncontradoException("Oferta não encontrada: " + ofertaId));
        if (ator.agePeloPaciente() && !o.pertenceA(ator.pacienteId()))
            throw new AcessoNegadoException("Esta oferta não é sua");
        return o;
    }
}
