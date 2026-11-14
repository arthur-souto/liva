package sus.liva.application.port.in;

import sus.liva.domain.model.agendamento.StatusAgendamento;
import sus.liva.domain.model.ausencia.LigacaoRegistrada;
import sus.liva.domain.model.ausencia.RiscoDeFalta;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConsultaEmRisco(
        UUID agendamentoId,
        UUID pacienteId,
        String nomePaciente,
        String telefone,
        LocalDateTime dataHora,
        String especialidade,
        String profissional,
        String unidade,
        StatusAgendamento status,
        RiscoDeFalta risco,
        LigacaoRegistrada ultimaLigacao
) {}
