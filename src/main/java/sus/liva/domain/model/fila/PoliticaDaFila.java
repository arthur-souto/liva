package sus.liva.domain.model.fila;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public record PoliticaDaFila(
        Duration prazoDeConfirmacao,
        Duration avisoFinal,
        AcaoSemResposta semResposta,
        boolean exigirEntrega,
        Duration prazoDaOferta,
        Duration antecedenciaMinima,
        int ofertasPorVaga,
        Duration esperaSemCandidato
) {
    public LocalDateTime confirmarAte(LocalDateTime consulta) {
        return consulta.minus(prazoDeConfirmacao);
    }

    public boolean aindaDaTempoDeOfertar(LocalDateTime inicioDaVaga, LocalDateTime agora) {
        return inicioDaVaga.minus(antecedenciaMinima).isAfter(agora);
    }

    public Instant expiracaoDaOferta(Instant agora, LocalDateTime inicioDaVaga, ZoneId fuso) {
        Instant limite = inicioDaVaga.minus(antecedenciaMinima).atZone(fuso).toInstant();
        Instant padrao = agora.plus(prazoDaOferta);
        return padrao.isBefore(limite) ? padrao : limite;
    }

    public boolean liberaSemResposta(boolean pedidoEntregue) {
        return semResposta == AcaoSemResposta.LIBERAR && pedidoEntregue;
    }
}
