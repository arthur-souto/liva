package sus.liva.application.port.in;

import java.util.UUID;

public interface HistoricoDoPacienteUseCase {
    HistoricoDoPaciente historico(Ator ator, UUID pacienteId);
    boolean podeAcessarPortal(UUID pacienteId);
}
