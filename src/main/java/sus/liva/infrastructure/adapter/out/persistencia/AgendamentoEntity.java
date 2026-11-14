package sus.liva.infrastructure.adapter.out.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.agendamento.StatusAgendamento;
import sus.liva.domain.model.agendamento.TipoAtendimento;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "agendamentos")
public class AgendamentoEntity {
    @Id
    private UUID id;
    @Column(name = "paciente_id", nullable = false)
    private UUID pacienteId;
    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoAtendimento tipo;
    @Column(nullable = false, length = 150)
    private String unidade;
    @Column(nullable = false, length = 255)
    private String endereco;
    @Column(length = 500)
    private String preparo;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusAgendamento status;

    protected AgendamentoEntity() {
    }

    static AgendamentoEntity de(Agendamento a) {
        AgendamentoEntity e = new AgendamentoEntity();
        e.id = a.getId();
        e.pacienteId = a.getPacienteId();
        e.dataHora = a.getDataHora();
        e.tipo = a.getTipo();
        e.unidade = a.getUnidade();
        e.endereco = a.getEndereco();
        e.preparo = a.getPreparo();
        e.status = a.getStatus();
        return e;
    }

    Agendamento paraDominio() {
        return new Agendamento(status, preparo, endereco, unidade, tipo, dataHora, pacienteId, id);
    }
}
