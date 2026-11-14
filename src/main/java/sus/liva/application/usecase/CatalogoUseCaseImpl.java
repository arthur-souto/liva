package sus.liva.application.usecase;

import sus.liva.application.port.in.Ator;
import sus.liva.application.port.in.CatalogoUseCase;
import sus.liva.application.port.in.Papel;
import sus.liva.application.port.out.ProfissionalRepositoryPort;
import sus.liva.domain.model.profissional.Especialidade;
import sus.liva.domain.model.profissional.Profissional;

import java.util.List;

public class CatalogoUseCaseImpl implements CatalogoUseCase {
    private final ProfissionalRepositoryPort profissionais;

    public CatalogoUseCaseImpl(ProfissionalRepositoryPort profissionais) {
        this.profissionais = profissionais;
    }

    @Override
    public List<Especialidade> especialidades(Ator ator) {
        Permissoes.exigir(ator, Papel.ADMINISTRADOR, Papel.RECEPCAO, Papel.CLINICO_GERAL, Papel.ESPECIALISTA, Papel.PACIENTE, Papel.CUIDADOR);
        return profissionais.especialidades();
    }

    @Override
    public List<Profissional> especialistas(Ator ator, String especialidade) {
        Permissoes.exigir(ator, Papel.ADMINISTRADOR, Papel.RECEPCAO, Papel.CLINICO_GERAL, Papel.ESPECIALISTA, Papel.PACIENTE, Papel.CUIDADOR);
        if (ValidacaoCadastral.vazio(especialidade)) throw new IllegalArgumentException("Informe a especialidade");
        if (profissionais.buscarEspecialidade(especialidade).isEmpty())
            throw new IllegalArgumentException("Especialidade desconhecida: " + especialidade);
        return profissionais.especialistasDe(especialidade);
    }
}
