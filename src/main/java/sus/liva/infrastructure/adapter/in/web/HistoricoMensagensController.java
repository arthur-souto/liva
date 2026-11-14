package sus.liva.infrastructure.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import sus.liva.application.port.in.ConsultarHistoricoUseCase;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Agendamentos", description = "Criação, consulta e listagem de agendamentos")
public class HistoricoMensagensController {
    private final ConsultarHistoricoUseCase consultarHistorico;

    public HistoricoMensagensController(ConsultarHistoricoUseCase consultarHistorico) {
        this.consultarHistorico = consultarHistorico;
    }

    @GetMapping("/api/agendamentos/{id}/mensagens")
    @Operation(summary = "Histórico de mensagens do agendamento",
            description = "Mensagens enviadas pela Liva e respostas do paciente, em ordem cronológica. 404 se o agendamento não existir.")
    public List<MensagemDoHistoricoResponse> historico(@PathVariable UUID id) {
        return consultarHistorico.historico(id).stream().map(MensagemDoHistoricoResponse::de).toList();
    }
}
