package sus.liva.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import sus.liva.infrastructure.adapter.in.web.seguranca.ChaveApiFiltro;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CorsConfigTest {

    private static final String FRONT = "http://localhost:3000";
    private static final String CHAVE = "chave";

    @RestController
    static class Rotas {
        @GetMapping("/api/agendamentos")
        String listar() {
            return "[]";
        }
    }

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new Rotas())
            .addFilters(new CorsConfig().corsFilter(new String[]{FRONT}).getFilter(), new ChaveApiFiltro(CHAVE))
            .build();

    @Test
    void respondeAoPreflightDoFrontSemExigirAChave() throws Exception {
        mvc.perform(options("/api/agendamentos")
                        .header(HttpHeaders.ORIGIN, FRONT)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type, x-api-key"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONT))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsStringIgnoringCase("x-api-key")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("POST")));
    }

    @Test
    void liberaAChamadaDoFrontComAChave() throws Exception {
        mvc.perform(get("/api/agendamentos").header(HttpHeaders.ORIGIN, FRONT).header(ChaveApiFiltro.CABECALHO, CHAVE))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONT))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, containsString("Location")));
    }

    @Test
    void oFrontConsegueLerO401() throws Exception {
        mvc.perform(get("/api/agendamentos").header(HttpHeaders.ORIGIN, FRONT))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONT));
    }

    @Test
    void recusaOutraOrigem() throws Exception {
        mvc.perform(options("/api/agendamentos")
                        .header(HttpHeaders.ORIGIN, "http://site-malicioso.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }
}
