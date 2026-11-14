package sus.liva.application.port.in;

import java.time.LocalDate;

public interface AutenticacaoUseCase {
    Sessao entrar(String login, String senha);
    void solicitarCodigoDoPortal(String documento, LocalDate dataNascimento);
    Sessao ativarPortal(String documento, LocalDate dataNascimento, String codigo, String senha);
}
