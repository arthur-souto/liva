package sus.liva.domain.model.profissional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record Disponibilidade(
        UUID id,
        UUID profissionalId,
        DayOfWeek diaSemana,
        LocalTime inicio,
        LocalTime fim,
        int duracaoMinutos
) {
    public List<LocalDateTime> horariosEm(LocalDate data) {
        List<LocalDateTime> horarios = new ArrayList<>();
        if (data.getDayOfWeek() != diaSemana) return horarios;

        for (LocalTime h = inicio; !h.plusMinutes(duracaoMinutos).isAfter(fim) && !h.isBefore(inicio); h = h.plusMinutes(duracaoMinutos))
            horarios.add(data.atTime(h));
        return horarios;
    }

    public boolean comporta(LocalDateTime horario) {
        return horariosEm(horario.toLocalDate()).contains(horario);
    }
}
