package sus.liva.infrastructure.adapter.in.web.assistencial;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sus.liva.application.port.in.AgendaDeConsultasUseCase;
import sus.liva.application.port.in.Ator;
import sus.liva.application.port.in.EncaminhamentoUseCase;
import sus.liva.application.port.in.HorarioDisponivel;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/encaminhamentos")
@Tag(name = "Encaminhamentos", description = "Recebimento pelo especialista e agendamento das consultas")
public class EncaminhamentoController {
    private final EncaminhamentoUseCase encaminhamentos;
    private final AgendaDeConsultasUseCase agenda;

    public EncaminhamentoController(EncaminhamentoUseCase encaminhamentos, AgendaDeConsultasUseCase agenda) {
        this.encaminhamentos = encaminhamentos;
        this.agenda = agenda;
    }

    @GetMapping("/recebidos")
    @Operation(summary = "Encaminhamentos da especialidade do especialista logado")
    public List<EncaminhamentoRecebidoResponse> recebidos(Ator ator) {
        return encaminhamentos.recebidos(ator).stream().map(EncaminhamentoRecebidoResponse::de).toList();
    }

    @PostMapping("/{id}/receber")
    @Operation(summary = "O plantão assume um encaminhamento imediato")
    public EncaminhamentoResponse receber(Ator ator, @PathVariable UUID id) {
        return EncaminhamentoResponse.de(encaminhamentos.receberNoPlantao(ator, id));
    }

    @PostMapping("/{id}/retorno")
    public EncaminhamentoResponse autorizarRetorno(Ator ator, @PathVariable UUID id) {
        return EncaminhamentoResponse.de(encaminhamentos.autorizarRetorno(ator, id));
    }

    @PostMapping("/{id}/concluir")
    public EncaminhamentoResponse concluir(Ator ator, @PathVariable UUID id, @RequestBody Requisicoes.Desfecho request) {
        return EncaminhamentoResponse.de(encaminhamentos.concluir(ator, id, request.desfecho()));
    }

    @PostMapping("/{id}/cancelar")
    public EncaminhamentoResponse cancelar(Ator ator, @PathVariable UUID id, @RequestBody Requisicoes.Motivo request) {
        return EncaminhamentoResponse.de(encaminhamentos.cancelar(ator, id, request.motivo()));
    }

    @GetMapping("/{id}/horarios")
    @Operation(summary = "Horários livres dos especialistas para este encaminhamento (até 31 dias)")
    public List<HorarioDisponivel> horarios(Ator ator, @PathVariable UUID id,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return agenda.horariosDisponiveis(ator, id, inicio, fim);
    }

    public record AgendaResponse(boolean possuiAgenda) {}

    @GetMapping("/{id}/agenda")
    @Operation(summary = "Indica se algum especialista atende este encaminhamento com agenda aberta")
    public AgendaResponse agenda(Ator ator, @PathVariable UUID id) {
        return new AgendaResponse(agenda.possuiAgenda(ator, id));
    }

    @PostMapping("/{id}/consultas")
    @Operation(summary = "Agenda a consulta inicial ou de retorno",
            description = "Envie o header Idempotency-Key para repetir a requisição com segurança. "
                    + "Se o horário acabou de ser reservado por outra pessoa, responde 409.")
    public ResponseEntity<ConsultaResponse> agendar(Ator ator, @PathVariable UUID id,
                                                    @RequestHeader(name = "Idempotency-Key", required = false) String chave,
                                                    @RequestBody Requisicoes.NovaConsulta request) {
        ConsultaResponse consulta = ConsultaResponse.de(agenda.agendar(ator, id, request.profissionalId(), request.inicio(), chave));
        return ResponseEntity.created(URI.create("/api/consultas/" + consulta.id())).body(consulta);
    }
}
