package sus.liva.application.port.in;

import sus.liva.domain.model.agendamento.StatusAgendamento;
import sus.liva.domain.model.fila.DecisaoSemResposta;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConsultaSemConfirmacao(
        UUID consultaId,
        UUID pacienteId,
        String paciente,
        String nomeEspecialidade,
        String profissional,
        String unidade,
        LocalDateTime dataHora,
        LocalDateTime confirmarAte,
        StatusAgendamento status,
        DecisaoSemResposta decisao,
        boolean pedidoEnviado,
        boolean pedidoEntregue
) {}
