package sus.liva.infrastructure.adapter.out.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import sus.liva.domain.model.encaminhamento.Encaminhamento;
import sus.liva.domain.model.encaminhamento.Modalidade;
import sus.liva.domain.model.encaminhamento.StatusEncaminhamento;
import sus.liva.domain.model.triagem.Prioridade;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "encaminhamentos")
public class EncaminhamentoEntity {
    @Id
    private UUID id;
    @Column(name = "triagem_id", nullable = false, updatable = false)
    private UUID triagemId;
    @Column(name = "paciente_id", nullable = false, updatable = false)
    private UUID pacienteId;
    @Column(name = "especialidade_codigo", nullable = false, updatable = false, length = 40)
    private String especialidade;
    @Column(name = "profissional_destino_id", updatable = false)
    private UUID profissionalDestinoId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private Modalidade modalidade;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private Prioridade prioridade;
    @Column(nullable = false, updatable = false, length = 1000)
    private String justificativa;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusEncaminhamento status;
    @Column(name = "criado_por", nullable = false, updatable = false)
    private UUID criadoPor;
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;
    @Column(name = "recebido_por")
    private UUID recebidoPor;
    @Column(name = "recebido_em")
    private Instant recebidoEm;
    @Column(name = "encerrado_em")
    private Instant encerradoEm;
    @Column(length = 2000)
    private String desfecho;
    @Version
    private Long version;

    protected EncaminhamentoEntity() {
    }

    static EncaminhamentoEntity novo(Encaminhamento e) {
        EncaminhamentoEntity n = new EncaminhamentoEntity();
        n.id = e.getId();
        n.triagemId = e.getTriagemId();
        n.pacienteId = e.getPacienteId();
        n.especialidade = e.getEspecialidade();
        n.profissionalDestinoId = e.getProfissionalDestinoId();
        n.modalidade = e.getModalidade();
        n.prioridade = e.getPrioridade();
        n.justificativa = e.getJustificativa();
        n.criadoPor = e.getCriadoPor();
        n.criadoEm = e.getCriadoEm();
        n.copiar(e);
        return n;
    }

    void copiar(Encaminhamento e) {
        Restricoes.conferirVersao(e.getVersao(), version, EncaminhamentoEntity.class, id);
        status = e.getStatus();
        recebidoPor = e.getRecebidoPor();
        recebidoEm = e.getRecebidoEm();
        encerradoEm = e.getEncerradoEm();
        desfecho = e.getDesfecho();
    }

    Encaminhamento paraDominio() {
        return new Encaminhamento(id, triagemId, pacienteId, especialidade, profissionalDestinoId, modalidade, prioridade,
                justificativa, status, criadoPor, criadoEm, recebidoPor, recebidoEm, encerradoEm, desfecho, version);
    }
}
