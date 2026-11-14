package sus.liva.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface CodigoDoPortalPort {
    Optional<CodigoDoPortal> buscarParaAtualizar(UUID pacienteId);
    void salvarCodigo(CodigoDoPortal codigo);
    void removerCodigo(UUID pacienteId);
}
