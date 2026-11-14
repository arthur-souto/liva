package sus.liva.infrastructure.adapter.in.web.teste;

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
import sus.liva.application.usecase.ModeloMensagens;
import sus.liva.application.usecase.ProcessarRespostaUseCaseImpl;
import sus.liva.infrastructure.adapter.in.web.GlobalExceptionHandler;
import sus.liva.infrastructure.adapter.out.memoria.AgendamentoMemoriaAdapter;
import sus.liva.infrastructure.adapter.out.memoria.PacienteMemoriaAdapter;
import sus.liva.infrastructure.adapter.out.memoria.RegistroMensagemMemoriaAdapter;
import sus.liva.infrastructure.adapter.out.memoria.TransacaoFake;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AtendimentoTesteControllerTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-13T12:00:00Z"), ZoneId.of("America/Sao_Paulo"));

    private final List<MensagemSaida> enviadas = new ArrayList<>();
    private final EnviadorMensagemPort enviador = m -> {
        enviadas.add(m);
        return ResultadoEnvio.ok("id-" + enviadas.size());
    };
    private final PacienteMemoriaAdapter pacientes = new PacienteMemoriaAdapter();
    private final AgendamentoMemoriaAdapter agendamentos = new AgendamentoMemoriaAdapter();
    private final RegistroMensagemMemoriaAdapter registros = new RegistroMensagemMemoriaAdapter();
    private final ModeloMensagens modelos = new ModeloMensagens();

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AtendimentoTesteController(
                    new AgendarAtendimentoUseCaseImpl(pacientes, agendamentos, new TransacaoFake(), CLOCK),
                    new DispararLembreteUseCaseImpl(agendamentos, pacientes, registros, enviador, modelos),
                    new ConsultarAgendamentoUseCaseImpl(agendamentos),
                    new ProcessarRespostaUseCaseImpl(registros, agendamentos, pacientes, enviador, modelos, new TransacaoFake()),
                    CLOCK))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void criaAtendimentoComPadroesEDisparaPedidoDeConfirmacao() throws Exception {
        mvc.perform(post("/api/teste/atendimentos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telefone\":\"51993953661\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.agendamento.status").value("AGUARDANDO_RESPOSTA"))
                .andExpect(jsonPath("$.agendamento.dataHora").value("2026-11-15T09:30:00"))
                .andExpect(jsonPath("$.agendamento.unidade").value("UBS Centro"))
                .andExpect(jsonPath("$.disparo.envios[0].sucesso").value(true))
                .andExpect(jsonPath("$.proximoPasso").value(org.hamcrest.Matchers.containsString("Responda 1, 2 ou 3")));

        assertThat(enviadas).hasSize(1);
        assertThat(enviadas.getFirst().texto()).contains("Paciente Teste").contains("Você consegue ir?");
    }

    @Test
    void enviaTambemParaOCuidador() throws Exception {
        mvc.perform(post("/api/teste/atendimentos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telefone\":\"51993953661\",\"nomeCuidador\":\"João\",\"telefoneCuidador\":\"11987654321\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.disparo.envios.length()").value(2));

        assertThat(enviadas.get(1).destino().apenasDigitos()).isEqualTo("5511987654321");
        assertThat(enviadas.get(1).texto()).startsWith("Olá, João.");
    }

    @Test
    void criaSemDispararQuandoSolicitado() throws Exception {
        mvc.perform(post("/api/teste/atendimentos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telefone\":\"51993953661\",\"disparar\":false}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.agendamento.status").value("AGENDADO"))
                .andExpect(jsonPath("$.disparo").doesNotExist());

        assertThat(enviadas).isEmpty();
    }

    @Test
    void simulaRespostaEConfirmaAtendimento() throws Exception {
        mvc.perform(post("/api/teste/atendimentos").contentType(MediaType.APPLICATION_JSON)
                .content("{\"telefone\":\"51993953661\"}"));

        mvc.perform(post("/api/teste/atendimentos/respostas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telefone\":\"51993953661\",\"texto\":\"1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aplicada").value(true))
                .andExpect(jsonPath("$.status").value("CONFIRMADO"))
                .andExpect(jsonPath("$.retornoEnviado").value(true));

        assertThat(enviadas.getLast().texto()).contains("presença está confirmada");
    }

    @Test
    void retorna400ParaTelefoneInvalido() throws Exception {
        mvc.perform(post("/api/teste/atendimentos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"telefone\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").exists());
    }
}
