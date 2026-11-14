package sus.liva.infrastructure.adapter.in.web.teste;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sus.liva.application.port.in.AgendarAtendimentoUseCase;
import sus.liva.application.port.in.AtendimentoAgendado;
import sus.liva.application.port.in.ConsultarAgendamentoUseCase;
import sus.liva.application.port.in.DispararLembreteUseCase;
import sus.liva.application.port.in.NovoAtendimento;
import sus.liva.application.port.in.ProcessarRespostaUseCase;
import sus.liva.application.port.in.ResultadoDisparo;
import sus.liva.application.port.in.ResultadoResposta;
import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.agendamento.TipoAtendimento;
import sus.liva.infrastructure.adapter.in.web.AgendamentoResponse;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/teste/atendimentos")
@ConditionalOnProperty(name = "confirmasus.atendimento-teste.habilitado", havingValue = "true", matchIfMissing = true)
@Tag(name = "Teste de atendimento", description = "Cria atendimentos para qualquer número e simula respostas, para testar o fluxo de ponta a ponta")
public class AtendimentoTesteController {
    private static final String NOME_PADRAO = "Paciente Teste";
    private static final String UNIDADE_PADRAO = "UBS Centro";
    private static final String ENDERECO_PADRAO = "Rua das Flores, 123 - Centro";
    private static final String EXEMPLO_PACIENTE = """
            {"telefone": "51993953661", "nomePaciente": "Paciente Teste", "preparo": "Jejum de 8 horas"}""";
    private static final String EXEMPLO_CUIDADOR = """
            {"telefone": "51993953661", "nomePaciente": "Paciente Teste", "nomeCuidador": "João Cuidador", "telefoneCuidador": "DDD+numero do cuidador"}""";
    private static final String EXEMPLO_VESPERA = """
            {"telefone": "51993953661", "preparo": "Jejum de 8 horas", "lembrete": "LEMBRETE_VESPERA"}""";

    private final AgendarAtendimentoUseCase agendarAtendimento;
    private final DispararLembreteUseCase dispararLembrete;
    private final ConsultarAgendamentoUseCase consultarAgendamento;
    private final ProcessarRespostaUseCase processarResposta;
    private final Clock clock;

    public AtendimentoTesteController(AgendarAtendimentoUseCase agendarAtendimento,
                                      DispararLembreteUseCase dispararLembrete,
                                      ConsultarAgendamentoUseCase consultarAgendamento,
                                      ProcessarRespostaUseCase processarResposta,
                                      Clock clock) {
        this.agendarAtendimento = agendarAtendimento;
        this.dispararLembrete = dispararLembrete;
        this.consultarAgendamento = consultarAgendamento;
        this.processarResposta = processarResposta;
        this.clock = clock;
    }

    @PostMapping
    @Operation(summary = "Cria um atendimento de teste para o número informado",
            description = "Só o telefone é obrigatório. Como no agendamento real, envia a confirmação e, dentro do prazo, o pedido de confirmação. "
                    + "Informe lembrete para enviar também outro lembrete na hora. Depois responda pelo WhatsApp (1, 2 ou 3) ou use o endpoint de simular resposta.")
    public ResponseEntity<AtendimentoTesteResponse> criar(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = {
                    @ExampleObject(name = "Somente paciente", value = EXEMPLO_PACIENTE),
                    @ExampleObject(name = "Paciente com cuidador", value = EXEMPLO_CUIDADOR),
                    @ExampleObject(name = "Com lembrete da véspera", value = EXEMPLO_VESPERA)}))
            @RequestBody AtendimentoTesteRequest request) {
        AtendimentoAgendado agendado = agendarAtendimento.agendar(new NovoAtendimento(
                Objects.requireNonNullElse(request.nomePaciente(), NOME_PADRAO),
                null,
                request.telefone(),
                request.nomeCuidador(),
                request.telefoneCuidador(),
                Objects.requireNonNullElseGet(request.dataHora(), () -> LocalDate.now(clock).plusDays(2).atTime(LocalTime.of(9, 30))),
                Objects.requireNonNullElse(request.tipo(), TipoAtendimento.CONSULTA),
                Objects.requireNonNullElse(request.unidade(), UNIDADE_PADRAO),
                Objects.requireNonNullElse(request.endereco(), ENDERECO_PADRAO),
                request.preparo()));

        UUID id = agendado.agendamento().getId();
        ResultadoDisparo lembrete = request.lembrete() == null ? null : dispararLembrete.disparar(id, request.lembrete(), true);

        Agendamento atual = consultarAgendamento.consultar(id).orElse(agendado.agendamento());
        return ResponseEntity.status(HttpStatus.CREATED).body(new AtendimentoTesteResponse(
                atual.getPacienteId(), agendado.pacienteNovo(), AgendamentoResponse.de(atual),
                agendado.confirmacao(), agendado.pedidoConfirmacao(), lembrete,
                proximoPasso(atual, agendado.confirmacao())));
    }

    @PostMapping("/respostas")
    @Operation(summary = "Simula a resposta do paciente sem usar o WhatsApp",
            description = "Equivale a uma mensagem recebida pelo webhook. Se a resposta for aplicada, a mensagem de retorno é enviada de verdade ao paciente.")
    public ResultadoResposta simularResposta(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = {
                    @ExampleObject(name = "Confirmar", value = "{\"telefone\": \"51993953661\", \"texto\": \"1\"}"),
                    @ExampleObject(name = "Cancelar", value = "{\"telefone\": \"51993953661\", \"texto\": \"2\"}"),
                    @ExampleObject(name = "Outro horário", value = "{\"telefone\": \"51993953661\", \"texto\": \"3\"}")}))
            @RequestBody SimularRespostaRequest request) {
        return processarResposta.processar(Telefone.de(request.telefone()), request.texto());
    }

    private static String proximoPasso(Agendamento agendamento, ResultadoDisparo confirmacao) {
        if (confirmacao.motivoNaoEnvio() != null) return "Confirmação não enviada: " + confirmacao.motivoNaoEnvio();
        if (confirmacao.envios().stream().noneMatch(e -> e.sucesso()))
            return "Falha no envio pelo WhatsApp: " + confirmacao.envios().getFirst().erro();
        if (agendamento.aguardandoResposta())
            return "Responda 1, 2 ou 3 no WhatsApp (ou use POST /api/teste/atendimentos/respostas) e consulte GET /api/agendamentos/" + agendamento.getId();
        return "Confirmação enviada. O pedido de confirmação sai pela rotina diária; para enviar agora use POST /api/agendamentos/"
                + agendamento.getId() + "/disparos?tipo=PEDIDO_CONFIRMACAO";
    }
}
