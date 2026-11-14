package sus.liva.application.port.out;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ContaDeAcessoPort {
    Optional<ContaDeAcesso> buscarPorLogin(String login);
    Optional<ContaDeAcesso> buscarPorPaciente(UUID pacienteId);
    void criarConta(ContaDeAcesso conta, Instant em);
    void redefinirSenha(UUID contaId, String senhaHash);
    void registrarFalha(UUID contaId, int limite, Instant bloquearAte);
    void registrarAcesso(UUID contaId);
}
