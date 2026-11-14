package sus.liva.application.port.out;

import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.paciente.Paciente;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PacienteRepositoryPort {
    Optional<Paciente> buscarPaciente(UUID id);
    Optional<Paciente> buscarPorCpf(String cpf);
    List<Paciente> buscarPorTelefone(Telefone telefone);
    void salvarPaciente(Paciente paciente);
}
