package sus.liva.infrastructure.adapter.in.web;

import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.agendamento.StatusAgendamento;
import sus.liva.domain.model.agendamento.TipoAtendimento;

import java.time.LocalDateTime;
import java.util.UUID;

public record AgendamentoResponse(
        UUID id,
        UUID pacienteId,
        LocalDateTime dataHora,
        TipoAtendimento tipo,
        String unidade,
        String endereco,
        String preparo,
        StatusAgendamento status
) {
    public static AgendamentoResponse de(Agendamento a) {
        return new AgendamentoResponse(a.getId(), a.getPacienteId(), a.getDataHora(), a.getTipo(),
                a.getUnidade(), a.getEndereco(), a.getPreparo(), a.getStatus());
    }
}
