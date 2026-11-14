package sus.liva.application.usecase;

import org.junit.jupiter.api.Test;
import sus.liva.application.port.in.AgendamentoDetalhado;
import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.agendamento.StatusAgendamento;
import sus.liva.domain.model.agendamento.TipoAtendimento;
import sus.liva.domain.model.paciente.Paciente;
import sus.liva.domain.model.paciente.Sexo;
import sus.liva.infrastructure.adapter.out.memoria.AgendamentoMemoriaAdapter;
import sus.liva.infrastructure.adapter.out.memoria.PacienteMemoriaAdapter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ListarAgendamentosUseCaseImplTest {

    private static final LocalDate DIA = LocalDate.of(2026, 11, 16);

    private final AgendamentoMemoriaAdapter agendamentos = new AgendamentoMemoriaAdapter();
    private final PacienteMemoriaAdapter pacientes = new PacienteMemoriaAdapter();
    private final ListarAgendamentosUseCaseImpl useCase = new ListarAgendamentosUseCaseImpl(agendamentos, pacientes);
    private final Paciente maria = new Paciente(UUID.randomUUID(), "Maria", null, null, null, null, Sexo.FEMININO, null,
            Telefone.de("51993953661"), null, null, null, null, null, null, true);

    @Test
    void listaOPeriodoEmOrdemComOPaciente() {
        pacientes.salvarPaciente(maria);
        Agendamento tarde = salvar(StatusAgendamento.CONFIRMADO, DIA.atTime(15, 0), maria.id());
        Agendamento manha = salvar(StatusAgendamento.AGENDADO, DIA.atTime(8, 0), maria.id());
        salvar(StatusAgendamento.AGENDADO, DIA.plusDays(1).atTime(8, 0), maria.id());

        List<AgendamentoDetalhado> lista = useCase.listar(DIA, DIA, Set.of());

        assertThat(lista).extracting(AgendamentoDetalhado::agendamento).containsExactly(manha, tarde);
        assertThat(lista).allSatisfy(d -> assertThat(d.paciente()).isEqualTo(maria));
    }

    @Test
    void filtraPorStatus() {
        salvar(StatusAgendamento.CANCELADO, DIA.atTime(8, 0), maria.id());
        salvar(StatusAgendamento.CONFIRMADO, DIA.atTime(9, 0), maria.id());

        assertThat(useCase.listar(DIA, DIA, Set.of(StatusAgendamento.CANCELADO)))
                .singleElement().matches(d -> d.agendamento().getStatus() == StatusAgendamento.CANCELADO);
        assertThat(useCase.listar(DIA, DIA, null)).hasSize(2);
    }

    @Test
    void mantemOAgendamentoMesmoSemPacienteCadastrado() {
        salvar(StatusAgendamento.AGENDADO, DIA.atTime(8, 0), UUID.randomUUID());

        assertThat(useCase.listar(DIA, DIA, Set.of())).singleElement().matches(d -> d.paciente() == null);
    }

    @Test
    void validaOPeriodo() {
        assertThatThrownBy(() -> useCase.listar(DIA, DIA.minusDays(1), Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.listar(DIA, DIA.plusDays(31), Set.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("31 dias");
        assertThat(useCase.listar(DIA, DIA.plusDays(30), Set.of())).isEmpty();
    }

    private Agendamento salvar(StatusAgendamento status, LocalDateTime dataHora, UUID pacienteId) {
        Agendamento a = new Agendamento(status, null, "Rua A", "UBS Centro", TipoAtendimento.CONSULTA, dataHora,
                pacienteId, UUID.randomUUID());
        agendamentos.salvarAgendamento(a);
        return a;
    }
}
