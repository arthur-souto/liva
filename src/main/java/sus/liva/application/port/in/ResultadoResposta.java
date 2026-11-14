package sus.liva.application.port.in;

import sus.liva.domain.model.agendamento.StatusAgendamento;

import java.util.UUID;

public record ResultadoResposta(boolean aplicada, UUID agendamentoId, StatusAgendamento status,
                                boolean retornoEnviado, String motivo) {
    public static ResultadoResposta aplicada(UUID agendamentoId, StatusAgendamento status, boolean retornoEnviado) {
        return new ResultadoResposta(true, agendamentoId, status, retornoEnviado, null);
    }
    public static ResultadoResposta naoReconhecida(UUID agendamentoId, StatusAgendamento status, boolean orientacaoEnviada) {
        return new ResultadoResposta(false, agendamentoId, status, orientacaoEnviada,
                orientacaoEnviada ? "Resposta não reconhecida; orientação enviada ao paciente"
                        : "Resposta não reconhecida; falha ao enviar a orientação");
    }
    public static ResultadoResposta autoatendimento(String etapa, UUID agendamentoId, boolean enviada) {
        return new ResultadoResposta(false, agendamentoId, null, enviada, "Autoatendimento: " + etapa);
    }
    public static ResultadoResposta ignorada(String motivo) {
        return new ResultadoResposta(false, null, null, false, motivo);
    }
}
