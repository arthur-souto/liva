package sus.liva.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sus.liva.application.port.out.EnviadorMensagemPort;
import sus.liva.application.port.out.MensagemSaida;
import sus.liva.application.port.out.ResultadoEnvio;
import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.agendamento.AntecedenciaDosLembretes;
import sus.liva.domain.model.agendamento.StatusAgendamento;
import sus.liva.domain.model.agendamento.TipoAtendimento;
import sus.liva.domain.model.agendamento.TipoLembrete;
import sus.liva.domain.model.paciente.Paciente;
import sus.liva.domain.model.paciente.Sexo;
import sus.liva.infrastructure.adapter.out.memoria.AgendamentoMemoriaAdapter;
import sus.liva.infrastructure.adapter.out.memoria.PacienteMemoriaAdapter;
import sus.liva.infrastructure.adapter.out.memoria.RegistroMensagemMemoriaAdapter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DispararLembretesDoDiaUseCaseImplTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 14);
    private static final LocalDate DIA_DO_PEDIDO = HOJE.plusDays(TipoLembrete.PEDIDO_CONFIRMACAO.getDiasAntes());
    private static final LocalDate DIA_DA_VESPERA = HOJE.plusDays(TipoLembrete.LEMBRETE_VESPERA.getDiasAntes());

    private final List<MensagemSaida> enviadas = new ArrayList<>();
    private ResultadoEnvio resposta = ResultadoEnvio.ok("id");
    private final EnviadorMensagemPort enviador = m -> {
        enviadas.add(m);
        return resposta;
    };
    private final PacienteMemoriaAdapter pacientes = new PacienteMemoriaAdapter();
    private final AgendamentoMemoriaAdapter agendamentos = new AgendamentoMemoriaAdapter();
    private final RegistroMensagemMemoriaAdapter registros = new RegistroMensagemMemoriaAdapter();
    private final DispararLembretesDoDiaUseCaseImpl useCase = new DispararLembretesDoDiaUseCaseImpl(agendamentos,
            new DispararLembreteUseCaseImpl(agendamentos, pacientes, registros, enviador, new ModeloMensagens()));

    private Paciente paciente;

    @BeforeEach
    void setUp() {
        paciente = new Paciente(UUID.randomUUID(), "Maria", null, null, null, null, Sexo.FEMININO, null,
                Telefone.de("51993953661"), null, null, null, null, null, null, true);
        pacientes.salvarPaciente(paciente);
    }

    @Test
    void enviaPedidoEVesperaConformeAntecedenciaDeCadaLembrete() {
        Agendamento doPedido = agendar(DIA_DO_PEDIDO, paciente.id());
        Agendamento daVespera = agendar(DIA_DA_VESPERA, paciente.id());

        agendar(DIA_DO_PEDIDO.plusDays(1), paciente.id());
        agendar(HOJE, paciente.id());

        int enviadasNoDia = useCase.dispararPendentes(HOJE);

        assertThat(enviadasNoDia).isEqualTo(2);
        assertThat(registros.jaEnviado(doPedido.getId(), TipoLembrete.PEDIDO_CONFIRMACAO)).isTrue();
        assertThat(registros.jaEnviado(daVespera.getId(), TipoLembrete.LEMBRETE_VESPERA)).isTrue();
        assertThat(agendamentos.buscarAgendamento(doPedido.getId()).orElseThrow().getStatus())
                .isEqualTo(StatusAgendamento.AGUARDANDO_RESPOSTA);
    }

    @Test
    void usaAAntecedenciaConfiguradaParaOPedidoEAVespera() {
        DispararLembretesDoDiaUseCaseImpl configurado = new DispararLembretesDoDiaUseCaseImpl(agendamentos,
                new DispararLembreteUseCaseImpl(agendamentos, pacientes, registros, enviador, new ModeloMensagens()),
                new AntecedenciaDosLembretes(2, 0));
        Agendamento doPedido = agendar(HOJE.plusDays(2), paciente.id());
        Agendamento deHoje = agendar(HOJE, paciente.id());
        Agendamento doPadrao = agendar(DIA_DO_PEDIDO, paciente.id());

        configurado.dispararPendentes(HOJE);

        assertThat(registros.jaEnviado(doPedido.getId(), TipoLembrete.PEDIDO_CONFIRMACAO)).isTrue();
        assertThat(registros.jaEnviado(deHoje.getId(), TipoLembrete.LEMBRETE_VESPERA)).isTrue();
        assertThat(registros.jaEnviado(doPadrao.getId(), TipoLembrete.PEDIDO_CONFIRMACAO)).isFalse();
    }

    @Test
    void naoReenviaQuandoRodaMaisDeUmaVezNoDia() {
        agendar(DIA_DO_PEDIDO, paciente.id());
        agendar(DIA_DA_VESPERA, paciente.id());

        useCase.dispararPendentes(HOJE);
        int segundaExecucao = useCase.dispararPendentes(HOJE);

        assertThat(segundaExecucao).isZero();
        assertThat(enviadas).hasSize(2);
    }

    @Test
    void contaSomenteEnviosComSucessoEReenviaNaProximaExecucao() {
        agendar(DIA_DO_PEDIDO, paciente.id());
        resposta = ResultadoEnvio.falha("HTTP 500");

        assertThat(useCase.dispararPendentes(HOJE)).isZero();

        resposta = ResultadoEnvio.ok("id");

        assertThat(useCase.dispararPendentes(HOJE)).isEqualTo(1);
    }

    @Test
    void continuaQuandoUmAgendamentoFalha() {
        agendar(DIA_DO_PEDIDO, UUID.randomUUID());

        Agendamento valido = agendar(DIA_DO_PEDIDO, paciente.id());

        int enviadasNoDia = useCase.dispararPendentes(HOJE);

        assertThat(enviadasNoDia).isEqualTo(1);
        assertThat(registros.jaEnviado(valido.getId(), TipoLembrete.PEDIDO_CONFIRMACAO)).isTrue();
    }

    @Test
    void naoEnviaParaAgendamentoCancelado() {
        Agendamento cancelado = new Agendamento(StatusAgendamento.CANCELADO, null, "Rua A", "UBS", TipoAtendimento.CONSULTA,
                DIA_DO_PEDIDO.atTime(9, 0), paciente.id(), UUID.randomUUID());
        agendamentos.salvarAgendamento(cancelado);

        assertThat(useCase.dispararPendentes(HOJE)).isZero();
        assertThat(enviadas).isEmpty();
    }

    private Agendamento agendar(LocalDate dia, UUID pacienteId) {
        Agendamento a = new Agendamento(StatusAgendamento.AGENDADO, "Jejum", "Rua A", "UBS Centro", TipoAtendimento.CONSULTA,
                dia.atTime(9, 30), pacienteId, UUID.randomUUID());
        agendamentos.salvarAgendamento(a);
        return a;
    }
}
