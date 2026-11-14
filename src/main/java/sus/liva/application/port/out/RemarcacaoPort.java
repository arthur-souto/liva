package sus.liva.application.port.out;

import java.time.Instant;
import java.util.UUID;

public interface RemarcacaoPort {
    int contarDesde(UUID pacienteId, Instant desde);
    void registrarRemarcacao(UUID pacienteId, UUID consultaAnterior, UUID consultaNova, String canal, Instant em);
}
