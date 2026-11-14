package sus.liva.domain.model.fila;

public enum StatusEntrada {
    AGUARDANDO,
    PAUSADA,
    ATENDIDA,
    ENCERRADA;

    public boolean ativa() {
        return this == AGUARDANDO || this == PAUSADA;
    }
}
