package sus.liva.domain.model.encaminhamento;

import org.junit.jupiter.api.Test;
import sus.liva.domain.model.triagem.AvaliacaoClinica;
import sus.liva.domain.model.triagem.Prioridade;
import sus.liva.domain.model.triagem.SinaisVitais;
import sus.liva.domain.model.triagem.Triagem;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncaminhamentoTest {

    private static final Instant AGORA = Instant.parse("2026-11-14T12:00:00Z");
    private static final UUID CLINICO = UUID.randomUUID();

    private final Triagem triagem = triagemEmAndamento();

    private static Triagem triagemEmAndamento() {
        Triagem t = Triagem.registrarChegada(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), AGORA);
        t.iniciar(CLINICO, AGORA);
        return t;
    }

    private Encaminhamento eletivo() {
        return Encaminhamento.aPartirDe(UUID.randomUUID(), triagem, "ORTOPEDIA", null, Modalidade.ELETIVO,
                Prioridade.POUCO_URGENTE, "Avaliar joelho", CLINICO, AGORA);
    }

    @Test
    void herdaOPacienteDaTriagem() {
        Encaminhamento e = eletivo();

        assertThat(e.getPacienteId()).isEqualTo(triagem.getPacienteId());
        assertThat(e.getTriagemId()).isEqualTo(triagem.getId());
        assertThat(e.getStatus()).isEqualTo(StatusEncaminhamento.PENDENTE);
    }

    @Test
    void soPodeSerAgendadoDepoisDeLiberado() {
        Encaminhamento e = eletivo();

        assertThat(e.permiteAgendamento()).isFalse();
        assertThatThrownBy(e::consultaAgendada).isInstanceOf(IllegalStateException.class);

        e.liberarParaAgendamento();
        e.consultaAgendada();

        assertThat(e.getStatus()).isEqualTo(StatusEncaminhamento.AGENDADO);
        assertThatThrownBy(e::consultaAgendada).isInstanceOf(IllegalStateException.class);
        assertThat(e.permiteRemarcacao()).isTrue();
        assertThat(e.permiteAgendamento()).isFalse();
    }

    @Test
    void cicloDeConsultaComRetornoEConclusao() {
        Encaminhamento e = eletivo();
        e.liberarParaAgendamento();
        e.consultaAgendada();
        e.consultaIniciada();
        e.consultaFinalizada(true);

        assertThat(e.permiteAgendamento()).isTrue();

        e.consultaAgendada();
        e.consultaIniciada();
        e.consultaFinalizada(false);

        assertThat(e.getStatus()).isEqualTo(StatusEncaminhamento.EM_ATENDIMENTO);

        e.concluir("Alta ortopédica", AGORA);

        assertThat(e.getStatus()).isEqualTo(StatusEncaminhamento.CONCLUIDO);
        assertThatThrownBy(e::autorizarRetorno).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void imediatoExigePrioridadeGraveENaoEntraNaAgenda() {
        assertThatThrownBy(() -> Encaminhamento.aPartirDe(UUID.randomUUID(), triagem, "PRONTO_ATENDIMENTO", null,
                Modalidade.IMEDIATO, Prioridade.URGENTE, "Dor", CLINICO, AGORA)).isInstanceOf(IllegalArgumentException.class);

        Encaminhamento imediato = Encaminhamento.aPartirDe(UUID.randomUUID(), triagem, "PRONTO_ATENDIMENTO", null,
                Modalidade.IMEDIATO, Prioridade.EMERGENCIA, "Fratura exposta", CLINICO, AGORA);
        imediato.liberarParaAgendamento();

        assertThat(imediato.permiteAgendamento()).isFalse();

        UUID plantonista = UUID.randomUUID();
        imediato.receberNoPlantao(plantonista, AGORA);

        assertThat(imediato.getStatus()).isEqualTo(StatusEncaminhamento.EM_ATENDIMENTO);
        assertThat(imediato.destinadoA(plantonista)).isTrue();
        assertThatThrownBy(() -> imediato.receberNoPlantao(plantonista, AGORA)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void naoEncaminhaDeTriagemFinalizada() {
        triagem.registrarAvaliacao(CLINICO, new AvaliacaoClinica("Dor", null, null, null, null), SinaisVitais.naoColetados(),
                Prioridade.NAO_URGENTE, null);
        triagem.finalizar(CLINICO, AGORA);

        assertThatThrownBy(this::eletivo).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void naoCancelaEncaminhamentoComConsultaAgendada() {
        Encaminhamento e = eletivo();
        e.liberarParaAgendamento();
        e.consultaAgendada();

        assertThatThrownBy(() -> e.cancelar("desistiu", AGORA)).isInstanceOf(IllegalStateException.class);
    }
}
