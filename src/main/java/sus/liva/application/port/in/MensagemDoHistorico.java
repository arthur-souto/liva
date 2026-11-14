package sus.liva.application.port.in;

import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.agendamento.RespostaPaciente;
import sus.liva.domain.model.agendamento.TipoLembrete;
import sus.liva.domain.model.mensagem.MensagemRecebida;
import sus.liva.domain.model.mensagem.RegistroMensagem;

import java.time.Instant;

public record MensagemDoHistorico(Direcao direcao, Telefone telefone, String texto, TipoLembrete tipo,
                                  RespostaPaciente resposta, boolean sucesso, String erro, Instant em) {
    public enum Direcao { ENVIADA, RECEBIDA }

    public static MensagemDoHistorico de(RegistroMensagem r) {
        return new MensagemDoHistorico(Direcao.ENVIADA, r.destino(), r.texto(), r.tipo(), null, r.sucesso(), r.erro(), r.enviadoEm());
    }

    public static MensagemDoHistorico de(MensagemRecebida m) {
        return new MensagemDoHistorico(Direcao.RECEBIDA, m.remetente(), m.texto(), null, m.resposta(), true, null, m.recebidaEm());
    }
}
