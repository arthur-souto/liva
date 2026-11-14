package sus.liva.infrastructure.adapter.out.whatsapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import sus.liva.application.port.out.MensagemSaida;
import sus.liva.application.port.out.ResultadoEnvio;
import sus.liva.domain.model.Telefone;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class EvolutionEnviadorAdapterTest {

    private static final String URL = "http://evolution.test/message/sendText/confirmasus";

    private MockRestServiceServer servidor;
    private EvolutionEnviadorAdapter adapter;
    private final MensagemSaida mensagem = new MensagemSaida(null, Telefone.de("11987654321"), null, "Olá");

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        adapter = new EvolutionEnviadorAdapter(builder, "http://evolution.test", "chave-teste", "confirmasus");
    }

    @Test
    void enviaRequisicaoEExtraiKeyId() {
        servidor.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("apikey", "chave-teste"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"number\":\"5511987654321\",\"text\":\"Olá\"}"))
                .andRespond(withSuccess("{\"key\":{\"id\":\"ABC123\"},\"status\":\"PENDING\"}", MediaType.APPLICATION_JSON));

        ResultadoEnvio resultado = adapter.enviar(mensagem);

        assertThat(resultado).isEqualTo(ResultadoEnvio.ok("ABC123"));

        servidor.verify();
    }

    @Test
    void toleraRespostaSemKeyId() {
        servidor.expect(requestTo(URL))
                .andRespond(withSuccess("{\"status\":\"PENDING\"}", MediaType.APPLICATION_JSON));

        ResultadoEnvio resultado = adapter.enviar(mensagem);

        assertThat(resultado.sucesso()).isTrue();
        assertThat(resultado.idExterno()).isNull();
    }

    @Test
    void extraiKeyIdMesmoSemContentTypeJson() {
        servidor.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body("{\"key\":{\"id\":\"XYZ789\"}}"));

        ResultadoEnvio resultado = adapter.enviar(mensagem);

        assertThat(resultado).isEqualTo(ResultadoEnvio.ok("XYZ789"));
    }

    @Test
    void retornaFalhaQuandoSucessoSemCorpo() {
        servidor.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.CREATED));

        ResultadoEnvio resultado = adapter.enviar(mensagem);

        assertThat(resultado.sucesso()).isFalse();
        assertThat(resultado.erro()).contains("201").contains("sem corpo");
    }

    @Test
    void retornaFalhaComCorpoQuandoRespostaNaoEJson() {
        servidor.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body("Connection Closed"));

        ResultadoEnvio resultado = adapter.enviar(mensagem);

        assertThat(resultado.sucesso()).isFalse();
        assertThat(resultado.erro()).contains("Connection Closed");
    }

    @Test
    void retornaFalhaComCorpoEmErro4xx() {
        servidor.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"number not exists\"}"));

        ResultadoEnvio resultado = adapter.enviar(mensagem);

        assertThat(resultado.sucesso()).isFalse();
        assertThat(resultado.erro()).contains("400").contains("number not exists");
    }

    @Test
    void retornaFalhaComCorpoEmErro5xx() {
        servidor.expect(requestTo(URL))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).body("instancia desconectada"));

        ResultadoEnvio resultado = adapter.enviar(mensagem);

        assertThat(resultado.sucesso()).isFalse();
        assertThat(resultado.erro()).contains("500").contains("instancia desconectada");
    }

    @Test
    void retornaFalhaEmErroDeConexao() {
        servidor.expect(requestTo(URL))
                .andRespond(withException(new IOException("Connection refused")));

        ResultadoEnvio resultado = adapter.enviar(mensagem);

        assertThat(resultado.sucesso()).isFalse();
        assertThat(resultado.erro()).contains("Connection refused");
    }
}
