package sus.liva.domain.model.encaminhamento;

import sus.liva.domain.model.triagem.Prioridade;
import sus.liva.domain.model.triagem.Triagem;

import java.time.Instant;
import java.util.UUID;

public class Encaminhamento {
    private final UUID id;
    private final UUID triagemId;
    private final UUID pacienteId;
    private final String especialidade;
    private final UUID profissionalDestinoId;
    private final Modalidade modalidade;
    private final Prioridade prioridade;
    private final String justificativa;
    private final UUID criadoPor;
    private final Instant criadoEm;
    private final Long versao;
    private StatusEncaminhamento status;
    private UUID recebidoPor;
    private Instant recebidoEm;
    private Instant encerradoEm;
    private String desfecho;

    public Encaminhamento(UUID id, UUID triagemId, UUID pacienteId, String especialidade, UUID profissionalDestinoId,
                          Modalidade modalidade, Prioridade prioridade, String justificativa, StatusEncaminhamento status,
                          UUID criadoPor, Instant criadoEm, UUID recebidoPor, Instant recebidoEm, Instant encerradoEm,
                          String desfecho, Long versao) {
        this.id = id;
        this.triagemId = triagemId;
        this.pacienteId = pacienteId;
        this.especialidade = especialidade;
        this.profissionalDestinoId = profissionalDestinoId;
        this.modalidade = modalidade;
        this.prioridade = prioridade;
        this.justificativa = justificativa;
        this.status = status;
        this.criadoPor = criadoPor;
        this.criadoEm = criadoEm;
        this.recebidoPor = recebidoPor;
        this.recebidoEm = recebidoEm;
        this.encerradoEm = encerradoEm;
        this.desfecho = desfecho;
        this.versao = versao;
    }

    public static Encaminhamento aPartirDe(UUID id, Triagem triagem, String especialidade, UUID profissionalDestinoId,
                                            Modalidade modalidade, Prioridade prioridade, String justificativa,
                                            UUID criadoPor, Instant em) {
        if (!triagem.aceitaEncaminhamento())
            throw new IllegalStateException("Encaminhamentos só podem ser registrados com a triagem em andamento");
        if (justificativa == null || justificativa.isBlank())
            throw new IllegalArgumentException("Informe a justificativa clínica do encaminhamento");
        if (prioridade == null) throw new IllegalArgumentException("Informe a prioridade do encaminhamento");
        if (modalidade == Modalidade.IMEDIATO && !prioridade.grave())
            throw new IllegalArgumentException("Encaminhamento imediato exige prioridade EMERGENCIA ou MUITO_URGENTE");

        return new Encaminhamento(id, triagem.getId(), triagem.getPacienteId(), especialidade, profissionalDestinoId,
                modalidade, prioridade, justificativa.trim(), StatusEncaminhamento.PENDENTE, criadoPor, em, null, null,
                null, null, null);
    }

    public void liberarParaAgendamento() {
        if (modalidade == Modalidade.ELETIVO && status == StatusEncaminhamento.PENDENTE)
            status = StatusEncaminhamento.AGUARDANDO_AGENDAMENTO;
    }

    public void receberNoPlantao(UUID profissional, Instant em) {
        if (modalidade != Modalidade.IMEDIATO) throw new IllegalStateException("Somente encaminhamentos imediatos são recebidos pelo plantão");
        exigir(StatusEncaminhamento.PENDENTE, "receber");

        recebidoPor = profissional;
        recebidoEm = em;
        status = StatusEncaminhamento.EM_ATENDIMENTO;
    }

    public boolean permiteAgendamento() {
        return modalidade == Modalidade.ELETIVO && status == StatusEncaminhamento.AGUARDANDO_AGENDAMENTO;
    }

    public boolean permiteRemarcacao() {
        return modalidade == Modalidade.ELETIVO && status == StatusEncaminhamento.AGENDADO;
    }

    public void consultaAgendada() {
        if (!permiteAgendamento())
            throw new IllegalStateException("Este encaminhamento não está liberado para agendamento. Status atual: " + status);
        status = StatusEncaminhamento.AGENDADO;
    }

    public void consultaIniciada() {
        exigir(StatusEncaminhamento.AGENDADO, "iniciar o atendimento de");
        status = StatusEncaminhamento.EM_ATENDIMENTO;
    }

    public void consultaDesmarcada() {
        if (status == StatusEncaminhamento.AGENDADO) status = StatusEncaminhamento.AGUARDANDO_AGENDAMENTO;
    }

    public void consultaFinalizada(boolean retornoAutorizado) {
        exigir(StatusEncaminhamento.EM_ATENDIMENTO, "finalizar a consulta de");
        if (retornoAutorizado) status = StatusEncaminhamento.AGUARDANDO_AGENDAMENTO;
    }

    public void autorizarRetorno() {
        exigir(StatusEncaminhamento.EM_ATENDIMENTO, "autorizar retorno em");
        if (modalidade != Modalidade.ELETIVO) throw new IllegalStateException("Retornos são agendados apenas em encaminhamentos eletivos");
        status = StatusEncaminhamento.AGUARDANDO_AGENDAMENTO;
    }

    public void concluir(String resultado, Instant em) {
        if (status != StatusEncaminhamento.EM_ATENDIMENTO && status != StatusEncaminhamento.AGUARDANDO_AGENDAMENTO)
            throw new IllegalStateException("Não é possível concluir um encaminhamento com status " + status);
        if (resultado == null || resultado.isBlank()) throw new IllegalArgumentException("Informe o desfecho do acompanhamento");

        desfecho = resultado.trim();
        encerradoEm = em;
        status = StatusEncaminhamento.CONCLUIDO;
    }

    public void cancelar(String motivo, Instant em) {
        if (status != StatusEncaminhamento.PENDENTE && status != StatusEncaminhamento.AGUARDANDO_AGENDAMENTO)
            throw new IllegalStateException("Não é possível cancelar um encaminhamento com status " + status);
        if (motivo == null || motivo.isBlank()) throw new IllegalArgumentException("Informe o motivo do cancelamento");

        desfecho = "Cancelado: " + motivo.trim();
        encerradoEm = em;
        status = StatusEncaminhamento.CANCELADO;
    }

    public boolean destinadoA(UUID profissionalId) {
        return profissionalId != null && (profissionalId.equals(profissionalDestinoId) || profissionalId.equals(recebidoPor));
    }

    private void exigir(StatusEncaminhamento esperado, String acao) {
        if (status != esperado)
            throw new IllegalStateException("Não é possível " + acao + " um encaminhamento com status " + status);
    }

    public UUID getId() {
        return id;
    }

    public UUID getTriagemId() {
        return triagemId;
    }

    public UUID getPacienteId() {
        return pacienteId;
    }

    public String getEspecialidade() {
        return especialidade;
    }

    public UUID getProfissionalDestinoId() {
        return profissionalDestinoId;
    }

    public Modalidade getModalidade() {
        return modalidade;
    }

    public Prioridade getPrioridade() {
        return prioridade;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public StatusEncaminhamento getStatus() {
        return status;
    }

    public UUID getCriadoPor() {
        return criadoPor;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public UUID getRecebidoPor() {
        return recebidoPor;
    }

    public Instant getRecebidoEm() {
        return recebidoEm;
    }

    public Instant getEncerradoEm() {
        return encerradoEm;
    }

    public String getDesfecho() {
        return desfecho;
    }

    public Long getVersao() {
        return versao;
    }
}
