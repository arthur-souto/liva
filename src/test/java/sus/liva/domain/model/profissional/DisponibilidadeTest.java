package sus.liva.domain.model.profissional;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DisponibilidadeTest {

    private static final LocalDate SEGUNDA = LocalDate.of(2026, 11, 16);
    private final Disponibilidade manha = new Disponibilidade(UUID.randomUUID(), UUID.randomUUID(), DayOfWeek.MONDAY,
            LocalTime.of(8, 0), LocalTime.of(10, 0), 40);

    @Test
    void geraHorariosQueCabemNaJanela() {
        assertThat(manha.horariosEm(SEGUNDA)).containsExactly(
                SEGUNDA.atTime(8, 0), SEGUNDA.atTime(8, 40), SEGUNDA.atTime(9, 20));
    }

    @Test
    void naoGeraHorariosEmOutroDia() {
        assertThat(manha.horariosEm(SEGUNDA.plusDays(1))).isEmpty();
    }

    @Test
    void comportaApenasHorariosAlinhados() {
        assertThat(manha.comporta(SEGUNDA.atTime(8, 40))).isTrue();
        assertThat(manha.comporta(SEGUNDA.atTime(8, 30))).isFalse();
        assertThat(manha.comporta(LocalDateTime.of(SEGUNDA, LocalTime.of(10, 0)))).isFalse();
    }
}
