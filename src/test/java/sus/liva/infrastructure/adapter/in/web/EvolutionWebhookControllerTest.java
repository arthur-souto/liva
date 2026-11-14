package sus.liva.infrastructure.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import sus.liva.application.port.in.ProcessarRespostaUseCase;
import sus.liva.application.port.in.ResultadoResposta;
import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.agendamento.StatusAgendamento;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EvolutionWebhookControllerTest {

    private record Chamada(Telefone remetente, String texto) {}

    private final List<Chamada> chamadas = new ArrayList<>();
    private final ProcessarRespostaUseCase useCase = (remetente, texto) -> {
        chamadas.add(new Chamada(remetente, texto));
        return ResultadoResposta.aplicada(UUID.randomUUID(), StatusAgendamento.CONFIRMADO, true);
    };

    private MockMvc mvc(boolean aceitarProprias) {
        return MockMvcBuilders.standaloneSetup(new EvolutionWebhookController(useCase, "segredo", aceitarProprias)).build();
    }

    private static String evento(String chave, String mensagem) {
        return "{\"event\":\"messages.upsert\",\"instance\":\"liva\",\"data\":{\"key\":" + chave + ",\"message\":" + mensagem + "}}";
    }

    @Test
    void processaRespostaDeTexto() throws Exception {
        mvc(false).perform(post("/api/webhooks/evolution").param("token", "segredo").contentType(MediaType.APPLICATION_JSON)
                        .content(evento("{\"remoteJid\":\"555193953661@s.whatsapp.net\",\"fromMe\":false,\"id\":\"A1\"}",
                                "{\"conversation\":\"1\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aplicada").value(true));

        assertThat(chamadas).containsExactly(new Chamada(Telefone.de("555193953661"), "1"));
    }

    @Test
    void usaRemoteJidAltQuandoRemetenteVemComoLid() throws Exception {
        mvc(false).perform(post("/api/webhooks/evolution").param("token", "segredo").contentType(MediaType.APPLICATION_JSON)
                        .content(evento("{\"remoteJid\":\"123456789@lid\",\"remoteJidAlt\":\"5551993953661@s.whatsapp.net\",\"fromMe\":false}",
                                "{\"extendedTextMessage\":{\"text\":\"Sim\"}}")))
                .andExpect(status().isOk());

        assertThat(chamadas).containsExactly(new Chamada(Telefone.de("5551993953661"), "Sim"));
    }

    @Test
    void rejeitaTokenInvalido() throws Exception {
        mvc(false).perform(post("/api/webhooks/evolution").param("token", "errado").contentType(MediaType.APPLICATION_JSON)
                        .content(evento("{\"remoteJid\":\"555193953661@s.whatsapp.net\",\"fromMe\":false}", "{\"conversation\":\"1\"}")))
                .andExpect(status().isUnauthorized());

        assertThat(chamadas).isEmpty();
    }

    @Test
    void ignoraMensagemPropriaPorPadrao() throws Exception {
        mvc(false).perform(post("/api/webhooks/evolution").param("token", "segredo").contentType(MediaType.APPLICATION_JSON)
                        .content(evento("{\"remoteJid\":\"555193953661@s.whatsapp.net\",\"fromMe\":true}", "{\"conversation\":\"1\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aplicada").value(false));

        assertThat(chamadas).isEmpty();
    }

    @Test
    void processaMensagemPropriaQuandoHabilitado() throws Exception {
        mvc(true).perform(post("/api/webhooks/evolution").param("token", "segredo").contentType(MediaType.APPLICATION_JSON)
                        .content(evento("{\"remoteJid\":\"555193953661@s.whatsapp.net\",\"fromMe\":true}", "{\"conversation\":\"1\"}")))
                .andExpect(status().isOk());

        assertThat(chamadas).hasSize(1);
    }

    @Test
    void ignoraGruposEOutrosEventos() throws Exception {
        mvc(false).perform(post("/api/webhooks/evolution").param("token", "segredo").contentType(MediaType.APPLICATION_JSON)
                        .content(evento("{\"remoteJid\":\"120363@g.us\",\"fromMe\":false}", "{\"conversation\":\"1\"}")))
                .andExpect(jsonPath("$.aplicada").value(false));
        mvc(false).perform(post("/api/webhooks/evolution").param("token", "segredo").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event\":\"connection.update\",\"data\":{}}"))
                .andExpect(jsonPath("$.motivo").value("Evento ignorado"));

        assertThat(chamadas).isEmpty();
    }
}
