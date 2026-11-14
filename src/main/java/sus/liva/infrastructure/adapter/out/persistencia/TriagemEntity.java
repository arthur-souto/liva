package sus.liva.infrastructure.adapter.out.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import sus.liva.domain.model.triagem.AvaliacaoClinica;
import sus.liva.domain.model.triagem.Prioridade;
import sus.liva.domain.model.triagem.SinaisVitais;
import sus.liva.domain.model.triagem.StatusTriagem;
import sus.liva.domain.model.triagem.Triagem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "triagens")
public class TriagemEntity {
    @Id
    private UUID id;
    @Column(name = "paciente_id", nullable = false, updatable = false)
    private UUID pacienteId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusTriagem status;
    @Column(name = "registrada_por", nullable = false, updatable = false)
    private UUID registradaPor;
    @Column(name = "chegada_em", nullable = false, updatable = false)
    private Instant chegadaEm;
    @Column(name = "profissional_id")
    private UUID profissionalId;
    @Column(name = "iniciada_em")
    private Instant iniciadaEm;
    @Column(name = "finalizada_em")
    private Instant finalizadaEm;
    @Column(name = "encerrada_em")
    private Instant encerradaEm;
    @Column(name = "cancelada_em")
    private Instant canceladaEm;
    @Column(name = "motivo_cancelamento", length = 1000)
    private String motivoCancelamento;
    @Column(name = "queixa_principal", length = 1000)
    private String queixaPrincipal;
    @Column(length = 2000)
    private String sintomas;
    @Column(name = "historico_clinico", length = 2000)
    private String historicoClinico;
    @Column(length = 2000)
    private String antecedentes;
    @Column(length = 2000)
    private String observacoes;
    @Column(name = "pressao_sistolica")
    private Integer pressaoSistolica;
    @Column(name = "pressao_diastolica")
    private Integer pressaoDiastolica;
    @Column(name = "frequencia_cardiaca")
    private Integer frequenciaCardiaca;
    @Column(name = "frequencia_respiratoria")
    private Integer frequenciaRespiratoria;
    @Column(precision = 4, scale = 1)
    private BigDecimal temperatura;
    private Integer saturacao;
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Prioridade prioridade;
    @Column(name = "justificativa_prioridade", length = 1000)
    private String justificativaPrioridade;
    @Version
    private Long version;

    protected TriagemEntity() {
    }

    static TriagemEntity nova(Triagem t) {
        TriagemEntity e = new TriagemEntity();
        e.id = t.getId();
        e.pacienteId = t.getPacienteId();
        e.registradaPor = t.getRegistradaPor();
        e.chegadaEm = t.getChegadaEm();
        e.copiar(t);
        return e;
    }

    void copiar(Triagem t) {
        Restricoes.conferirVersao(t.getVersao(), version, TriagemEntity.class, id);
        status = t.getStatus();
        profissionalId = t.getProfissionalId();
        iniciadaEm = t.getIniciadaEm();
        finalizadaEm = t.getFinalizadaEm();
        encerradaEm = t.getEncerradaEm();
        canceladaEm = t.getCanceladaEm();
        motivoCancelamento = t.getMotivoCancelamento();
        AvaliacaoClinica a = t.getAvaliacao();
        queixaPrincipal = a.queixaPrincipal();
        sintomas = a.sintomas();
        historicoClinico = a.historicoClinico();
        antecedentes = a.antecedentes();
        observacoes = a.observacoes();
        SinaisVitais s = t.getSinaisVitais();
        pressaoSistolica = s.pressaoSistolica();
        pressaoDiastolica = s.pressaoDiastolica();
        frequenciaCardiaca = s.frequenciaCardiaca();
        frequenciaRespiratoria = s.frequenciaRespiratoria();
        temperatura = s.temperatura();
        saturacao = s.saturacao();
        prioridade = t.getPrioridade();
        justificativaPrioridade = t.getJustificativaPrioridade();
    }

    Triagem paraDominio() {
        return new Triagem(id, pacienteId, registradaPor, chegadaEm, status, profissionalId, iniciadaEm, finalizadaEm,
                encerradaEm, canceladaEm, motivoCancelamento,
                new AvaliacaoClinica(queixaPrincipal, sintomas, historicoClinico, antecedentes, observacoes),
                new SinaisVitais(pressaoSistolica, pressaoDiastolica, frequenciaCardiaca, frequenciaRespiratoria, temperatura, saturacao),
                prioridade, justificativaPrioridade, version);
    }
}
