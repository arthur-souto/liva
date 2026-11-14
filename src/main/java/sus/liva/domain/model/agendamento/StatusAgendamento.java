package sus.liva.domain.model.agendamento;

public enum StatusAgendamento {
    AGENDADO,
    AGUARDANDO_RESPOSTA,
    CONFIRMADO,
    CANCELADO,
    REAGENDADO,
    COMPARECEU,
    FALTOU,
    EM_ANDAMENTO;

    public boolean ocupaAgenda() {
        return this == AGENDADO || this == AGUARDANDO_RESPOSTA || this == CONFIRMADO || this == EM_ANDAMENTO;
    }
}
