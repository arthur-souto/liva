package sus.liva.domain.model.encaminhamento;

public enum StatusEncaminhamento {
    PENDENTE,
    AGUARDANDO_AGENDAMENTO,
    AGENDADO,
    EM_ATENDIMENTO,
    CONCLUIDO,
    CANCELADO;

    public boolean encerrado() {
        return this == CONCLUIDO || this == CANCELADO;
    }
}
