package sus.liva.domain.model.fila;

import sus.liva.domain.model.agendamento.NaturezaConsulta;
import sus.liva.domain.model.triagem.Prioridade;

import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;

public class EntradaNaFila {
    public static final Comparator<EntradaNaFila> ORDEM = Comparator
            .comparing((EntradaNaFila e) -> e.prioridade.ordinal())
            .thenComparing(e -> e.entrouEm)
            .thenComparing(e -> e.id.toString());

    private final UUID id;
    private final UUID encaminhamentoId;
    private final UUID triagemId;
    private final UUID pacienteId;
    private final int ciclo;
    private final String especialidade;
    private final UUID profissionalId;
    private final Prioridade prioridade;
    private final NaturezaConsulta natureza;
    private final Instant entrouEm;
    private StatusEntrada status;
    private String motivo;
    private Instant atualizadaEm;

    public EntradaNaFila(UUID id, UUID encaminhamentoId, UUID triagemId, UUID pacienteId, int ciclo, String especialidade,
                         UUID profissionalId, Prioridade prioridade, NaturezaConsulta natureza, StatusEntrada status,
                         Instant entrouEm, String motivo, Instant atualizadaEm) {
        this.id = id;
        this.encaminhamentoId = encaminhamentoId;
        this.triagemId = triagemId;
        this.pacienteId = pacienteId;
        this.ciclo = ciclo;
        this.especialidade = especialidade;
        this.profissionalId = profissionalId;
        this.prioridade = prioridade;
        this.natureza = natureza;
        this.status = status;
        this.entrouEm = entrouEm;
        this.motivo = motivo;
        this.atualizadaEm = atualizadaEm;
    }

    public boolean podeReceberOferta() {
        return status == StatusEntrada.AGUARDANDO;
    }

    public boolean disputa(UUID profissional, String codigoEspecialidade) {
        return especialidade.equals(codigoEspecialidade) && (profissionalId == null || profissionalId.equals(profissional));
    }

    public void pausar(String justificativa, Instant em) {
        if (status != StatusEntrada.AGUARDANDO)
            throw new IllegalStateException("Só é possível pausar quem está aguardando. Situação atual: " + status);
        if (justificativa == null || justificativa.isBlank()) throw new IllegalArgumentException("Informe o motivo da pausa");

        status = StatusEntrada.PAUSADA;
        motivo = justificativa.trim();
        atualizadaEm = em;
    }

    public void retomar(Instant em) {
        if (status != StatusEntrada.PAUSADA)
            throw new IllegalStateException("Só é possível retomar uma entrada pausada. Situação atual: " + status);

        status = StatusEntrada.AGUARDANDO;
        motivo = null;
        atualizadaEm = em;
    }

    public void vagaObtida(Instant em) {
        if (status != StatusEntrada.AGUARDANDO)
            throw new IllegalStateException("A entrada não está aguardando vaga. Situação atual: " + status);

        status = StatusEntrada.ATENDIDA;
        motivo = "Vaga obtida pela fila";
        atualizadaEm = em;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEncaminhamentoId() {
        return encaminhamentoId;
    }

    public UUID getTriagemId() {
        return triagemId;
    }

    public UUID getPacienteId() {
        return pacienteId;
    }

    public int getCiclo() {
        return ciclo;
    }

    public String getEspecialidade() {
        return especialidade;
    }

    public UUID getProfissionalId() {
        return profissionalId;
    }

    public Prioridade getPrioridade() {
        return prioridade;
    }

    public NaturezaConsulta getNatureza() {
        return natureza;
    }

    public StatusEntrada getStatus() {
        return status;
    }

    public Instant getEntrouEm() {
        return entrouEm;
    }

    public String getMotivo() {
        return motivo;
    }

    public Instant getAtualizadaEm() {
        return atualizadaEm;
    }
}
