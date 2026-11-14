package sus.liva.infrastructure.adapter.in.web;

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
import sus.liva.application.exception.NaoEncontradoException;
import sus.liva.application.port.in.PedidoDePagina;
import sus.liva.application.port.in.AgendarAtendimentoUseCase;
import sus.liva.application.port.in.AtendimentoAgendado;
import sus.liva.application.port.in.ConsultarAgendamentoUseCase;
import sus.liva.application.port.in.ListarAgendamentosUseCase;
import sus.liva.domain.model.agendamento.StatusAgendamento;

import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/agendamentos")
@Tag(name = "Agendamentos", description = "Criação, consulta e listagem de agendamentos")
public class AgendamentoController {
    private final AgendarAtendimentoUseCase agendarAtendimento;
    private final ConsultarAgendamentoUseCase consultarAgendamento;
    private final ListarAgendamentosUseCase listarAgendamentos;
    private final Clock clock;

    public AgendamentoController(AgendarAtendimentoUseCase agendarAtendimento,
                                 ConsultarAgendamentoUseCase consultarAgendamento,
                                 ListarAgendamentosUseCase listarAgendamentos,
                                 Clock clock) {
        this.agendarAtendimento = agendarAtendimento;
        this.consultarAgendamento = consultarAgendamento;
        this.listarAgendamentos = listarAgendamentos;
        this.clock = clock;
    }

    @GetMapping
    @Operation(summary = "Lista os agendamentos para a equipe da unidade",
            description = "Sem parâmetros, lista os de hoje. Use inicio/fim (AAAA-MM-DD, no máximo 31 dias) e status "
                    + "(pode repetir, ex.: status=CANCELADO&status=REAGENDADO) para ver quem precisa de atenção. "
                    + "porStatus traz a contagem de cada situação no período. "
                    + "Envie pagina e tamanho (máximo 50) para receber a lista em páginas e busca para filtrar por nome ou telefone; "
                    + "nesse caso porStatus e total contam o período inteiro (com a busca) e encontrados conta o resultado do filtro de status.")
    public AgendamentosDaEquipeResponse listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) Set<StatusAgendamento> status,
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamanho) {
        LocalDate de = inicio != null ? inicio : LocalDate.now(clock);
        LocalDate ate = fim != null ? fim : de;
        if (pagina == null && tamanho == null && busca == null)
            return AgendamentosDaEquipeResponse.de(de, ate, listarAgendamentos.listar(de, ate, status));
        return AgendamentosDaEquipeResponse.de(de, ate, listarAgendamentos.listar(de, ate, status, busca, PedidoDePagina.de(pagina, tamanho)));
    }

    @PostMapping
    @Operation(summary = "Agenda um atendimento e avisa o paciente pelo WhatsApp",
            description = "Reaproveita o paciente pelo CPF ou, sem CPF, pelo telefone + nome. Envia a confirmação do agendamento "
                    + "e, se o atendimento já estiver dentro do prazo do pedido de confirmação, também o pedido (1, 2 ou 3). "
                    + "Falha no WhatsApp não desfaz o agendamento: veja os campos confirmacao e pedidoConfirmacao.")
    public ResponseEntity<AgendamentoCriadoResponse> agendar(@RequestBody AgendamentoRequest request) {
        AtendimentoAgendado agendado = agendarAtendimento.agendar(request.paraNovoAtendimento());
        return ResponseEntity.created(URI.create("/api/agendamentos/" + agendado.agendamento().getId()))
                .body(AgendamentoCriadoResponse.de(agendado));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta um agendamento e seu status atual", description = "Retorna 404 se o agendamento não existir.")
    public AgendamentoResponse consultar(@PathVariable UUID id) {
        return consultarAgendamento.consultar(id)
                .map(AgendamentoResponse::de)
                .orElseThrow(() -> new NaoEncontradoException("Agendamento não encontrado: " + id));
    }
}
