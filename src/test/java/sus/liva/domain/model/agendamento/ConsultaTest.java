package sus.liva.domain.model.agendamento;

import org.junit.jupiter.api.Test;
import sus.liva.domain.model.encaminhamento.Encaminhamento;
import sus.liva.domain.model.encaminhamento.Modalidade;
import sus.liva.domain.model.profissional.Profissional;
import sus.liva.domain.model.profissional.TipoProfissional;
import sus.liva.domain.model.triagem.Prioridade;
import sus.liva.domain.model.triagem.Triagem;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsultaTest {

    private static final Instant AGORA = Instant.parse("2026-11-14T12:00:00Z");
    private static final LocalDateTime HORARIO = LocalDateTime.of(2026, 11, 20, 9, 0);
    private final Profissional ortopedista = especialista("ORTOPEDIA");
    private final Encaminhamento encaminhamento = encaminhamento();

    private static Profissional especialista(String especialidade) {
        return new Profissional(UUID.randomUUID(), "Dra. Ana", "CRM-" + UUID.randomUUID(), TipoProfissional.ESPECIALISTA,
                especialidade, "Policlínica", "Rua B, 2", true);
    }

    private static Encaminhamento encaminhamento() {
        UUID clinico = UUID.randomUUID();
        Triagem t = Triagem.registrarChegada(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), AGORA);
        t.iniciar(clinico, AGORA);
        return Encaminhamento.aPartirDe(UUID.randomUUID(), t, "ORTOPEDIA", null, Modalidade.ELETIVO,
                Prioridade.POUCO_URGENTE, "Joelho", clinico, AGORA);
    }

    @Test
    void consultaFicaVinculadaAoRamoDoEncaminhamento() {
        Agendamento c = Agendamento.consulta(UUID.randomUUID(), encaminhamento, ortopedista, HORARIO, 30, NaturezaConsulta.INICIAL, null);

        assertThat(c.getTriagemId()).isEqualTo(encaminhamento.getTriagemId());
        assertThat(c.getPacienteId()).isEqualTo(encaminhamento.getPacienteId());
        assertThat(c.getEncaminhamentoId()).isEqualTo(encaminhamento.getId());
        assertThat(c.getFim()).isEqualTo(HORARIO.plusMinutes(30));
        assertThat(c.getStatus().ocupaAgenda()).isTrue();
    }

    @Test
    void recusaEspecialistaDeOutraEspecialidade() {
        assertThatThrownBy(() -> Agendamento.consulta(UUID.randomUUID(), encaminhamento, especialista("CARDIOLOGIA"),
                HORARIO, 30, NaturezaConsulta.INICIAL, null)).hasMessageContaining("especialidade");
    }

    @Test
    void transicoesDaConsulta() {
        Agendamento c = Agendamento.consulta(UUID.randomUUID(), encaminhamento, ortopedista, HORARIO, 30, NaturezaConsulta.INICIAL, null);
        assertThatThrownBy(c::finalizarAtendimento).isInstanceOf(IllegalStateException.class);

        c.iniciarAtendimento();
        assertThatThrownBy(c::cancelar).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(c::registrarFalta).isInstanceOf(IllegalStateException.class);

        c.finalizarAtendimento();
        assertThat(c.getStatus()).isEqualTo(StatusAgendamento.COMPARECEU);
        assertThat(c.getStatus().ocupaAgenda()).isFalse();
        assertThatThrownBy(c::iniciarAtendimento).isInstanceOf(IllegalStateException.class);
    }
}
