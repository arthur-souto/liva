package sus.liva.infrastructure.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import sus.liva.application.port.in.ConfirmacaoDeEntregaUseCase;
import sus.liva.application.port.in.ProcessarRespostaUseCase;
import sus.liva.application.port.in.ResultadoResposta;
import sus.liva.domain.model.Telefone;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/webhooks/evolution")
@Tag(name = "Webhook", description = "Recebimento de mensagens da Evolution API")
public class EvolutionWebhookController {
    private static final Logger log = LoggerFactory.getLogger(EvolutionWebhookController.class);
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String SUFIXO_CONTATO = "@s.whatsapp.net";
    private static final Set<String> ENTREGUE = Set.of("DELIVERY_ACK", "READ", "PLAYED", "3", "4", "5");

    private final ProcessarRespostaUseCase processarResposta;
    private final ConfirmacaoDeEntregaUseCase entregas;
    private final String token;
    private final boolean aceitarProprias;

    public EvolutionWebhookController(ProcessarRespostaUseCase processarResposta,
                                      @Value("${confirmasus.webhook.token:}") String token,
                                      @Value("${confirmasus.webhook.aceitar-mensagens-proprias:false}") boolean aceitarProprias) {
        this(processarResposta, null, token, aceitarProprias);
    }

    @Autowired
    public EvolutionWebhookController(ProcessarRespostaUseCase processarResposta, ConfirmacaoDeEntregaUseCase entregas,
                                      @Value("${confirmasus.webhook.token:}") String token,
                                      @Value("${confirmasus.webhook.aceitar-mensagens-proprias:false}") boolean aceitarProprias) {
        this.processarResposta = processarResposta;
        this.entregas = entregas;
        this.token = token;
        this.aceitarProprias = aceitarProprias;
    }

    @SecurityRequirements
    @PostMapping
    @Operation(summary = "Recebe eventos da Evolution e processa a resposta do paciente",
            description = "Chamado pela Evolution. Exige ?token=WEBHOOK_TOKEN (401 caso contrário). Sempre responde 200 com o resultado do processamento.")
    public ResponseEntity<ResultadoResposta> receber(@RequestParam(required = false) String token,
                                                     @RequestBody String corpo) {
        if (!tokenValido(token)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token do webhook ausente ou inválido");

        JsonNode evento;
        try {
            evento = JSON.readTree(corpo);
        } catch (JacksonException e) {
            evento = JSON.missingNode();
        }
        ResultadoResposta resultado = interpretarEvento(evento);
        JsonNode chave = evento.path("data").path("key");
        log.info("Webhook Evolution [evento={}, jid={}, jidAlt={}, fromMe={}]: {}",
                evento.path("event").asString("-"),
                chave.path("remoteJid").asString("-"),
                chave.path("remoteJidAlt").asString(chave.path("senderPn").asString("-")),
                chave.path("fromMe").asBoolean(false),
                resultado);
        return ResponseEntity.ok(resultado);
    }

    private ResultadoResposta interpretarEvento(JsonNode evento) {
        if (evento.isMissingNode()) return ResultadoResposta.ignorada("Payload inválido");
        if ("messages.update".equals(evento.path("event").asString(""))) return registrarEntregas(evento.path("data"));
        if (!"messages.upsert".equals(evento.path("event").asString(""))) return ResultadoResposta.ignorada("Evento ignorado");

        JsonNode dados = evento.path("data");
        JsonNode chave = dados.path("key");
        if (chave.path("fromMe").asBoolean(false) && !aceitarProprias)
            return ResultadoResposta.ignorada("Mensagem enviada pela própria instância");

        Optional<Telefone> remetente = extrairTelefone(chave);
        if (remetente.isEmpty()) return ResultadoResposta.ignorada("Remetente não é um contato individual");

        String texto = extrairTexto(dados.path("message"));
        if (texto == null) return ResultadoResposta.ignorada("Mensagem sem texto");

        return processarResposta.processar(remetente.get(), texto, chave.path("id").asString(null));
    }

    private ResultadoResposta registrarEntregas(JsonNode dados) {
        if (entregas == null) return ResultadoResposta.ignorada("Evento ignorado");

        long confirmadas = (dados.isArray() ? dados.valueStream() : Stream.of(dados))
                .filter(item -> ENTREGUE.contains(item.path("status").asString(item.path("update").path("status").asString(""))))
                .map(item -> item.path("keyId").asString(item.path("key").path("id").asString(null)))
                .filter(entregas::registrarEntrega)
                .count();
        return ResultadoResposta.ignorada("Confirmação de entrega registrada em " + confirmadas + " mensagem(ns)");
    }

    private static Optional<Telefone> extrairTelefone(JsonNode chave) {
        return Stream.of("remoteJidAlt", "senderPn", "remoteJid")
                .map(campo -> chave.path(campo).asString(""))
                .filter(jid -> jid.endsWith(SUFIXO_CONTATO))
                .map(jid -> jid.substring(0, jid.length() - SUFIXO_CONTATO.length()))
                .flatMap(numero -> {
                    try {
                        return Stream.of(Telefone.de(numero));
                    } catch (IllegalArgumentException e) {
                        return Stream.empty();
                    }
                })
                .findFirst();
    }

    private static String extrairTexto(JsonNode mensagem) {
        String conversa = mensagem.path("conversation").asString(null);
        if (conversa != null) return conversa;
        return mensagem.path("extendedTextMessage").path("text").asString(null);
    }

    private boolean tokenValido(String recebido) {
        if (token == null || token.isBlank() || recebido == null) return false;
        return MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), recebido.getBytes(StandardCharsets.UTF_8));
    }
}
