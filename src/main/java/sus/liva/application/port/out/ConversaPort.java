package sus.liva.application.port.out;

import sus.liva.domain.model.Telefone;

import java.util.Optional;

public interface ConversaPort {
    Optional<Conversa> buscarConversa(Telefone telefone);
    void salvarConversa(Conversa conversa);
    void encerrarConversa(Telefone telefone);
}
