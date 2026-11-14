package sus.liva.domain.model.triagem;

public enum StatusTriagem {
    AGUARDANDO_ATENDIMENTO,
    EM_ANDAMENTO,
    FINALIZADA,
    CANCELADA,
    ENCERRADA;

    public boolean aberta() {
        return this == AGUARDANDO_ATENDIMENTO || this == EM_ANDAMENTO;
    }

    public boolean concluida() {
        return this == FINALIZADA || this == ENCERRADA;
    }
}
