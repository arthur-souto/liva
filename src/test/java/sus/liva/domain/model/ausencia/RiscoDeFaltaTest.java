package sus.liva.domain.model.ausencia;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RiscoDeFaltaTest {

    @Test
    void historicoDeFaltasEFaltaDeContatoTornamORiscoAlto() {
        RiscoDeFalta risco = RiscoDeFalta.avaliar(new SinaisDeRisco(2, 1, 0, false, true, true, 45, false, false));

        assertThat(risco.nivel()).isEqualTo(NivelDeRisco.ALTO);
        assertThat(risco.pontos()).isEqualTo(100);
        assertThat(risco.fatores()).containsExactly("Faltou 2 vezes nos últimos 12 meses", "O WhatsApp não recebeu as mensagens",
                "Não respondeu ao pedido de confirmação", "Marcada há mais de 30 dias");
    }

    @Test
    void confirmarPresencaDerrubaORisco() {
        RiscoDeFalta risco = RiscoDeFalta.avaliar(new SinaisDeRisco(1, 0, 0, true, false, false, 10, false, false));

        assertThat(risco.nivel()).isEqualTo(NivelDeRisco.BAIXO);
        assertThat(risco.pontos()).isZero();
        assertThat(risco.fatores()).containsExactly("Faltou 1 vez nos últimos 12 meses", "Confirmou presença");
    }

    @Test
    void quemSempreCompareceTemRiscoBaixoEOsLimitesDosNiveisSaoRespeitados() {
        assertThat(RiscoDeFalta.avaliar(new SinaisDeRisco(0, 4, 0, false, false, false, 5, false, false)).nivel()).isEqualTo(NivelDeRisco.BAIXO);
        assertThat(RiscoDeFalta.avaliar(new SinaisDeRisco(1, 0, 0, false, false, false, null, false, false)).nivel()).isEqualTo(NivelDeRisco.MEDIO);
        assertThat(RiscoDeFalta.avaliar(new SinaisDeRisco(1, 0, 2, false, false, false, null, false, false)).pontos()).isEqualTo(35);
        assertThat(RiscoDeFalta.avaliar(new SinaisDeRisco(1, 0, 0, false, true, false, null, true, false)).nivel()).isEqualTo(NivelDeRisco.ALTO);
    }
}
