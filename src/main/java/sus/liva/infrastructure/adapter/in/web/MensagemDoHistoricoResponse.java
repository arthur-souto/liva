package sus.liva.infrastructure.adapter.in.web;

import sus.liva.application.port.in.MensagemDoHistorico;
import sus.liva.domain.model.agendamento.RespostaPaciente;
import sus.liva.domain.model.agendamento.TipoLembrete;

import java.time.Instant;

public record MensagemDoHistoricoResponse(MensagemDoHistorico.Direcao direcao, String telefone, String texto,
                                          TipoLembrete tipo, RespostaPaciente resposta, boolean sucesso,
                                          String erro, Instant em) {
    static MensagemDoHistoricoResponse de(MensagemDoHistorico m) {
        return new MensagemDoHistoricoResponse(m.direcao(), m.telefone().apenasDigitos(), m.texto(), m.tipo(),
                m.resposta(), m.sucesso(), m.erro(), m.em());
    }
}
