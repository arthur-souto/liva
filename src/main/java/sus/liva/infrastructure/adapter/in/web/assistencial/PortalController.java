package sus.liva.infrastructure.adapter.in.web.assistencial;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sus.liva.application.exception.AcessoNegadoException;
import sus.liva.application.port.in.Ator;
import sus.liva.application.port.in.HistoricoDoPacienteUseCase;
import sus.liva.application.port.in.Papel;

@RestController
@RequestMapping("/api/portal")
@Tag(name = "Portal do paciente", description = "O paciente acompanha as próprias triagens, encaminhamentos e consultas")
public class PortalController {
    private final HistoricoDoPacienteUseCase historico;

    public PortalController(HistoricoDoPacienteUseCase historico) {
        this.historico = historico;
    }

    @GetMapping("/historico")
    @Operation(summary = "Histórico do paciente logado")
    public HistoricoResponse historico(Ator ator) {
        if (!ator.eh(Papel.PACIENTE)) throw new AcessoNegadoException("O portal é exclusivo do paciente");
        return HistoricoResponse.de(historico.historico(ator, ator.pacienteId()));
    }
}
