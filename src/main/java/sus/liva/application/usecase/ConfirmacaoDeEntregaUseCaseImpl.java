package sus.liva.application.usecase;

import sus.liva.application.port.in.ConfirmacaoDeEntregaUseCase;
import sus.liva.application.port.out.RegistroMensagemPort;

import java.time.Clock;
import java.time.Instant;

public class ConfirmacaoDeEntregaUseCaseImpl implements ConfirmacaoDeEntregaUseCase {
    private final RegistroMensagemPort registros;
    private final Clock clock;

    public ConfirmacaoDeEntregaUseCaseImpl(RegistroMensagemPort registros, Clock clock) {
        this.registros = registros;
        this.clock = clock;
    }

    @Override
    public boolean registrarEntrega(String idExterno) {
        if (idExterno == null || idExterno.isBlank()) return false;
        return registros.marcarEntregue(idExterno, Instant.now(clock));
    }
}
