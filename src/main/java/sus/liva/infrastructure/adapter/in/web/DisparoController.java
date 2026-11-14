package sus.liva.infrastructure.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sus.liva.application.port.in.DispararLembreteUseCase;
import sus.liva.application.port.in.DispararLembretesDoDiaUseCase;
import sus.liva.application.port.in.ResultadoDisparo;
import sus.liva.domain.model.agendamento.TipoLembrete;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "Lembretes", description = "Disparo de lembretes de agendamento pelo WhatsApp")
public class DisparoController {
    private final DispararLembreteUseCase disparar;
    private final DispararLembretesDoDiaUseCase lembretesDoDia;

    public DisparoController(DispararLembreteUseCase disparar, DispararLembretesDoDiaUseCase lembretesDoDia) {
        this.disparar = disparar;
        this.lembretesDoDia = lembretesDoDia;
    }

    @PostMapping("/agendamentos/{id}/disparos")
    @Operation(summary = "Dispara um lembrete para o paciente (e cuidador) de um agendamento",
            description = "forcar=true reenvia mesmo que o lembrete já tenha sido enviado. PEDIDO_CONFIRMACAO deixa o agendamento AGUARDANDO_RESPOSTA.")
    public ResultadoDisparo disparar(@PathVariable UUID id, @RequestParam TipoLembrete tipo,
                                     @RequestParam(defaultValue = "true") boolean forcar) {
        return disparar.disparar(id, tipo, forcar);
    }

    @PostMapping("/disparos/executar")
    @Operation(summary = "Dispara os lembretes pendentes do dia",
            description = "PEDIDO_CONFIRMACAO para atendimentos em data+2 e LEMBRETE_VESPERA para data+1. Sem data, usa hoje.")
    public Map<String, Integer> executar(@RequestParam(required = false)
                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        LocalDate dia = data != null ? data : LocalDate.now();
        return Map.of("mensagensEnviadas", lembretesDoDia.dispararPendentes(dia));
    }
}
