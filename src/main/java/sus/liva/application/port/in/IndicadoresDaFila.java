package sus.liva.application.port.in;

import java.time.Instant;

public record IndicadoresDaFila(
        long aguardando,
        long pausadas,
        long vagasAbertas,
        long ofertasAtivas,
        long aguardandoConfirmacao,
        long semResposta,
        long vagasReaproveitadas,
        long ofertasRecusadas,
        long ofertasExpiradas,
        long confirmadas,
        long pedidosEntregues,
        long compareceram,
        long faltaram,
        Instant desde
) {
    public Double taxaDeConfirmacao() {
        return pedidosEntregues == 0 ? null : (double) confirmadas / pedidosEntregues;
    }

    public Double taxaDeAbsenteismo() {
        long total = compareceram + faltaram;
        return total == 0 ? null : (double) faltaram / total;
    }
}
