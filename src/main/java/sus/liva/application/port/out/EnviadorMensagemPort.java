package sus.liva.application.port.out;

public interface EnviadorMensagemPort {
    ResultadoEnvio enviar(MensagemSaida mensagem);
}
