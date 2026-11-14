package sus.liva.infrastructure.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import sus.liva.application.usecase.ConsultarHistoricoUseCaseImpl;
import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.agendamento.RespostaPaciente;
import sus.liva.domain.model.agendamento.StatusAgendamento;
import sus.liva.domain.model.agendamento.TipoAtendimento;
import sus.liva.domain.model.agendamento.TipoLembrete;
import sus.liva.domain.model.mensagem.MensagemRecebida;
import sus.liva.domain.model.mensagem.RegistroMensagem;
import sus.liva.infrastructure.adapter.out.memoria.AgendamentoMemoriaAdapter;
import sus.liva.infrastructure.adapter.out.memoria.MensagemRecebidaMemoriaAdapter;
import sus.liva.infrastructure.adapter.out.memoria.RegistroMensagemMemoriaAdapter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HistoricoMensagensControllerTest {

    private static final UUID ID = UUID.randomUUID();
    private static final Telefone TELEFONE = Telefone.de("51993953661");
    private static final Instant PEDIDO = Instant.parse("2026-11-13T12:00:00Z");

    private final AgendamentoMemoriaAdapter agendamentos = new AgendamentoMemoriaAdapter();
    private final RegistroMensagemMemoriaAdapter registros = new RegistroMensagemMemoriaAdapter();
    private final MensagemRecebidaMemoriaAdapter recebidas = new MensagemRecebidaMemoriaAdapter();
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new HistoricoMensagensController(new ConsultarHistoricoUseCaseImpl(agendamentos, registros, recebidas)))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void retornaEnviadasERecebidasEmOrdemCronologica() throws Exception {
        agendamentos.salvarAgendamento(new Agendamento(StatusAgendamento.CONFIRMADO, null, "Rua A, 1", "UBS Centro",
                TipoAtendimento.CONSULTA, LocalDateTime.parse("2026-11-16T09:00:00"), UUID.randomUUID(), ID));
        registros.salvarRegistro(new RegistroMensagem(ID, TELEFONE, null, "Presença confirmada", true, "id-3", null, PEDIDO.plusSeconds(61)));
        recebidas.salvarRecebida(new MensagemRecebida(ID, TELEFONE, "sim", "id-2", RespostaPaciente.CONFIRMAR, PEDIDO.plusSeconds(60)));
        registros.salvarRegistro(new RegistroMensagem(ID, TELEFONE, TipoLembrete.PEDIDO_CONFIRMACAO, "Você consegue ir?", true, "id-1", null, PEDIDO));
        registros.salvarRegistro(new RegistroMensagem(UUID.randomUUID(), TELEFONE, null, "outro agendamento", true, "id-4", null, PEDIDO));

        mvc.perform(get("/api/agendamentos/{id}/mensagens", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].direcao").value("ENVIADA"))
                .andExpect(jsonPath("$[0].tipo").value("PEDIDO_CONFIRMACAO"))
                .andExpect(jsonPath("$[0].telefone").value("5551993953661"))
                .andExpect(jsonPath("$[1].direcao").value("RECEBIDA"))
                .andExpect(jsonPath("$[1].texto").value("sim"))
                .andExpect(jsonPath("$[1].resposta").value("CONFIRMAR"))
                .andExpect(jsonPath("$[2].texto").value("Presença confirmada"));
    }

    @Test
    void retorna404ParaAgendamentoInexistente() throws Exception {
        mvc.perform(get("/api/agendamentos/{id}/mensagens", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }
}
