package sus.liva.infrastructure.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sus.liva.application.port.in.ConsultarAgendamentoUseCase;

import java.util.UUID;

@RestController
@RequestMapping("/api/agendamentos")
@Tag(name = "Agendamentos", description = "Consulta de agendamentos")
public class AgendamentoController {
    private final ConsultarAgendamentoUseCase consultarAgendamento;

    public AgendamentoController(ConsultarAgendamentoUseCase consultarAgendamento) {
        this.consultarAgendamento = consultarAgendamento;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta um agendamento e seu status atual", description = "Retorna 404 se o agendamento não existir.")
    public ResponseEntity<AgendamentoResponse> consultar(@PathVariable UUID id) {
        return consultarAgendamento.consultar(id)
                .map(AgendamentoResponse::de)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
