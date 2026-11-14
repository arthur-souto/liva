package sus.liva.domain.model.triagem;

import java.time.Instant;
import java.util.UUID;

public class Triagem {
    private final UUID id;
    private final UUID pacienteId;
    private final UUID registradaPor;
    private final Instant chegadaEm;
    private final Long versao;
    private StatusTriagem status;
    private UUID profissionalId;
    private Instant iniciadaEm;
    private Instant finalizadaEm;
    private Instant encerradaEm;
    private Instant canceladaEm;
    private String motivoCancelamento;
    private AvaliacaoClinica avaliacao;
    private SinaisVitais sinaisVitais;
    private Prioridade prioridade;
    private String justificativaPrioridade;

    public Triagem(UUID id, UUID pacienteId, UUID registradaPor, Instant chegadaEm, StatusTriagem status,
                   UUID profissionalId, Instant iniciadaEm, Instant finalizadaEm, Instant encerradaEm,
                   Instant canceladaEm, String motivoCancelamento, AvaliacaoClinica avaliacao,
                   SinaisVitais sinaisVitais, Prioridade prioridade, String justificativaPrioridade, Long versao) {
        this.id = id;
        this.pacienteId = pacienteId;
        this.registradaPor = registradaPor;
        this.chegadaEm = chegadaEm;
        this.status = status;
        this.profissionalId = profissionalId;
        this.iniciadaEm = iniciadaEm;
        this.finalizadaEm = finalizadaEm;
        this.encerradaEm = encerradaEm;
        this.canceladaEm = canceladaEm;
        this.motivoCancelamento = motivoCancelamento;
        this.avaliacao = avaliacao;
        this.sinaisVitais = sinaisVitais;
        this.prioridade = prioridade;
        this.justificativaPrioridade = justificativaPrioridade;
        this.versao = versao;
    }

    public static Triagem registrarChegada(UUID id, UUID pacienteId, UUID registradaPor, Instant em) {
        return new Triagem(id, pacienteId, registradaPor, em, StatusTriagem.AGUARDANDO_ATENDIMENTO, null, null, null,
                null, null, null, AvaliacaoClinica.vazia(), SinaisVitais.naoColetados(), null, null, null);
    }

    public void iniciar(UUID profissional, Instant em) {
        exigirStatus(StatusTriagem.AGUARDANDO_ATENDIMENTO, "iniciar");
        profissionalId = profissional;
        iniciadaEm = em;
        status = StatusTriagem.EM_ANDAMENTO;
    }

    public void registrarAvaliacao(UUID profissional, AvaliacaoClinica novaAvaliacao, SinaisVitais sinais,
                                   Prioridade novaPrioridade, String justificativa) {
        exigirResponsavelEmAndamento(profissional);
        if (novaPrioridade != null && novaPrioridade.grave() && vazio(justificativa))
            throw new IllegalArgumentException("Prioridade " + novaPrioridade + " exige justificativa clínica");
        avaliacao = novaAvaliacao;
        sinaisVitais = sinais;
        prioridade = novaPrioridade;
        justificativaPrioridade = vazio(justificativa) ? null : justificativa.trim();
    }

    public void finalizar(UUID profissional, Instant em) {
        exigirResponsavelEmAndamento(profissional);
        if (avaliacao == null || vazio(avaliacao.queixaPrincipal()))
            throw new IllegalStateException("Registre a queixa principal antes de finalizar a triagem");
        if (prioridade == null) throw new IllegalStateException("Classifique a prioridade antes de finalizar a triagem");
        finalizadaEm = em;
        status = StatusTriagem.FINALIZADA;
    }

    public void cancelar(String motivo, Instant em) {
        if (!status.aberta()) throw new IllegalStateException("Somente triagens abertas podem ser canceladas. Status atual: " + status);
        if (vazio(motivo)) throw new IllegalArgumentException("Informe o motivo do cancelamento");
        motivoCancelamento = motivo.trim();
        canceladaEm = em;
        status = StatusTriagem.CANCELADA;
    }

    public void encerrar(Instant em) {
        exigirStatus(StatusTriagem.FINALIZADA, "encerrar");
        encerradaEm = em;
        status = StatusTriagem.ENCERRADA;
    }

    public Retificacao retificar(UUID autor, String texto, String justificativa, Instant em) {
        if (!status.concluida()) throw new IllegalStateException("Somente triagens finalizadas recebem retificação; edite a triagem em andamento");
        if (vazio(texto) || vazio(justificativa)) throw new IllegalArgumentException("Retificação exige texto e justificativa");
        return new Retificacao(id, autor, texto.trim(), justificativa.trim(), em);
    }

    public boolean aceitaEncaminhamento() {
        return status == StatusTriagem.EM_ANDAMENTO;
    }

    public boolean responsavel(UUID profissional) {
        return profissional != null && profissional.equals(profissionalId);
    }

    private void exigirResponsavelEmAndamento(UUID profissional) {
        exigirStatus(StatusTriagem.EM_ANDAMENTO, "alterar");
        if (!responsavel(profissional))
            throw new IllegalStateException("A triagem está sendo conduzida por outro profissional");
    }

    private void exigirStatus(StatusTriagem esperado, String acao) {
        if (status != esperado)
            throw new IllegalStateException("Não é possível " + acao + " uma triagem com status " + status);
    }

    private static boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }

    public UUID getId() {
        return id;
    }

    public UUID getPacienteId() {
        return pacienteId;
    }

    public UUID getRegistradaPor() {
        return registradaPor;
    }

    public Instant getChegadaEm() {
        return chegadaEm;
    }

    public StatusTriagem getStatus() {
        return status;
    }

    public UUID getProfissionalId() {
        return profissionalId;
    }

    public Instant getIniciadaEm() {
        return iniciadaEm;
    }

    public Instant getFinalizadaEm() {
        return finalizadaEm;
    }

    public Instant getEncerradaEm() {
        return encerradaEm;
    }

    public Instant getCanceladaEm() {
        return canceladaEm;
    }

    public String getMotivoCancelamento() {
        return motivoCancelamento;
    }

    public AvaliacaoClinica getAvaliacao() {
        return avaliacao;
    }

    public SinaisVitais getSinaisVitais() {
        return sinaisVitais;
    }

    public Prioridade getPrioridade() {
        return prioridade;
    }

    public String getJustificativaPrioridade() {
        return justificativaPrioridade;
    }

    public Long getVersao() {
        return versao;
    }
}
