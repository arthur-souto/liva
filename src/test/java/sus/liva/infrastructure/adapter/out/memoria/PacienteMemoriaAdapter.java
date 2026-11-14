package sus.liva.infrastructure.adapter.out.memoria;

import sus.liva.application.port.out.PacienteRepositoryPort;
import sus.liva.domain.model.paciente.Paciente;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PacienteMemoriaAdapter implements PacienteRepositoryPort {
    private final Map<UUID, Paciente> dados = new ConcurrentHashMap<>();

    @Override
    public Optional<Paciente> buscarPaciente(UUID id) {
        return Optional.ofNullable(dados.get(id));
    }

    @Override
    public void salvarPaciente(Paciente paciente) {
        dados.put(paciente.id(), paciente);
    }
}
