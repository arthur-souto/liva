package sus.liva.infrastructure.adapter.out.memoria;

import sus.liva.application.port.out.PacienteRepositoryPort;
import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.paciente.Paciente;

import java.util.List;
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
    public Optional<Paciente> buscarPorCpf(String cpf) {
        return dados.values().stream().filter(p -> cpf.equals(p.cpf())).findFirst();
    }

    @Override
    public List<Paciente> buscarPorTelefone(Telefone telefone) {
        return dados.values().stream().filter(p -> telefone.equals(p.telefone())).toList();
    }

    @Override
    public void salvarPaciente(Paciente paciente) {
        dados.put(paciente.id(), paciente);
    }

    public int total() {
        return dados.size();
    }
}
