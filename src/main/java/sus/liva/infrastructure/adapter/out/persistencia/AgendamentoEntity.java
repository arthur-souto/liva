package sus.liva.infrastructure.adapter.out.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.agendamento.NaturezaConsulta;
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
    @Column(name = "triagem_id")
    private UUID triagemId;
    @Column(name = "encaminhamento_id")
    private UUID encaminhamentoId;
    @Column(name = "profissional_id")
    private UUID profissionalId;
    @Column(name = "duracao_minutos", nullable = false)
    private int duracaoMinutos;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NaturezaConsulta natureza;
    @Column(name = "chave_idempotencia", length = 64)
    private String chaveIdempotencia;
    @Version
    private Long version;

    protected AgendamentoEntity() {
    }

    static AgendamentoEntity novo(Agendamento a) {
        AgendamentoEntity e = new AgendamentoEntity();
        e.id = a.getId();
        e.copiar(a);
        return e;
    }

    void copiar(Agendamento a) {
        Restricoes.conferirVersao(a.getVersao(), version, AgendamentoEntity.class, id);
        pacienteId = a.getPacienteId();
        dataHora = a.getDataHora();
        tipo = a.getTipo();
        unidade = a.getUnidade();
        endereco = a.getEndereco();
        preparo = a.getPreparo();
        status = a.getStatus();
        triagemId = a.getTriagemId();
        encaminhamentoId = a.getEncaminhamentoId();
        profissionalId = a.getProfissionalId();
        duracaoMinutos = a.getDuracaoMinutos();
        natureza = a.getNatureza();
        chaveIdempotencia = a.getChaveIdempotencia();
    }

    Agendamento paraDominio() {
        return new Agendamento(id, pacienteId, dataHora, tipo, unidade, endereco, preparo, status, triagemId,
                encaminhamentoId, profissionalId, duracaoMinutos, natureza, chaveIdempotencia, version);
    }
}
