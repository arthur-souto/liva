package sus.liva.application.port.out;

import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.agendamento.TipoLembrete;
import sus.liva.domain.model.mensagem.RegistroMensagem;

import java.util.Optional;
import java.util.UUID;

public interface RegistroMensagemPort {
    void salvarRegistro(RegistroMensagem registro);
    boolean jaEnviado(UUID agendamentoId, TipoLembrete tipo);
    Optional<RegistroMensagem> ultimoEnvio(Telefone destino, TipoLembrete tipo);
}
