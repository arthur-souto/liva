package sus.liva.domain.model.mensagem;

import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.agendamento.RespostaPaciente;

import java.time.Instant;
import java.util.UUID;

public record MensagemRecebida(
        UUID agendamentoId,
        Telefone remetente,
        String texto,
        String idExterno,
        RespostaPaciente resposta,
        Instant recebidaEm
) {}
