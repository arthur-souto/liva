package sus.liva.infrastructure.adapter.in.web.teste;

import sus.liva.application.port.in.ResultadoDisparo;
import sus.liva.infrastructure.adapter.in.web.AgendamentoResponse;

import java.util.UUID;

public record AtendimentoTesteResponse(UUID pacienteId, AgendamentoResponse agendamento,
                                       ResultadoDisparo disparo, String proximoPasso) {}
