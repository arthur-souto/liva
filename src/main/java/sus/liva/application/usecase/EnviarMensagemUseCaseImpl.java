package sus.liva.application.usecase;

import sus.liva.application.port.in.EnviarMensagemUseCase;
import sus.liva.application.port.out.EnviadorMensagemPort;
import sus.liva.application.port.out.MensagemSaida;
import sus.liva.application.port.out.RegistroMensagemPort;
import sus.liva.application.port.out.ResultadoEnvio;
import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.mensagem.RegistroMensagem;

import java.time.Clock;
import java.time.Instant;

public class EnviarMensagemUseCaseImpl implements EnviarMensagemUseCase {
    private final EnviadorMensagemPort enviador;
    private final RegistroMensagemPort registros;
    private final Clock clock;

    public EnviarMensagemUseCaseImpl(EnviadorMensagemPort enviador, RegistroMensagemPort registros, Clock clock) {
        this.enviador = enviador;
        this.registros = registros;
        this.clock = clock;
    }

    @Override
    public ResultadoEnvio enviar(String telefone, String texto) {
        Telefone destino = Telefone.de(telefone);
        if (texto == null || texto.isBlank())
            throw new IllegalArgumentException("Texto da mensagem não pode ser vazio");
        ResultadoEnvio resultado = enviador.enviar(new MensagemSaida(null, destino, null, texto));
        registros.salvarRegistro(new RegistroMensagem(null, destino, null, texto,
                resultado.sucesso(), resultado.idExterno(), resultado.erro(), Instant.now(clock)));
        return resultado;
    }
}
