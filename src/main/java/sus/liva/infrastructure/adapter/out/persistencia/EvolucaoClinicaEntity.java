package sus.liva.infrastructure.adapter.out.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import sus.liva.domain.model.agendamento.EvolucaoClinica;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "evolucoes_clinicas")
public class EvolucaoClinicaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "agendamento_id", nullable = false, updatable = false)
    private UUID agendamentoId;
    @Column(name = "autor_id", nullable = false, updatable = false)
    private UUID autorId;
    @Column(nullable = false, length = 4000, updatable = false)
    private String texto;
    @Column(name = "registrada_em", nullable = false, updatable = false)
    private Instant registradaEm;

    protected EvolucaoClinicaEntity() {
    }

    static EvolucaoClinicaEntity de(EvolucaoClinica e) {
        EvolucaoClinicaEntity n = new EvolucaoClinicaEntity();
        n.agendamentoId = e.agendamentoId();
        n.autorId = e.autorId();
        n.texto = e.texto();
        n.registradaEm = e.registradaEm();
        return n;
    }

    EvolucaoClinica paraDominio() {
        return new EvolucaoClinica(agendamentoId, autorId, texto, registradaEm);
    }
}
