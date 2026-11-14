package sus.liva.application.port.out;

import sus.liva.domain.model.paciente.Paciente;

import java.util.Optional;
import java.util.UUID;

public interface PacienteRepositoryPort {
    Optional<Paciente> buscarPaciente(UUID id);
    void salvarPaciente(Paciente paciente);
}
