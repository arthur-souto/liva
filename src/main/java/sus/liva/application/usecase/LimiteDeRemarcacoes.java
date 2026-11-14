package sus.liva.application.usecase;

import sus.liva.application.port.in.RemarcacoesDoMes;
import sus.liva.application.port.out.RemarcacaoPort;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class LimiteDeRemarcacoes {
    public static final int POR_MES = 2;
    public static final String PORTAL = "PORTAL";
    public static final String WHATSAPP = "WHATSAPP";
    public static final String RECEPCAO = "RECEPCAO";

    private final RemarcacaoPort remarcacoes;
    private final Clock clock;

    public LimiteDeRemarcacoes(RemarcacaoPort remarcacoes, Clock clock) {
        this.remarcacoes = remarcacoes;
        this.clock = clock;
    }

    public RemarcacoesDoMes doMes(UUID pacienteId) {
        return new RemarcacoesDoMes(POR_MES, remarcacoes.contarDesde(pacienteId, inicioDoMes()));
    }

    public void exigirDisponivel(UUID pacienteId) {
        if (doMes(pacienteId).restantes() == 0)
            throw new IllegalStateException("Você já remarcou " + POR_MES + " vezes este mês. Para mudar de novo, procure a recepção da unidade.");
    }

    public void registrar(UUID pacienteId, UUID anterior, UUID nova, String canal) {
        remarcacoes.registrarRemarcacao(pacienteId, anterior, nova, canal, Instant.now(clock));
    }

    private Instant inicioDoMes() {
        return LocalDate.now(clock).withDayOfMonth(1).atStartOfDay(clock.getZone()).toInstant();
    }
}
