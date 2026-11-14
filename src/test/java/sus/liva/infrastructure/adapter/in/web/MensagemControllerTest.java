package sus.liva.infrastructure.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import sus.liva.application.port.in.EnviarMensagemUseCase;
import sus.liva.application.port.out.ResultadoEnvio;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MensagemControllerTest {

    private ResultadoEnvio resposta;
    private final EnviarMensagemUseCase useCase = (telefone, texto) -> {
        if (telefone.equals("123")) throw new IllegalArgumentException("Telefone inválido: +123");
        return resposta;
    };
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new MensagemController(useCase))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void retorna200QuandoEnvioTemSucesso() throws Exception {
        resposta = ResultadoEnvio.ok("ABC123");

        mvc.perform(post("/api/mensagens").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telefone\":\"11999999999\",\"texto\":\"Teste\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sucesso").value(true))
                .andExpect(jsonPath("$.idExterno").value("ABC123"));
    }

    @Test
    void retorna400QuandoArgumentoInvalido() throws Exception {
        mvc.perform(post("/api/mensagens").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telefone\":\"123\",\"texto\":\"Teste\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Telefone inválido: +123"));
    }

    @Test
    void retorna502QuandoEvolutionFalha() throws Exception {
        resposta = ResultadoEnvio.falha("HTTP 500: erro");

        mvc.perform(post("/api/mensagens").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telefone\":\"11999999999\",\"texto\":\"Teste\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.sucesso").value(false))
                .andExpect(jsonPath("$.erro").value("HTTP 500: erro"));
    }
}
