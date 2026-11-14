package sus.liva.application.port.in;

import sus.liva.domain.model.mensagem.RegistroMensagem;

import java.util.List;

public record ResultadoDisparo(List<RegistroMensagem> envios, String motivoNaoEnvio) {
    public static ResultadoDisparo enviado(List<RegistroMensagem> e) { return new ResultadoDisparo(e, null); }
    public static ResultadoDisparo ignorado(String motivo) { return new ResultadoDisparo(List.of(), motivo); }
}
