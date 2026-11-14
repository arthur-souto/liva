package sus.liva.domain.model.fila;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public class VagaLiberada {
    private final UUID id;
    private final UUID agendamentoOrigemId;
    private final UUID profissionalId;
    private final String especialidade;
    private final LocalDateTime inicio;
    private final int duracaoMinutos;
    private final String unidade;
    private final String endereco;
    private final OrigemDaVaga origem;
    private final Instant liberadaEm;
    private StatusVaga status;
    private String motivo;
    private Instant proximaTentativaEm;
    private Instant encerradaEm;

    public VagaLiberada(UUID id, UUID agendamentoOrigemId, UUID profissionalId, String especialidade, LocalDateTime inicio,
                        int duracaoMinutos, String unidade, String endereco, OrigemDaVaga origem, StatusVaga status,
                        String motivo, Instant liberadaEm, Instant proximaTentativaEm, Instant encerradaEm) {
        this.id = id;
        this.agendamentoOrigemId = agendamentoOrigemId;
        this.profissionalId = profissionalId;
        this.especialidade = especialidade;
        this.inicio = inicio;
        this.duracaoMinutos = duracaoMinutos;
        this.unidade = unidade;
        this.endereco = endereco;
        this.origem = origem;
        this.status = status;
        this.motivo = motivo;
        this.liberadaEm = liberadaEm;
        this.proximaTentativaEm = proximaTentativaEm;
        this.encerradaEm = encerradaEm;
    }

    public boolean aberta() {
        return status == StatusVaga.ABERTA;
    }

    public LocalDateTime getFim() {
        return inicio.plusMinutes(duracaoMinutos);
    }

    public void preencher(Instant em) {
        exigirAberta();
        status = StatusVaga.PREENCHIDA;
        motivo = null;
        encerradaEm = em;
    }

    public void encerrar(String justificativa, Instant em) {
        exigirAberta();
        status = StatusVaga.ENCERRADA;
        motivo = justificativa;
        encerradaEm = em;
    }

    public void indisponivel(String justificativa, Instant em) {
        exigirAberta();
        status = StatusVaga.INDISPONIVEL;
        motivo = justificativa;
        encerradaEm = em;
    }

    public void tentarDepois(Instant quando) {
        exigirAberta();
        proximaTentativaEm = quando;
    }

    private void exigirAberta() {
        if (status != StatusVaga.ABERTA) throw new IllegalStateException("A vaga não está mais aberta. Situação atual: " + status);
    }

    public UUID getId() {
        return id;
    }

    public UUID getAgendamentoOrigemId() {
        return agendamentoOrigemId;
    }

    public UUID getProfissionalId() {
        return profissionalId;
    }

    public String getEspecialidade() {
        return especialidade;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public int getDuracaoMinutos() {
        return duracaoMinutos;
    }

    public String getUnidade() {
        return unidade;
    }

    public String getEndereco() {
        return endereco;
    }

    public OrigemDaVaga getOrigem() {
        return origem;
    }

    public StatusVaga getStatus() {
        return status;
    }

    public String getMotivo() {
        return motivo;
    }

    public Instant getLiberadaEm() {
        return liberadaEm;
    }

    public Instant getProximaTentativaEm() {
        return proximaTentativaEm;
    }

    public Instant getEncerradaEm() {
        return encerradaEm;
    }
}
