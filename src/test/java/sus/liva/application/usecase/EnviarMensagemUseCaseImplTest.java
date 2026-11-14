package sus.liva.application.usecase;

import org.junit.jupiter.api.Test;
import sus.liva.application.port.out.EnviadorMensagemPort;
import sus.liva.application.port.out.MensagemSaida;
import sus.liva.application.port.out.ResultadoEnvio;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnviarMensagemUseCaseImplTest {

    private static class EnviadorFake implements EnviadorMensagemPort {
        final List<MensagemSaida> enviadas = new ArrayList<>();
        ResultadoEnvio resposta = ResultadoEnvio.ok("id-123");

        @Override
        public ResultadoEnvio enviar(MensagemSaida mensagem) {
            enviadas.add(mensagem);
            return resposta;
        }
    }

    private final EnviadorFake enviador = new EnviadorFake();
    private final EnviarMensagemUseCaseImpl useCase = new EnviarMensagemUseCaseImpl(enviador);

    @Test
    void enviaMensagemComTelefoneNormalizado() {
        ResultadoEnvio resultado = useCase.enviar("(11) 98765-4321", "Olá");

        assertThat(resultado).isEqualTo(ResultadoEnvio.ok("id-123"));
        assertThat(enviador.enviadas).hasSize(1);
        assertThat(enviador.enviadas.getFirst().destino().apenasDigitos()).isEqualTo("5511987654321");
        assertThat(enviador.enviadas.getFirst().texto()).isEqualTo("Olá");
    }

    @Test
    void repassaFalhaDoEnviador() {
        enviador.resposta = ResultadoEnvio.falha("HTTP 500: erro");

        ResultadoEnvio resultado = useCase.enviar("11987654321", "Olá");

        assertThat(resultado.sucesso()).isFalse();
        assertThat(resultado.erro()).isEqualTo("HTTP 500: erro");
    }

    @Test
    void rejeitaTelefoneInvalido() {
        assertThatThrownBy(() -> useCase.enviar("123", "Olá")).isInstanceOf(IllegalArgumentException.class);
        assertThat(enviador.enviadas).isEmpty();
    }

    @Test
    void rejeitaTextoVazio() {
        assertThatThrownBy(() -> useCase.enviar("11987654321", "  ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.enviar("11987654321", null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(enviador.enviadas).isEmpty();
    }
}
