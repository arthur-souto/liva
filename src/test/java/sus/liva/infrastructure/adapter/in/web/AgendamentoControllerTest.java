package sus.liva.infrastructure.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import sus.liva.application.port.out.EnviadorMensagemPort;
import sus.liva.application.port.out.MensagemSaida;
import sus.liva.application.port.out.ResultadoEnvio;
import sus.liva.application.usecase.AgendarAtendimentoUseCaseImpl;
import sus.liva.application.usecase.ConsultarAgendamentoUseCaseImpl;
import sus.liva.application.usecase.DispararLembreteUseCaseImpl;
import sus.liva.application.usecase.ListarAgendamentosUseCaseImpl;
import sus.liva.application.usecase.ModeloMensagens;
import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.agendamento.StatusAgendamento;
import sus.liva.domain.model.agendamento.TipoAtendimento;
import sus.liva.infrastructure.adapter.out.memoria.AgendamentoMemoriaAdapter;
import sus.liva.infrastructure.adapter.out.memoria.PacienteMemoriaAdapter;
import sus.liva.infrastructure.adapter.out.memoria.RegistroMensagemMemoriaAdapter;
import sus.liva.infrastructure.adapter.out.memoria.TransacaoFake;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AgendamentoControllerTest {

    private static final UUID ID = UUID.fromString("3f2b8c1e-7a4d-4c9b-9e21-5d6f0a8b1c02");
    private static final UUID PACIENTE_ID = UUID.fromString("a1b2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-13T12:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    private static final String CORPO = """
            {"nomePaciente": "Maria da Silva", "cpf": "529.982.247-25", "telefone": "51993953661",
             "dataHora": "%s", "tipo": "EXAME", "unidade": "UBS Centro", "endereco": "Rua das Flores, 123",
             "preparo": "Jejum de 8 horas"}""";

    private final List<MensagemSaida> enviadas = new ArrayList<>();
    private final EnviadorMensagemPort enviador = m -> {
        enviadas.add(m);
        return ResultadoEnvio.ok("id");
    };
    private final AgendamentoMemoriaAdapter agendamentos = new AgendamentoMemoriaAdapter();
    private final PacienteMemoriaAdapter pacientes = new PacienteMemoriaAdapter();
    private final DispararLembreteUseCaseImpl dispararLembrete = new DispararLembreteUseCaseImpl(agendamentos, pacientes,
            new RegistroMensagemMemoriaAdapter(), enviador, new ModeloMensagens());
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new AgendamentoController(
                    new AgendarAtendimentoUseCaseImpl(pacientes, agendamentos, dispararLembrete, new TransacaoFake(), CLOCK),
                    new ConsultarAgendamentoUseCaseImpl(agendamentos),
                    new ListarAgendamentosUseCaseImpl(agendamentos, pacientes),
                    CLOCK))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void agendaEEnviaAConfirmacao() throws Exception {
        mvc.perform(post("/api/agendamentos").contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO.formatted("2026-11-25T08:00:00")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/agendamentos/")))
                .andExpect(jsonPath("$.pacienteNovo").value(true))
                .andExpect(jsonPath("$.agendamento.status").value("AGENDADO"))
                .andExpect(jsonPath("$.agendamento.tipo").value("EXAME"))
                .andExpect(jsonPath("$.confirmacao.envios[0].sucesso").value(true))
                .andExpect(jsonPath("$.pedidoConfirmacao").doesNotExist());

        assertThat(enviadas).singleElement().satisfies(m -> assertThat(m.texto()).contains("Maria da Silva", "foi agendado"));
    }

    @Test
    void agendaDentroDoPrazoEJaPedeAConfirmacao() throws Exception {
        mvc.perform(post("/api/agendamentos").contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO.formatted("2026-11-15T08:00:00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.agendamento.status").value("AGUARDANDO_RESPOSTA"))
                .andExpect(jsonPath("$.pedidoConfirmacao.envios[0].sucesso").value(true));
    }

    @Test
    void reaproveitaPacienteNoSegundoAgendamento() throws Exception {
        mvc.perform(post("/api/agendamentos").contentType(MediaType.APPLICATION_JSON)
                .content(CORPO.formatted("2026-11-25T08:00:00")));

        mvc.perform(post("/api/agendamentos").contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO.formatted("2026-12-02T08:00:00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pacienteNovo").value(false));
    }

    @Test
    void retorna400ComMensagemParaDadosInvalidos() throws Exception {
        mvc.perform(post("/api/agendamentos").contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO.formatted("2026-11-01T08:00:00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"))
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("futuro")))
                .andExpect(jsonPath("$.caminho").value("/api/agendamentos"));

        assertThat(enviadas).isEmpty();
    }

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
    void retorna404PadronizadoQuandoNaoExiste() throws Exception {
        UUID id = UUID.randomUUID();
        mvc.perform(get("/api/agendamentos/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"))
                .andExpect(jsonPath("$.mensagem").value("Agendamento não encontrado: " + id));
    }

    @Test
    void retorna400ParaIdQueNaoEUuid() throws Exception {
        mvc.perform(get("/api/agendamentos/{id}", "f2b8c1e-7a4d"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"))
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("UUID")));
    }

    @Test
    void retorna400ParaJsonInvalido() throws Exception {
        mvc.perform(post("/api/agendamentos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nomePaciente\": \"Maria\", \"tipo\": \"VACINA\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"))
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("JSON")));
    }

    @Test
    void retorna405ParaMetodoNaoAceito() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/agendamentos/{id}", ID))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.codigo").value("METODO_NAO_PERMITIDO"));
    }

    @Test
    void listaOsAgendamentosDeHojeComDadosDoPaciente() throws Exception {
        mvc.perform(post("/api/agendamentos").contentType(MediaType.APPLICATION_JSON)
                .content(CORPO.formatted("2026-11-13T15:00:00")));
        mvc.perform(post("/api/agendamentos").contentType(MediaType.APPLICATION_JSON)
                .content(CORPO.formatted("2026-11-20T08:00:00")));

        mvc.perform(get("/api/agendamentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inicio").value("2026-11-13"))
                .andExpect(jsonPath("$.fim").value("2026-11-13"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.porStatus.AGUARDANDO_RESPOSTA").value(1))
                .andExpect(jsonPath("$.porStatus.CONFIRMADO").value(0))
                .andExpect(jsonPath("$.agendamentos[0].nomePaciente").value("Maria da Silva"))
                .andExpect(jsonPath("$.agendamentos[0].telefone").value("5551993953661"))
                .andExpect(jsonPath("$.agendamentos[0].dataHora").value("2026-11-13T15:00:00"));
    }

    @Test
    void filtraPorPeriodoEStatus() throws Exception {
        salvar(StatusAgendamento.CANCELADO, LocalDateTime.of(2026, 11, 16, 8, 0));
        salvar(StatusAgendamento.REAGENDADO, LocalDateTime.of(2026, 11, 17, 8, 0));
        salvar(StatusAgendamento.CONFIRMADO, LocalDateTime.of(2026, 11, 17, 9, 0));
        salvar(StatusAgendamento.CANCELADO, LocalDateTime.of(2026, 11, 30, 9, 0));

        mvc.perform(get("/api/agendamentos").param("inicio", "2026-11-16").param("fim", "2026-11-20")
                        .param("status", "CANCELADO", "REAGENDADO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.agendamentos[0].status").value("CANCELADO"))
                .andExpect(jsonPath("$.agendamentos[1].status").value("REAGENDADO"));
    }

    @Test
    void retorna400ParaPeriodoInvalido() throws Exception {
        mvc.perform(get("/api/agendamentos").param("inicio", "2026-11-20").param("fim", "2026-11-10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("data final")));
        mvc.perform(get("/api/agendamentos").param("inicio", "20/11/2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("AAAA-MM-DD")));
        mvc.perform(get("/api/agendamentos").param("status", "PERDIDO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
    }

    private void salvar(StatusAgendamento status, LocalDateTime dataHora) {
        agendamentos.salvarAgendamento(new Agendamento(status, null, "Rua A", "UBS Centro", TipoAtendimento.CONSULTA,
                dataHora, PACIENTE_ID, UUID.randomUUID()));
    }
}
