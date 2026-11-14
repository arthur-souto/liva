package sus.liva.application.port.out;

import sus.liva.domain.model.agendamento.StatusAgendamento;
import sus.liva.domain.model.ausencia.LigacaoRegistrada;
import sus.liva.domain.model.ausencia.SinaisDeRisco;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConsultaComSinais(
        UUID agendamentoId,
        UUID pacienteId,
        String nomePaciente,
        String telefone,
        LocalDateTime dataHora,
        String especialidade,
        String profissional,
        String unidade,
        StatusAgendamento status,
        SinaisDeRisco sinais,
        LigacaoRegistrada ultimaLigacao
) {}
