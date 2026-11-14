package sus.liva.application.port.in;

import sus.liva.application.port.out.ResultadoEnvio;

public interface EnviarMensagemUseCase {
    ResultadoEnvio enviar(String telefone, String texto);
}
