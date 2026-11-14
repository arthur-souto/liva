package sus.liva.domain.model.agendamento;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class RespostaPacienteTest {

    @ParameterizedTest
    @ValueSource(strings = {"1", " 1 ", "1 - Sim, vou", "Sim", "SIM!", "sim vou", "Confirmo"})
    void reconheceConfirmacao(String texto) {
        assertThat(RespostaPaciente.interpretar(texto)).contains(RespostaPaciente.CONFIRMAR);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2", "Não", "nao vou conseguir", "Não vou", "cancelar"})
    void reconheceCancelamento(String texto) {
        assertThat(RespostaPaciente.interpretar(texto)).contains(RespostaPaciente.CANCELAR);
    }

    @ParameterizedTest
    @ValueSource(strings = {"3", "Quero outro horário", "remarcar"})
    void reconheceReagendamento(String texto) {
        assertThat(RespostaPaciente.interpretar(texto)).contains(RespostaPaciente.REAGENDAR);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "4", "talvez", "Olá, Maria da Silva! Você tem atendimento", "sim mas atrasado"})
    void naoReconheceOutrosTextos(String texto) {
        assertThat(RespostaPaciente.interpretar(texto)).isEmpty();
    }
}
