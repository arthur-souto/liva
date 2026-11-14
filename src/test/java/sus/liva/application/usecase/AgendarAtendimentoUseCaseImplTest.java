package sus.liva.application.usecase;

import org.junit.jupiter.api.Test;
import sus.liva.application.port.in.NovoAtendimento;
import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.agendamento.StatusAgendamento;
import sus.liva.domain.model.agendamento.TipoAtendimento;
import sus.liva.domain.model.paciente.Paciente;
import sus.liva.infrastructure.adapter.out.memoria.AgendamentoMemoriaAdapter;
import sus.liva.infrastructure.adapter.out.memoria.PacienteMemoriaAdapter;
import sus.liva.infrastructure.adapter.out.memoria.TransacaoFake;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgendarAtendimentoUseCaseImplTest {

    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-13T12:00:00Z"), ZONA);
    private static final LocalDateTime FUTURO = LocalDateTime.of(2026, 11, 15, 9, 30);

    private final PacienteMemoriaAdapter pacientes = new PacienteMemoriaAdapter();
    private final AgendamentoMemoriaAdapter agendamentos = new AgendamentoMemoriaAdapter();
    private final TransacaoFake transacao = new TransacaoFake();
    private final AgendarAtendimentoUseCaseImpl useCase = new AgendarAtendimentoUseCaseImpl(pacientes, agendamentos, transacao, CLOCK);

    private static NovoAtendimento novo(String telefone, String nomeCuidador, String telefoneCuidador, LocalDateTime dataHora) {
        return new NovoAtendimento("Maria", telefone, nomeCuidador, telefoneCuidador, dataHora,
                TipoAtendimento.EXAME, "UBS Centro", "Rua A, 1", "Jejum");
    }

    @Test
    void criaPacienteEAgendamento() {
        Agendamento agendamento = useCase.agendar(novo("(51) 99395-3661", null, null, FUTURO));

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
    void gravaPacienteEAgendamentoNaMesmaTransacao() {
        useCase.agendar(novo("51993953661", null, null, FUTURO));

        assertThat(transacao.execucoes()).isEqualTo(1);
    }

    @Test
    void naoAbreTransacaoQuandoValidacaoFalha() {
        assertThatThrownBy(() -> useCase.agendar(novo("123", null, null, FUTURO)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(transacao.execucoes()).isZero();
    }

    @Test
    void criaPacienteComCuidador() {
        Agendamento agendamento = useCase.agendar(novo("51993953661", "João", "11987654321", FUTURO));

        Paciente paciente = pacientes.buscarPaciente(agendamento.getPacienteId()).orElseThrow();
        assertThat(paciente.temCuidador()).isTrue();
        assertThat(paciente.nomeCuidador()).isEqualTo("João");
        assertThat(paciente.telefoneCuidador()).isEqualTo(Telefone.de("11987654321"));
    }

    @Test
    void rejeitaTelefoneInvalido() {
        assertThatThrownBy(() -> useCase.agendar(novo("123", null, null, FUTURO)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void indicaQualTelefoneEstaInvalido() {
        assertThatThrownBy(() -> useCase.agendar(novo("51993953661", "João", "string", FUTURO)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Telefone do cuidador inválido: \"string\"");
    }

    @Test
    void rejeitaDataNoPassado() {
        assertThatThrownBy(() -> useCase.agendar(novo("51993953661", null, null, LocalDateTime.of(2026, 11, 13, 8, 0))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futuro");
    }

    @Test
    void exigeNomeDoCuidadorQuandoHaTelefoneDoCuidador() {
        assertThatThrownBy(() -> useCase.agendar(novo("51993953661", null, "11987654321", FUTURO)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cuidador");
    }

    @Test
    void naoSalvaNadaQuandoInvalido() {
        assertThatThrownBy(() -> useCase.agendar(new NovoAtendimento(" ", "51993953661", null, null, FUTURO,
                TipoAtendimento.CONSULTA, "UBS", "Rua", null))).isInstanceOf(IllegalArgumentException.class);

        assertThat(agendamentos.agendamentosDoDia(FUTURO.toLocalDate())).isEmpty();
    }
}
