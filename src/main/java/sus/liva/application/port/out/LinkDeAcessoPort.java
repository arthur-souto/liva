package sus.liva.application.port.out;

import java.time.Instant;
import java.util.Optional;

public interface LinkDeAcessoPort {
    void salvarLink(LinkDeAcesso link);
    Optional<LinkDeAcesso> buscarParaUsar(String tokenHash);
    void marcarUsado(String tokenHash, Instant em);
}
