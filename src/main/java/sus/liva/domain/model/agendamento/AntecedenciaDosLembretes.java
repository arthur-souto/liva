package sus.liva.domain.model.agendamento;

public record AntecedenciaDosLembretes(int pedidoDeConfirmacao, int vespera) {
    public static AntecedenciaDosLembretes padrao() {
        return new AntecedenciaDosLembretes(TipoLembrete.PEDIDO_CONFIRMACAO.getDiasAntes(), TipoLembrete.LEMBRETE_VESPERA.getDiasAntes());
    }

    public int diasAntes(TipoLembrete tipo) {
        return switch (tipo) {
            case PEDIDO_CONFIRMACAO -> pedidoDeConfirmacao;
            case LEMBRETE_VESPERA -> vespera;
            default -> tipo.getDiasAntes();
        };
    }
}
