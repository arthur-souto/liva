package sus.liva.infrastructure.adapter.out.whatsapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import sus.liva.application.port.out.EnviadorMensagemPort;
import sus.liva.application.port.out.MensagemSaida;
import sus.liva.application.port.out.ResultadoEnvio;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "confirmasus.whatsapp.provedor", havingValue = "console", matchIfMissing = true)
public class ConsoleEnviadorAdapter implements EnviadorMensagemPort {
    private static final Logger log = LoggerFactory.getLogger(ConsoleEnviadorAdapter.class);

    @Override
    public ResultadoEnvio enviar(MensagemSaida m) {
        log.info("[WHATSAPP FAKE] para {}:\n{}", m.destino().e164(), m.texto());
        return ResultadoEnvio.ok("fake-" + UUID.randomUUID());
    }
}
