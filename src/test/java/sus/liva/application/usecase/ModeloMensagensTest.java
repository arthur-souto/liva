package sus.liva.application.usecase;

import org.junit.jupiter.api.Test;
import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.agendamento.StatusAgendamento;
import sus.liva.domain.model.agendamento.TipoAtendimento;
import sus.liva.domain.model.agendamento.TipoLembrete;
import sus.liva.domain.model.paciente.Paciente;
import sus.liva.domain.model.paciente.Sexo;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ModeloMensagensTest {

    private final ModeloMensagens modelos = new ModeloMensagens();
    private final Paciente paciente = new Paciente(UUID.randomUUID(), "Maria", null, null, null, null, Sexo.FEMININO,
            null, Telefone.de("51993953661"), null, null, null, null, null, null, true);
    private final Agendamento agendamento = new Agendamento(StatusAgendamento.AGENDADO, null, "Rua A", "UBS Centro",
            TipoAtendimento.CONSULTA, LocalDateTime.of(2026, 11, 16, 9, 30), paciente.id(), UUID.randomUUID());

    @Test
    void pedidoDeConfirmacaoNaoPrometeRespostaPorAudio() {
        String texto = modelos.montar(TipoLembrete.PEDIDO_CONFIRMACAO, paciente, agendamento);

        assertThat(texto).contains("1 - Sim, vou").contains("16/11").contains("09:30").doesNotContainIgnoringCase("áudio");
    }

    @Test
    void orientacaoParaRespostaNaoReconhecidaListaAsOpcoes() {
        String texto = modelos.naoEntendi(paciente, agendamento);

        assertThat(texto).contains("Maria").contains("16/11").contains("09:30").contains("UBS Centro")
                .contains("1 - Sim, vou").contains("2 - Não vou conseguir").contains("3 - Quero outro horário");
    }
}
