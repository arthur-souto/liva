package sus.liva.application.port.out;

import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.agendamento.TipoLembrete;

import java.util.UUID;

public record MensagemSaida(UUID agendamentoId, Telefone destino, TipoLembrete tipo, String texto) {}
