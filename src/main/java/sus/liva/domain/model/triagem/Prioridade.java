package sus.liva.domain.model.triagem;

public enum Prioridade {
    EMERGENCIA,
    MUITO_URGENTE,
    URGENTE,
    POUCO_URGENTE,
    NAO_URGENTE;

    public boolean grave() {
        return this == EMERGENCIA || this == MUITO_URGENTE;
    }
}
