package sus.liva.infrastructure.adapter.in.agendador;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import sus.liva.application.port.in.ProcessarFilaUseCase;
import sus.liva.application.port.in.ResumoDoCiclo;

@Component
@ConditionalOnProperty(name = "confirmasus.fila.automacao.habilitada", havingValue = "true")
public class FilaDeEsperaAgendador {
    private static final Logger log = LoggerFactory.getLogger(FilaDeEsperaAgendador.class);

    private final ProcessarFilaUseCase processarFila;

    public FilaDeEsperaAgendador(ProcessarFilaUseCase processarFila) {
        this.processarFila = processarFila;
    }

    @Scheduled(fixedDelayString = "${confirmasus.fila.automacao.intervalo}", initialDelayString = "${confirmasus.fila.automacao.intervalo}")
    public void executar() {
        try {
            ResumoDoCiclo resumo = processarFila.executarCiclo();
            if (!resumo.vazio()) log.info("Fila de espera: {}", resumo);
        } catch (RuntimeException e) {
            log.error("Ciclo da fila de espera falhou", e);
        }
    }
}
