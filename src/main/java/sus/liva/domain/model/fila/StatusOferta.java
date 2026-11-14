package sus.liva.domain.model.fila;

public enum StatusOferta {
    PENDENTE,
    ENVIADA,
    ACEITA,
    RECUSADA,
    EXPIRADA,
    CANCELADA;

    public boolean ativa() {
        return this == PENDENTE || this == ENVIADA;
    }
}
