package sus.liva.infrastructure.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import sus.liva.application.usecase.ConsultarAgendamentoUseCaseImpl;
import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.agendamento.StatusAgendamento;
import sus.liva.domain.model.agendamento.TipoAtendimento;
import sus.liva.infrastructure.adapter.out.memoria.AgendamentoMemoriaAdapter;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AgendamentoControllerTest {

    private static final UUID ID = UUID.fromString("3f2b8c1e-7a4d-4c9b-9e21-5d6f0a8b1c02");
    private static final UUID PACIENTE_ID = UUID.fromString("a1b2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d");

    private final AgendamentoMemoriaAdapter agendamentos = new AgendamentoMemoriaAdapter();
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new AgendamentoController(new ConsultarAgendamentoUseCaseImpl(agendamentos)))
            .build();

    @Test
    void retornaAgendamentoExistente() throws Exception {
        agendamentos.salvarAgendamento(new Agendamento(StatusAgendamento.CONFIRMADO, "Jejum de 8 horas",
                "Rua das Flores, 123", "UBS Centro", TipoAtendimento.CONSULTA,
                LocalDateTime.of(2026, 11, 15, 9, 30), PACIENTE_ID, ID));

        mvc.perform(get("/api/agendamentos/{id}", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.pacienteId").value(PACIENTE_ID.toString()))
                .andExpect(jsonPath("$.dataHora").value("2026-11-15T09:30:00"))
                .andExpect(jsonPath("$.tipo").value("CONSULTA"))
                .andExpect(jsonPath("$.unidade").value("UBS Centro"))
                .andExpect(jsonPath("$.preparo").value("Jejum de 8 horas"))
                .andExpect(jsonPath("$.status").value("CONFIRMADO"));
    }

    @Test
    void retorna404QuandoNaoExiste() throws Exception {
        mvc.perform(get("/api/agendamentos/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
