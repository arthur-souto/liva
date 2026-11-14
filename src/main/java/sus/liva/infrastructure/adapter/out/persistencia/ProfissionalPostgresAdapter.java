package sus.liva.infrastructure.adapter.out.persistencia;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sus.liva.application.port.out.ProfissionalRepositoryPort;
import sus.liva.domain.model.profissional.Disponibilidade;
import sus.liva.domain.model.profissional.Especialidade;
import sus.liva.domain.model.profissional.Profissional;
import sus.liva.domain.model.profissional.TipoProfissional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ProfissionalPostgresAdapter implements ProfissionalRepositoryPort {
    private final ProfissionalJpaRepository profissionais;
    private final DisponibilidadeJpaRepository disponibilidades;
    private final EspecialidadeJpaRepository especialidades;

    ProfissionalPostgresAdapter(ProfissionalJpaRepository profissionais, DisponibilidadeJpaRepository disponibilidades,
                                EspecialidadeJpaRepository especialidades) {
        this.profissionais = profissionais;
        this.disponibilidades = disponibilidades;
        this.especialidades = especialidades;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Profissional> buscarProfissional(UUID id) {
        return profissionais.findById(id).map(ProfissionalEntity::paraDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Profissional> especialistasDe(String especialidade) {
        return profissionais.findByTipoAndEspecialidadeAndAtivoTrueOrderByNome(TipoProfissional.ESPECIALISTA, especialidade)
                .stream().map(ProfissionalEntity::paraDominio).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Disponibilidade> disponibilidadesDe(UUID profissionalId) {
        return disponibilidades.findByProfissionalIdOrderByDiaSemanaAscHoraInicioAsc(profissionalId).stream()
                .map(DisponibilidadeEntity::paraDominio).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Especialidade> buscarEspecialidade(String codigo) {
        return especialidades.findById(codigo).map(EspecialidadeEntity::paraDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Especialidade> especialidades() {
        return especialidades.findAll(Sort.by("nome")).stream().map(EspecialidadeEntity::paraDominio).toList();
    }

    @Override
    @Transactional
    public void salvarProfissional(Profissional profissional) {
        Restricoes.traduzir(() -> profissionais.saveAndFlush(ProfissionalEntity.de(profissional)));
    }

    @Override
    @Transactional
    public void salvarDisponibilidade(Disponibilidade disponibilidade) {
        disponibilidades.saveAndFlush(DisponibilidadeEntity.de(disponibilidade));
    }
}
