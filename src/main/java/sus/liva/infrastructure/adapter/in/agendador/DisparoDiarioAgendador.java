package sus.liva.infrastructure.adapter.in.agendador;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import sus.liva.application.port.in.DispararLembretesDoDiaUseCase;

import java.time.Clock;
import java.time.LocalDate;

@Component
@ConditionalOnProperty(name = "confirmasus.disparo-diario.habilitado", havingValue = "true", matchIfMissing = true)
public class DisparoDiarioAgendador {
    private static final Logger log = LoggerFactory.getLogger(DisparoDiarioAgendador.class);

    private final DispararLembretesDoDiaUseCase lembretesDoDia;
    private final Clock clock;

    public DisparoDiarioAgendador(DispararLembretesDoDiaUseCase lembretesDoDia, Clock clock) {
        this.lembretesDoDia = lembretesDoDia;
        this.clock = clock;
    }

    @Scheduled(cron = "${confirmasus.disparo-diario.cron}", zone = "${confirmasus.fuso}")
    public void executar() {
        LocalDate hoje = LocalDate.now(clock);
        try {
            int enviadas = lembretesDoDia.dispararPendentes(hoje);
            log.info("Disparo diário de {}: {} mensagem(ns) enviada(s)", hoje, enviadas);
        } catch (RuntimeException e) {
            log.error("Disparo diário de {} falhou", hoje, e);
        }
    }
}
