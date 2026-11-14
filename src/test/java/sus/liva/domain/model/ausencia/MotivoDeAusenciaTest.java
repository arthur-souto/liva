package sus.liva.domain.model.ausencia;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MotivoDeAusenciaTest {

    @Test
    void entendeONumeroAPalavraEFrasesComuns() {
        assertThat(MotivoDeAusencia.interpretar("1")).contains(MotivoDeAusencia.ESQUECI);
        assertThat(MotivoDeAusencia.interpretar(" 2 - transporte")).contains(MotivoDeAusencia.TRANSPORTE);
        assertThat(MotivoDeAusencia.interpretar("Esqueci!")).contains(MotivoDeAusencia.ESQUECI);
        assertThat(MotivoDeAusencia.interpretar("Sem dinheiro pro ônibus")).contains(MotivoDeAusencia.TRANSPORTE);
        assertThat(MotivoDeAusencia.interpretar("já melhorei")).contains(MotivoDeAusencia.NAO_PRECISA);
        assertThat(MotivoDeAusencia.interpretar("6")).contains(MotivoDeAusencia.OUTRO);
    }

    @Test
    void ignoraTextoSemMotivoReconhecivel() {
        assertThat(MotivoDeAusencia.interpretar("tá complicado")).isEmpty();
        assertThat(MotivoDeAusencia.interpretar("7")).isEmpty();
        assertThat(MotivoDeAusencia.interpretar("")).isEmpty();
        assertThat(MotivoDeAusencia.interpretar(null)).isEmpty();
    }
}
