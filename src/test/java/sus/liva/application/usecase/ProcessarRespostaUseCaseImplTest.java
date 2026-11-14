package sus.liva.application.usecase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sus.liva.application.port.in.ResultadoResposta;
import sus.liva.application.port.out.EnviadorMensagemPort;
import sus.liva.application.port.out.MensagemSaida;
import sus.liva.application.port.out.ResultadoEnvio;
import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.agendamento.StatusAgendamento;
import sus.liva.domain.model.agendamento.TipoAtendimento;
import sus.liva.domain.model.agendamento.TipoLembrete;
import sus.liva.domain.model.mensagem.RegistroMensagem;
import sus.liva.domain.model.paciente.Paciente;
import sus.liva.domain.model.paciente.Sexo;
import sus.liva.infrastructure.adapter.out.memoria.AgendamentoMemoriaAdapter;
import sus.liva.infrastructure.adapter.out.memoria.PacienteMemoriaAdapter;
import sus.liva.infrastructure.adapter.out.memoria.RegistroMensagemMemoriaAdapter;
import sus.liva.infrastructure.adapter.out.memoria.TransacaoFake;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessarRespostaUseCaseImplTest {

    private static final UUID AGENDAMENTO_ID = UUID.randomUUID();
    private static final UUID PACIENTE_ID = UUID.randomUUID();
    private static final Telefone TELEFONE_PACIENTE = Telefone.de("51993953661");

    private final TransacaoFake transacao = new TransacaoFake();

    private class EnviadorFake implements EnviadorMensagemPort {
        final List<MensagemSaida> enviadas = new ArrayList<>();
        final List<Boolean> dentroDeTransacao = new ArrayList<>();
        ResultadoEnvio resposta = ResultadoEnvio.ok("id-retorno");

        @Override
        public ResultadoEnvio enviar(MensagemSaida mensagem) {
            enviadas.add(mensagem);
            dentroDeTransacao.add(transacao.ativa());
            return resposta;
        }
    }

    private final RegistroMensagemMemoriaAdapter registros = new RegistroMensagemMemoriaAdapter();
    private final AgendamentoMemoriaAdapter agendamentos = new AgendamentoMemoriaAdapter();
    private final PacienteMemoriaAdapter pacientes = new PacienteMemoriaAdapter();
    private final EnviadorFake enviador = new EnviadorFake();
    private final ProcessarRespostaUseCaseImpl useCase =
            new ProcessarRespostaUseCaseImpl(registros, agendamentos, pacientes, enviador, new ModeloMensagens(), transacao);

    @BeforeEach
    void setUp() {
        pacientes.salvarPaciente(paciente());
        agendamentos.salvarAgendamento(new Agendamento(StatusAgendamento.AGUARDANDO_RESPOSTA, null, "Rua A, 1",
                "UBS Centro", TipoAtendimento.CONSULTA, LocalDateTime.now().plusDays(2), PACIENTE_ID, AGENDAMENTO_ID));
        registros.salvarRegistro(new RegistroMensagem(AGENDAMENTO_ID, TELEFONE_PACIENTE, TipoLembrete.PEDIDO_CONFIRMACAO,
                "pedido", true, "id-1", null, Instant.now()));
    }

    @Test
    void confirmaAgendamentoMesmoComJidSemNonoDigito() {
        ResultadoResposta resultado = useCase.processar(Telefone.de("555193953661"), "1");

        assertThat(resultado).isEqualTo(ResultadoResposta.aplicada(AGENDAMENTO_ID, StatusAgendamento.CONFIRMADO, true));
        assertThat(agendamentos.buscarAgendamento(AGENDAMENTO_ID).orElseThrow().getStatus())
                .isEqualTo(StatusAgendamento.CONFIRMADO);
    }

    @Test
    void enviaRetornoDeConfirmacaoParaOTelefoneDoPedido() {
        useCase.processar(Telefone.de("555193953661"), "1");

        assertThat(enviador.enviadas).hasSize(1);
        MensagemSaida retorno = enviador.enviadas.getFirst();
        assertThat(retorno.destino()).isEqualTo(TELEFONE_PACIENTE);
        assertThat(retorno.agendamentoId()).isEqualTo(AGENDAMENTO_ID);
        assertThat(retorno.texto()).contains("Maria da Silva").contains("presença está confirmada").contains("UBS Centro");
    }

    @Test
    void enviaRetornoSomenteDepoisDoCommitDaTransacao() {
        useCase.processar(TELEFONE_PACIENTE, "1");

        assertThat(transacao.execucoes()).isEqualTo(1);
        assertThat(enviador.dentroDeTransacao).containsExactly(false);
    }

    @Test
    void naoAbreTransacaoParaRespostaNaoReconhecida() {
        useCase.processar(TELEFONE_PACIENTE, "talvez");

        assertThat(transacao.execucoes()).isZero();
    }

    @Test
    void cancelaAgendamentoEEnviaRetornoDeCancelamento() {
        ResultadoResposta resultado = useCase.processar(TELEFONE_PACIENTE, "Não vou conseguir");

        assertThat(resultado.status()).isEqualTo(StatusAgendamento.CANCELADO);
        assertThat(enviador.enviadas.getFirst().texto()).contains("Cancelamos seu atendimento");
    }

    @Test
    void solicitaReagendamentoEEnviaRetorno() {
        ResultadoResposta resultado = useCase.processar(TELEFONE_PACIENTE, "3");

        assertThat(resultado.status()).isEqualTo(StatusAgendamento.REAGENDADO);
        assertThat(enviador.enviadas.getFirst().texto()).contains("outro horário");
    }

    @Test
    void mantemStatusQuandoRetornoFalha() {
        enviador.resposta = ResultadoEnvio.falha("HTTP 500: erro");

        ResultadoResposta resultado = useCase.processar(TELEFONE_PACIENTE, "1");

        assertThat(resultado.aplicada()).isTrue();
        assertThat(resultado.retornoEnviado()).isFalse();
        assertThat(agendamentos.buscarAgendamento(AGENDAMENTO_ID).orElseThrow().getStatus())
                .isEqualTo(StatusAgendamento.CONFIRMADO);
    }

    @Test
    void ignoraRespostaNaoReconhecidaSemEnviarRetorno() {
        ResultadoResposta resultado = useCase.processar(TELEFONE_PACIENTE, "talvez");

        assertThat(resultado.aplicada()).isFalse();
        assertThat(enviador.enviadas).isEmpty();
        assertThat(agendamentos.buscarAgendamento(AGENDAMENTO_ID).orElseThrow().getStatus())
                .isEqualTo(StatusAgendamento.AGUARDANDO_RESPOSTA);
    }

    @Test
    void ignoraTelefoneSemPedidoDeConfirmacao() {
        ResultadoResposta resultado = useCase.processar(Telefone.de("11987654321"), "1");

        assertThat(resultado.aplicada()).isFalse();
        assertThat(resultado.motivo()).contains("Nenhum pedido");
        assertThat(enviador.enviadas).isEmpty();
    }

    @Test
    void ignoraSegundaRespostaDepoisDeConfirmado() {
        useCase.processar(TELEFONE_PACIENTE, "1");

        ResultadoResposta resultado = useCase.processar(TELEFONE_PACIENTE, "2");

        assertThat(resultado.aplicada()).isFalse();
        assertThat(resultado.motivo()).contains("CONFIRMADO");
        assertThat(enviador.enviadas).hasSize(1);
    }

    private static Paciente paciente() {
        return new Paciente(PACIENTE_ID, "Maria da Silva", null, "12345678909", null, LocalDate.of(1958, 3, 12),
                Sexo.FEMININO, null, TELEFONE_PACIENTE, null, null, null, null, null, null, true);
    }
}
