package sus.liva.application.port.in;

import sus.liva.domain.model.profissional.Especialidade;
import sus.liva.domain.model.profissional.Profissional;

import java.util.List;

public interface CatalogoUseCase {
    List<Especialidade> especialidades(Ator ator);
    List<Profissional> especialistas(Ator ator, String especialidade);
}
