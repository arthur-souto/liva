package sus.liva.domain.model.agendamento;

import java.time.LocalDateTime;
import java.util.UUID;

public class Agendamento {
    private final UUID id;
    private final UUID pacienteId;
    private final LocalDateTime dataHora;
    private final TipoAtendimento tipo;
    private final String unidade;
    private final String endereco;
    private final String preparo;
    private StatusAgendamento status;

    public Agendamento(StatusAgendamento status, String preparo, String endereco, String unidade, TipoAtendimento tipo, LocalDateTime dataHora, UUID pacienteId, UUID id) {
        this.status = status;
        this.preparo = preparo;
        this.endereco = endereco;
        this.unidade = unidade;
        this.tipo = tipo;
        this.dataHora = dataHora;
        this.pacienteId = pacienteId;
        this.id = id;
    }

    public boolean podeReceber(TipoLembrete lembrete) {
        return switch (status) {
            case AGENDADO, AGUARDANDO_RESPOSTA -> true;
            case CONFIRMADO -> lembrete == TipoLembrete.LEMBRETE_VESPERA; // D-1 leva o preparo
            default -> false;
        };
    }

    public void aguardarResposta() {
        if (status == StatusAgendamento.AGENDADO) status = StatusAgendamento.AGUARDANDO_RESPOSTA;
    }

    public boolean aguardandoResposta() {
        return status == StatusAgendamento.AGUARDANDO_RESPOSTA;
    }

    public void responder(RespostaPaciente resposta) {
        if (!aguardandoResposta())
            throw new IllegalStateException("Agendamento não está aguardando resposta: " + status);
        status = resposta.getStatusResultante();
    }

    public UUID getId() {
        return id;
    }

    public UUID getPacienteId() {
        return pacienteId;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public TipoAtendimento getTipo() {
        return tipo;
    }

    public String getUnidade() {
        return unidade;
    }

    public String getEndereco() {
        return endereco;
    }

    public String getPreparo() {
        return preparo;
    }

    public StatusAgendamento getStatus() {
        return status;
    }

    public void setStatus(StatusAgendamento status) {
        this.status = status;
    }
}