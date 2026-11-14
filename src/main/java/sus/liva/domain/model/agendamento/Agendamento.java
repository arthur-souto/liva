package sus.liva.domain.model.agendamento;

import sus.liva.domain.model.encaminhamento.Encaminhamento;
import sus.liva.domain.model.profissional.Profissional;

import java.time.LocalDateTime;
import java.util.UUID;

public class Agendamento {
    public static final int DURACAO_PADRAO = 30;

    private final UUID id;
    private final UUID pacienteId;
    private final LocalDateTime dataHora;
    private final TipoAtendimento tipo;
    private final String unidade;
    private final String endereco;
    private final String preparo;
    private final UUID triagemId;
    private final UUID encaminhamentoId;
    private final UUID profissionalId;
    private final int duracaoMinutos;
    private final NaturezaConsulta natureza;
    private final String chaveIdempotencia;
    private final Long versao;
    private StatusAgendamento status;

    public Agendamento(StatusAgendamento status, String preparo, String endereco, String unidade, TipoAtendimento tipo, LocalDateTime dataHora, UUID pacienteId, UUID id) {
        this(id, pacienteId, dataHora, tipo, unidade, endereco, preparo, status, null, null, null, DURACAO_PADRAO,
                NaturezaConsulta.AVULSA, null, null);
    }

    public Agendamento(UUID id, UUID pacienteId, LocalDateTime dataHora, TipoAtendimento tipo, String unidade,
                       String endereco, String preparo, StatusAgendamento status, UUID triagemId, UUID encaminhamentoId,
                       UUID profissionalId, int duracaoMinutos, NaturezaConsulta natureza, String chaveIdempotencia,
                       Long versao) {
        this.id = id;
        this.pacienteId = pacienteId;
        this.dataHora = dataHora;
        this.tipo = tipo;
        this.unidade = unidade;
        this.endereco = endereco;
        this.preparo = preparo;
        this.status = status;
        this.triagemId = triagemId;
        this.encaminhamentoId = encaminhamentoId;
        this.profissionalId = profissionalId;
        this.duracaoMinutos = duracaoMinutos;
        this.natureza = natureza;
        this.chaveIdempotencia = chaveIdempotencia;
        this.versao = versao;
    }

    public static Agendamento consulta(UUID id, Encaminhamento encaminhamento, Profissional profissional,
                                       LocalDateTime inicio, int duracaoMinutos, NaturezaConsulta natureza,
                                       String chaveIdempotencia) {
        if (natureza == NaturezaConsulta.AVULSA) throw new IllegalArgumentException("Consulta de encaminhamento não pode ser avulsa");
        if (!profissional.atende(encaminhamento.getEspecialidade()))
            throw new IllegalArgumentException("O profissional escolhido não atende a especialidade do encaminhamento");
        if (encaminhamento.getProfissionalDestinoId() != null && !encaminhamento.getProfissionalDestinoId().equals(profissional.id()))
            throw new IllegalArgumentException("Este encaminhamento é destinado a outro profissional");
        return new Agendamento(id, encaminhamento.getPacienteId(), inicio, TipoAtendimento.CONSULTA, profissional.unidade(),
                profissional.endereco(), null, StatusAgendamento.AGENDADO, encaminhamento.getTriagemId(),
                encaminhamento.getId(), profissional.id(), duracaoMinutos, natureza, chaveIdempotencia, null);
    }

    public void iniciarAtendimento() {
        if (!status.ocupaAgenda() || status == StatusAgendamento.EM_ANDAMENTO)
            throw new IllegalStateException("Não é possível iniciar uma consulta com status " + status);
        status = StatusAgendamento.EM_ANDAMENTO;
    }

    public void finalizarAtendimento() {
        if (status != StatusAgendamento.EM_ANDAMENTO)
            throw new IllegalStateException("Somente consultas em andamento podem ser finalizadas. Status atual: " + status);
        status = StatusAgendamento.COMPARECEU;
    }

    public void registrarFalta() {
        if (!status.ocupaAgenda() || status == StatusAgendamento.EM_ANDAMENTO)
            throw new IllegalStateException("Não é possível registrar falta em uma consulta com status " + status);
        status = StatusAgendamento.FALTOU;
    }

    public void cancelar() {
        if (!status.ocupaAgenda() || status == StatusAgendamento.EM_ANDAMENTO)
            throw new IllegalStateException("Não é possível cancelar uma consulta com status " + status);
        status = StatusAgendamento.CANCELADO;
    }

    public boolean doEncaminhamento() {
        return encaminhamentoId != null;
    }

    public boolean doProfissional(UUID profissional) {
        return profissional != null && profissional.equals(profissionalId);
    }

    public LocalDateTime getFim() {
        return dataHora.plusMinutes(duracaoMinutos);
    }

    public boolean podeReceber(TipoLembrete lembrete) {
        return switch (status) {
            case AGENDADO, AGUARDANDO_RESPOSTA -> true;
            case CONFIRMADO -> lembrete == TipoLembrete.LEMBRETE_VESPERA; // D-1 leva o preparo
            default -> false;
        };
    }

    public void aguardarResposta() {
        if (status == StatusAgendamento.AGENDADO) status = StatusAgendamento.AGUARDANDO_RESPOSTA;
    }

    public boolean aguardandoResposta() {
        return status == StatusAgendamento.AGUARDANDO_RESPOSTA;
    }

    public void responder(RespostaPaciente resposta) {
        if (!aguardandoResposta())
            throw new IllegalStateException("Agendamento não está aguardando resposta: " + status);
        status = resposta.getStatusResultante();
    }

    public UUID getId() {
        return id;
    }

    public UUID getPacienteId() {
        return pacienteId;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public TipoAtendimento getTipo() {
        return tipo;
    }

    public String getUnidade() {
        return unidade;
    }

    public String getEndereco() {
        return endereco;
    }

    public String getPreparo() {
        return preparo;
    }

    public StatusAgendamento getStatus() {
        return status;
    }

    public UUID getTriagemId() {
        return triagemId;
    }

    public UUID getEncaminhamentoId() {
        return encaminhamentoId;
    }

    public UUID getProfissionalId() {
        return profissionalId;
    }

    public int getDuracaoMinutos() {
        return duracaoMinutos;
    }

    public NaturezaConsulta getNatureza() {
        return natureza;
    }

    public String getChaveIdempotencia() {
        return chaveIdempotencia;
    }

    public Long getVersao() {
        return versao;
    }

    public void setStatus(StatusAgendamento status) {
        this.status = status;
    }
}