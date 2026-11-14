package sus.liva.domain.model.mensagem;

import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.agendamento.TipoLembrete;

import java.time.Instant;
import java.util.UUID;

public record RegistroMensagem(
        UUID agendamentoId,
        Telefone destino,
        TipoLembrete tipo,
        String texto,
        boolean sucesso,
        String idExterno,
        String erro,
        Instant enviadoEm
) {}
