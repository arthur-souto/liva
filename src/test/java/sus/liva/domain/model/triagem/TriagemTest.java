package sus.liva.domain.model.triagem;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TriagemTest {

    private static final Instant AGORA = Instant.parse("2026-11-14T12:00:00Z");
    private static final UUID CLINICO = UUID.randomUUID();
    private static final AvaliacaoClinica AVALIACAO = new AvaliacaoClinica("Dor no joelho", "Inchaço", null, null, null);

    private Triagem emAndamento() {
        Triagem t = Triagem.registrarChegada(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), AGORA);
        t.iniciar(CLINICO, AGORA);
        return t;
    }

    @Test
    void percorreOFluxoAteEncerrar() {
        Triagem t = emAndamento();
        t.registrarAvaliacao(CLINICO, AVALIACAO, SinaisVitais.naoColetados(), Prioridade.POUCO_URGENTE, null);
        t.finalizar(CLINICO, AGORA);
        t.encerrar(AGORA);

        assertThat(t.getStatus()).isEqualTo(StatusTriagem.ENCERRADA);
        assertThat(t.getStatus().concluida()).isTrue();
    }

    @Test
    void naoIniciaDuasVezes() {
        Triagem t = emAndamento();

        assertThatThrownBy(() -> t.iniciar(UUID.randomUUID(), AGORA)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void naoFinalizaSemQueixaOuPrioridade() {
        Triagem t = emAndamento();
        assertThatThrownBy(() -> t.finalizar(CLINICO, AGORA)).hasMessageContaining("queixa principal");

        t.registrarAvaliacao(CLINICO, AVALIACAO, SinaisVitais.naoColetados(), null, null);
        assertThatThrownBy(() -> t.finalizar(CLINICO, AGORA)).hasMessageContaining("prioridade");
    }

    @Test
    void prioridadeGraveExigeJustificativa() {
        Triagem t = emAndamento();

        assertThatThrownBy(() -> t.registrarAvaliacao(CLINICO, AVALIACAO, SinaisVitais.naoColetados(), Prioridade.EMERGENCIA, " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void somenteOResponsavelAlteraATriagem() {
        Triagem t = emAndamento();

        assertThatThrownBy(() -> t.registrarAvaliacao(UUID.randomUUID(), AVALIACAO, SinaisVitais.naoColetados(), Prioridade.URGENTE, null))
                .hasMessageContaining("outro profissional");
    }

    @Test
    void triagemFinalizadaNaoEhAlteradaSomenteRetificada() {
        Triagem t = emAndamento();
        t.registrarAvaliacao(CLINICO, AVALIACAO, SinaisVitais.naoColetados(), Prioridade.URGENTE, null);
        t.finalizar(CLINICO, AGORA);

        assertThatThrownBy(() -> t.registrarAvaliacao(CLINICO, AVALIACAO, SinaisVitais.naoColetados(), Prioridade.NAO_URGENTE, null))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> t.cancelar("engano", AGORA)).isInstanceOf(IllegalStateException.class);
        Retificacao r = t.retificar(CLINICO, "Dor no joelho esquerdo", "lateralidade", AGORA);
        assertThat(r.texto()).isEqualTo("Dor no joelho esquerdo");
        assertThat(t.getAvaliacao()).isEqualTo(AVALIACAO);
    }

    @Test
    void naoRetificaTriagemEmAndamento() {
        assertThatThrownBy(() -> emAndamento().retificar(CLINICO, "x", "y", AGORA)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void naoEncerraTriagemQueNaoFoiFinalizada() {
        assertThatThrownBy(() -> emAndamento().encerrar(AGORA)).isInstanceOf(IllegalStateException.class);
    }
}
