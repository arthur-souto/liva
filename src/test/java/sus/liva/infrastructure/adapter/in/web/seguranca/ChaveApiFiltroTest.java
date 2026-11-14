package sus.liva.infrastructure.adapter.in.web.seguranca;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChaveApiFiltroTest {

    private static final String CHAVE = "chave-secreta";

    @RestController
    static class Rotas {
        @GetMapping("/api/agendamentos/1")
        String api() {
            return "ok";
        }

        @PostMapping("/api/webhooks/evolution")
        String webhook() {
            return "ok";
        }

        @GetMapping("/v3/api-docs")
        String documentacao() {
            return "ok";
        }
    }

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new Rotas())
            .addFilters(new ChaveApiFiltro(CHAVE))
            .build();

    @Test
    void liberaComAChaveCorreta() throws Exception {
        mvc.perform(get("/api/agendamentos/1").header(ChaveApiFiltro.CABECALHO, CHAVE))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @Test
    void recusaSemChave() throws Exception {
        mvc.perform(get("/api/agendamentos/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.codigo").value("NAO_AUTORIZADO"))
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("X-API-Key")))
                .andExpect(jsonPath("$.caminho").value("/api/agendamentos/1"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void recusaChaveErrada() throws Exception {
        mvc.perform(get("/api/agendamentos/1").header(ChaveApiFiltro.CABECALHO, "chave-secreta-errada"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void webhookUsaOProprioTokenENaoExigeAChave() throws Exception {
        mvc.perform(post("/api/webhooks/evolution")).andExpect(status().isOk());
    }

    @Test
    void documentacaoFicaAberta() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    void naoSobeSemChaveConfigurada() {
        assertThatThrownBy(() -> new ChaveApiFiltro(" "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("API_KEY");
    }
}
