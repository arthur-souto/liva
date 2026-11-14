package sus.liva.application.port.out;

import sus.liva.domain.model.profissional.Disponibilidade;
import sus.liva.domain.model.profissional.Especialidade;
import sus.liva.domain.model.profissional.Profissional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfissionalRepositoryPort {
    Optional<Profissional> buscarProfissional(UUID id);
    List<Profissional> especialistasDe(String especialidade);
    List<Disponibilidade> disponibilidadesDe(UUID profissionalId);
    Optional<Especialidade> buscarEspecialidade(String codigo);
    List<Especialidade> especialidades();
    void salvarProfissional(Profissional profissional);
    void salvarDisponibilidade(Disponibilidade disponibilidade);
}
