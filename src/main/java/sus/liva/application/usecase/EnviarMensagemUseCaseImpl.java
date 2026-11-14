package sus.liva.application.usecase;

import sus.liva.application.port.in.EnviarMensagemUseCase;
import sus.liva.application.port.out.EnviadorMensagemPort;
import sus.liva.application.port.out.MensagemSaida;
import sus.liva.application.port.out.ResultadoEnvio;
import sus.liva.domain.model.Telefone;

public class EnviarMensagemUseCaseImpl implements EnviarMensagemUseCase {
    private final EnviadorMensagemPort enviador;

    public EnviarMensagemUseCaseImpl(EnviadorMensagemPort enviador) {
        this.enviador = enviador;
    }

    @Override
    public ResultadoEnvio enviar(String telefone, String texto) {
        Telefone destino = Telefone.de(telefone);
        if (texto == null || texto.isBlank())
            throw new IllegalArgumentException("Texto da mensagem não pode ser vazio");
        return enviador.enviar(new MensagemSaida(null, destino, null, texto));
    }
}
