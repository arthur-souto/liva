package sus.liva.application.usecase;

import org.junit.jupiter.api.Test;
import sus.liva.application.port.in.AtendimentoAgendado;
import sus.liva.application.port.in.NovoAtendimento;
import sus.liva.application.port.out.EnviadorMensagemPort;
import sus.liva.application.port.out.MensagemSaida;
import sus.liva.application.port.out.ResultadoEnvio;
import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.agendamento.StatusAgendamento;
import sus.liva.domain.model.agendamento.TipoAtendimento;
import sus.liva.domain.model.agendamento.TipoLembrete;
import sus.liva.domain.model.paciente.Paciente;
import sus.liva.infrastructure.adapter.out.memoria.AgendamentoMemoriaAdapter;
import sus.liva.infrastructure.adapter.out.memoria.PacienteMemoriaAdapter;
import sus.liva.infrastructure.adapter.out.memoria.RegistroMensagemMemoriaAdapter;
import sus.liva.infrastructure.adapter.out.memoria.TransacaoFake;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgendarAtendimentoUseCaseImplTest {

    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-13T12:00:00Z"), ZONA);
    private static final LocalDate HOJE = LocalDate.now(CLOCK);
    private static final LocalDateTime DENTRO_DO_PRAZO = HOJE.plusDays(TipoLembrete.PEDIDO_CONFIRMACAO.getDiasAntes()).atTime(9, 30);
    private static final LocalDateTime FORA_DO_PRAZO = DENTRO_DO_PRAZO.plusDays(1);
    private static final String CPF = "52998224725";

    private final List<MensagemSaida> enviadas = new ArrayList<>();
    private ResultadoEnvio resposta = ResultadoEnvio.ok("id");
    private final EnviadorMensagemPort enviador = m -> {
        enviadas.add(m);
        return resposta;
    };
    private final PacienteMemoriaAdapter pacientes = new PacienteMemoriaAdapter();
    private final AgendamentoMemoriaAdapter agendamentos = new AgendamentoMemoriaAdapter();
    private final RegistroMensagemMemoriaAdapter registros = new RegistroMensagemMemoriaAdapter();
    private final TransacaoFake transacao = new TransacaoFake();
    private final AgendarAtendimentoUseCaseImpl useCase = new AgendarAtendimentoUseCaseImpl(pacientes, agendamentos,
            new DispararLembreteUseCaseImpl(agendamentos, pacientes, registros, enviador, new ModeloMensagens()),
            transacao, CLOCK);

    private static NovoAtendimento novo(String nome, String cpf, String telefone, String nomeCuidador,
                                        String telefoneCuidador, LocalDateTime dataHora) {
        return new NovoAtendimento(nome, cpf, telefone, nomeCuidador, telefoneCuidador, dataHora,
                TipoAtendimento.EXAME, "UBS Centro", "Rua A, 1", "Jejum");
    }

    private static NovoAtendimento novo(String telefone, LocalDateTime dataHora) {
        return novo("Maria", null, telefone, null, null, dataHora);
    }

    @Test
    void criaPacienteEAgendamento() {
        AtendimentoAgendado agendado = useCase.agendar(novo("(51) 99395-3661", FORA_DO_PRAZO));

        Agendamento agendamento = agendado.agendamento();
        assertThat(agendado.pacienteNovo()).isTrue();
        assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.AGENDADO);
        assertThat(agendamento.getTipo()).isEqualTo(TipoAtendimento.EXAME);
        assertThat(agendamentos.buscarAgendamento(agendamento.getId())).isPresent();

        Paciente paciente = pacientes.buscarPaciente(agendamento.getPacienteId()).orElseThrow();
        assertThat(paciente.nomeCompleto()).isEqualTo("Maria");
        assertThat(paciente.telefone()).isEqualTo(Telefone.de("51993953661"));
        assertThat(paciente.consentimento()).isTrue();
        assertThat(paciente.temCuidador()).isFalse();
    }

    @Test
    void enviaSomenteAConfirmacaoQuandoForaDoPrazoDoPedido() {
        AtendimentoAgendado agendado = useCase.agendar(novo("51993953661", FORA_DO_PRAZO));

        assertThat(agendado.confirmacao().envios()).singleElement().matches(e -> e.sucesso());
        assertThat(agendado.pedidoConfirmacao()).isNull();
        assertThat(enviadas).extracting(MensagemSaida::tipo).containsExactly(TipoLembrete.CONFIRMACAO_AGENDAMENTO);
        assertThat(enviadas.getFirst().texto()).contains("foi agendado");
        assertThat(registros.jaEnviado(agendado.agendamento().getId(), TipoLembrete.CONFIRMACAO_AGENDAMENTO)).isTrue();
    }

    @Test
    void enviaTambemOPedidoQuandoJaEstaDentroDoPrazo() {
        AtendimentoAgendado agendado = useCase.agendar(novo("51993953661", DENTRO_DO_PRAZO));

        assertThat(enviadas).extracting(MensagemSaida::tipo)
                .containsExactly(TipoLembrete.CONFIRMACAO_AGENDAMENTO, TipoLembrete.PEDIDO_CONFIRMACAO);
        assertThat(agendado.pedidoConfirmacao().envios()).singleElement().matches(e -> e.sucesso());
        assertThat(agendado.agendamento().getStatus()).isEqualTo(StatusAgendamento.AGUARDANDO_RESPOSTA);
    }

    @Test
    void mantemOAgendamentoQuandoOWhatsAppFalha() {
        resposta = ResultadoEnvio.falha("HTTP 500");

        AtendimentoAgendado agendado = useCase.agendar(novo("51993953661", DENTRO_DO_PRAZO));

        assertThat(agendado.confirmacao().envios()).singleElement().matches(e -> !e.sucesso());
        assertThat(agendado.agendamento().getStatus()).isEqualTo(StatusAgendamento.AGENDADO);
        assertThat(agendamentos.buscarAgendamento(agendado.agendamento().getId())).isPresent();
    }

    @Test
    void enviaAsMensagensDepoisDeGravar() {
        List<Boolean> transacaoAtivaNoEnvio = new ArrayList<>();
        AgendarAtendimentoUseCaseImpl comVerificacao = new AgendarAtendimentoUseCaseImpl(pacientes, agendamentos,
                new DispararLembreteUseCaseImpl(agendamentos, pacientes, registros, m -> {
                    transacaoAtivaNoEnvio.add(transacao.ativa());
                    return ResultadoEnvio.ok("id");
                }, new ModeloMensagens()), transacao, CLOCK);

        comVerificacao.agendar(novo("51993953661", DENTRO_DO_PRAZO));

        assertThat(transacaoAtivaNoEnvio).hasSize(2).containsOnly(false);
    }

    @Test
    void reaproveitaPacientePeloCpfEAtualizaOTelefone() {
        AtendimentoAgendado primeiro = useCase.agendar(novo("Maria", "529.982.247-25", "51993953661", null, null, FORA_DO_PRAZO));
        AtendimentoAgendado segundo = useCase.agendar(novo("Maria S.", CPF, "11987654321", null, null, FORA_DO_PRAZO.plusDays(7)));

        assertThat(segundo.pacienteNovo()).isFalse();
        assertThat(segundo.agendamento().getPacienteId()).isEqualTo(primeiro.agendamento().getPacienteId());
        assertThat(pacientes.total()).isEqualTo(1);
        Paciente paciente = pacientes.buscarPaciente(primeiro.agendamento().getPacienteId()).orElseThrow();
        assertThat(paciente.cpf()).isEqualTo(CPF);
        assertThat(paciente.nomeCompleto()).isEqualTo("Maria");
        assertThat(paciente.telefone()).isEqualTo(Telefone.de("11987654321"));
    }

    @Test
    void reaproveitaPacientePeloTelefoneENomeIgnorandoAcentosEMaiusculas() {
        AtendimentoAgendado primeiro = useCase.agendar(novo("José da Conceição", null, "51993953661", null, null, FORA_DO_PRAZO));
        AtendimentoAgendado segundo = useCase.agendar(novo("  jose da  conceicao ", CPF, "51993953661", null, null, FORA_DO_PRAZO));

        assertThat(segundo.pacienteNovo()).isFalse();
        assertThat(segundo.agendamento().getPacienteId()).isEqualTo(primeiro.agendamento().getPacienteId());
        assertThat(pacientes.buscarPaciente(primeiro.agendamento().getPacienteId()).orElseThrow().cpf()).isEqualTo(CPF);
    }

    @Test
    void criaOutroPacienteQuandoOTelefoneEDeOutraPessoaDaFamilia() {
        useCase.agendar(novo("Maria", null, "51993953661", null, null, FORA_DO_PRAZO));
        AtendimentoAgendado filho = useCase.agendar(novo("Pedro", null, "51993953661", null, null, FORA_DO_PRAZO));

        assertThat(filho.pacienteNovo()).isTrue();
        assertThat(pacientes.total()).isEqualTo(2);
    }

    @Test
    void naoMisturaPacientesComMesmoNomeETelefoneMasCpfsDiferentes() {
        useCase.agendar(novo("Maria", CPF, "51993953661", null, null, FORA_DO_PRAZO));
        AtendimentoAgendado outra = useCase.agendar(novo("Maria", "11144477735", "51993953661", null, null, FORA_DO_PRAZO));

        assertThat(outra.pacienteNovo()).isTrue();
    }

    @Test
    void atualizaOCuidadorDoPacienteExistente() {
        AtendimentoAgendado primeiro = useCase.agendar(novo("Maria", CPF, "51993953661", null, null, FORA_DO_PRAZO));
        useCase.agendar(novo("Maria", CPF, "51993953661", "João", "11987654321", FORA_DO_PRAZO));

        Paciente paciente = pacientes.buscarPaciente(primeiro.agendamento().getPacienteId()).orElseThrow();
        assertThat(paciente.nomeCuidador()).isEqualTo("João");
        assertThat(paciente.telefoneCuidador()).isEqualTo(Telefone.de("11987654321"));
    }

    @Test
    void rejeitaCpfInvalido() {
        assertThatThrownBy(() -> useCase.agendar(novo("Maria", "123.456.789-00", "51993953661", null, null, FORA_DO_PRAZO)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CPF inválido");
        assertThatThrownBy(() -> useCase.agendar(novo("Maria", "111.111.111-11", "51993953661", null, null, FORA_DO_PRAZO)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void gravaPacienteEAgendamentoNaMesmaTransacao() {
        useCase.agendar(novo("51993953661", FORA_DO_PRAZO));

        assertThat(transacao.execucoes()).isEqualTo(1);
    }

    @Test
    void naoAbreTransacaoNemEnviaQuandoValidacaoFalha() {
        assertThatThrownBy(() -> useCase.agendar(novo("123", FORA_DO_PRAZO)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(transacao.execucoes()).isZero();
        assertThat(enviadas).isEmpty();
    }

    @Test
    void criaPacienteComCuidador() {
        AtendimentoAgendado agendado = useCase.agendar(novo("Maria", null, "51993953661", "João", "11987654321", FORA_DO_PRAZO));

        Paciente paciente = pacientes.buscarPaciente(agendado.agendamento().getPacienteId()).orElseThrow();
        assertThat(paciente.temCuidador()).isTrue();
        assertThat(paciente.nomeCuidador()).isEqualTo("João");
        assertThat(paciente.telefoneCuidador()).isEqualTo(Telefone.de("11987654321"));
        assertThat(agendado.confirmacao().envios()).hasSize(2);
    }

    @Test
    void indicaQualTelefoneEstaInvalido() {
        assertThatThrownBy(() -> useCase.agendar(novo("Maria", null, "51993953661", "João", "string", FORA_DO_PRAZO)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Telefone do cuidador inválido: \"string\"");
    }

    @Test
    void rejeitaDataNoPassado() {
        assertThatThrownBy(() -> useCase.agendar(novo("51993953661", LocalDateTime.of(2026, 11, 13, 8, 0))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futuro");
    }

    @Test
    void exigeNomeDoCuidadorQuandoHaTelefoneDoCuidador() {
        assertThatThrownBy(() -> useCase.agendar(novo("Maria", null, "51993953661", null, "11987654321", FORA_DO_PRAZO)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cuidador");
    }

    @Test
    void naoSalvaNadaQuandoInvalido() {
        assertThatThrownBy(() -> useCase.agendar(novo(" ", null, "51993953661", null, null, FORA_DO_PRAZO)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(agendamentos.agendamentosDoDia(FORA_DO_PRAZO.toLocalDate())).isEmpty();
    }
}
