package sus.liva.infrastructure.adapter.in.web.assistencial;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sus.liva.application.port.in.AgendaDeConsultasUseCase;
import sus.liva.application.port.in.AtendimentoEspecializadoUseCase;
import sus.liva.application.port.in.Ator;
import sus.liva.domain.model.agendamento.EvolucaoClinica;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/consultas")
@Tag(name = "Consultas", description = "Agenda do especialista, atendimento, remarcação e cancelamento")
public class ConsultaController {
    private final AgendaDeConsultasUseCase agenda;
    private final AtendimentoEspecializadoUseCase atendimento;

    public ConsultaController(AgendaDeConsultasUseCase agenda, AtendimentoEspecializadoUseCase atendimento) {
        this.agenda = agenda;
        this.atendimento = atendimento;
    }

    @GetMapping("/agenda")
    @Operation(summary = "Agenda do especialista logado no período")
    public List<ConsultaDaAgendaResponse> agenda(Ator ator,
                                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return atendimento.agenda(ator, inicio, fim).stream().map(ConsultaDaAgendaResponse::de).toList();
    }

    @PostMapping("/{id}/remarcar")
    public ConsultaResponse remarcar(Ator ator, @PathVariable UUID id, @RequestBody Requisicoes.Remarcacao request) {
        return ConsultaResponse.de(agenda.remarcar(ator, id, request.profissionalId(), request.inicio(), Versao.exigir(request.versao())));
    }

    @PostMapping("/{id}/cancelar")
    public ConsultaResponse cancelar(Ator ator, @PathVariable UUID id, @RequestBody Requisicoes.ComVersao request) {
        return ConsultaResponse.de(agenda.cancelar(ator, id, Versao.exigir(request.versao())));
    }

    @PostMapping("/{id}/iniciar")
    public ConsultaResponse iniciar(Ator ator, @PathVariable UUID id, @RequestBody Requisicoes.ComVersao request) {
        return ConsultaResponse.de(atendimento.iniciar(ator, id, Versao.exigir(request.versao())));
    }

    @PostMapping("/{id}/finalizar")
    @Operation(summary = "Finaliza o atendimento com a evolução clínica", description = "autorizarRetorno libera um novo agendamento.")
    public ConsultaResponse finalizar(Ator ator, @PathVariable UUID id, @RequestBody Requisicoes.Finalizacao request) {
        return ConsultaResponse.de(atendimento.finalizar(ator, id, request.evolucao(), request.autorizarRetorno(),
                Versao.exigir(request.versao())));
    }

    @PostMapping("/{id}/evolucoes")
    public ResponseEntity<EvolucaoClinica> registrarEvolucao(Ator ator, @PathVariable UUID id,
                                                             @RequestBody Requisicoes.NovaEvolucao request) {
        return ResponseEntity.status(201).body(atendimento.registrarEvolucao(ator, id, request.texto()));
    }

    @PostMapping("/{id}/falta")
    public ConsultaResponse registrarFalta(Ator ator, @PathVariable UUID id, @RequestBody Requisicoes.ComVersao request) {
        return ConsultaResponse.de(atendimento.registrarFalta(ator, id, Versao.exigir(request.versao())));
    }
}
