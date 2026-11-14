package sus.liva.domain.model.agendamento;

public enum TipoLembrete {
    CONFIRMACAO_AGENDAMENTO(0), PEDIDO_CONFIRMACAO(3), LEMBRETE_VESPERA(1);
    private final int diasAntes;

    TipoLembrete(int diasAntes) {
        this.diasAntes = diasAntes;
    }
    public int getDiasAntes() {
        return diasAntes;
    }
}