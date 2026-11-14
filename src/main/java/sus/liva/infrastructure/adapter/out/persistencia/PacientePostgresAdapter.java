package sus.liva.infrastructure.adapter.out.persistencia;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sus.liva.application.port.out.PacienteRepositoryPort;
import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.paciente.Paciente;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PacientePostgresAdapter implements PacienteRepositoryPort {
    private final PacienteJpaRepository repository;

    PacientePostgresAdapter(PacienteJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Paciente> buscarPaciente(UUID id) {
        return repository.findById(id).map(PacienteEntity::paraDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Paciente> buscarPorCpf(String cpf) {
        return repository.findByCpf(cpf).map(PacienteEntity::paraDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Paciente> buscarPorTelefone(Telefone telefone) {
        return repository.findByTelefone(telefone.e164()).stream().map(PacienteEntity::paraDominio).toList();
    }

    @Override
    @Transactional
    public void salvarPaciente(Paciente paciente) {
        repository.save(PacienteEntity.de(paciente));
    }
}
