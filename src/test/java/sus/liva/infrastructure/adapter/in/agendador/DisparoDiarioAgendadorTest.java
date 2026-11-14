package sus.liva.infrastructure.adapter.in.agendador;

import org.junit.jupiter.api.Test;
import sus.liva.application.port.in.DispararLembretesDoDiaUseCase;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class DisparoDiarioAgendadorTest {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    @Test
    void usaADataDoFusoConfiguradoENaoADoServidor() {
        List<LocalDate> chamadas = new ArrayList<>();
        Clock duasDaManhaUtc = Clock.fixed(Instant.parse("2026-11-14T02:00:00Z"), SAO_PAULO);

        DisparoDiarioAgendador agendador = new DisparoDiarioAgendador(dia -> {
            chamadas.add(dia);
            return 0;
        }, duasDaManhaUtc);

        agendador.executar();

        assertThat(chamadas).containsExactly(LocalDate.of(2026, 11, 13));
    }

    @Test
    void naoPropagaFalhaDaRotina() {
        DispararLembretesDoDiaUseCase comFalha = dia -> {
            throw new IllegalStateException("banco fora do ar");
        };
        DisparoDiarioAgendador agendador = new DisparoDiarioAgendador(comFalha, Clock.system(SAO_PAULO));

        assertThatCode(agendador::executar).doesNotThrowAnyException();
    }
}
