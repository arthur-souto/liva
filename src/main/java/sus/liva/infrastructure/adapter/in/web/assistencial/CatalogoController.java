package sus.liva.infrastructure.adapter.in.web.assistencial;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sus.liva.application.port.in.Ator;
import sus.liva.application.port.in.CatalogoUseCase;
import sus.liva.domain.model.profissional.Especialidade;
import sus.liva.domain.model.profissional.Profissional;

import java.util.List;

@RestController
@RequestMapping("/api/catalogo")
@Tag(name = "Catálogo", description = "Especialidades e especialistas para os encaminhamentos")
public class CatalogoController {
    private final CatalogoUseCase catalogo;

    public CatalogoController(CatalogoUseCase catalogo) {
        this.catalogo = catalogo;
    }

    @GetMapping("/especialidades")
    public List<Especialidade> especialidades(Ator ator) {
        return catalogo.especialidades(ator);
    }

    @GetMapping("/especialistas")
    public List<Profissional> especialistas(Ator ator, @RequestParam String especialidade) {
        return catalogo.especialistas(ator, especialidade);
    }
}
