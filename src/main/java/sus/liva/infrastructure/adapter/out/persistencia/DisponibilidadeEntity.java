package sus.liva.infrastructure.adapter.out.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import sus.liva.domain.model.profissional.Disponibilidade;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "disponibilidades")
public class DisponibilidadeEntity {
    @Id
    private UUID id;
    @Column(name = "profissional_id", nullable = false)
    private UUID profissionalId;
    @Column(name = "dia_semana", nullable = false)
    private short diaSemana;
    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;
    @Column(name = "hora_fim", nullable = false)
    private LocalTime horaFim;
    @Column(name = "duracao_minutos", nullable = false)
    private int duracaoMinutos;

    protected DisponibilidadeEntity() {
    }

    static DisponibilidadeEntity de(Disponibilidade d) {
        DisponibilidadeEntity e = new DisponibilidadeEntity();
        e.id = d.id();
        e.profissionalId = d.profissionalId();
        e.diaSemana = (short) d.diaSemana().getValue();
        e.horaInicio = d.inicio();
        e.horaFim = d.fim();
        e.duracaoMinutos = d.duracaoMinutos();
        return e;
    }

    Disponibilidade paraDominio() {
        return new Disponibilidade(id, profissionalId, DayOfWeek.of(diaSemana), horaInicio, horaFim, duracaoMinutos);
    }
}
