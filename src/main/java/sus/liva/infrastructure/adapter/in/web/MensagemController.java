package sus.liva.infrastructure.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sus.liva.application.port.in.EnviarMensagemUseCase;
import sus.liva.application.port.out.ResultadoEnvio;

@RestController
@RequestMapping("/api/mensagens")
@Tag(name = "Mensagens", description = "Envio avulso de mensagens pelo WhatsApp")
public class MensagemController {
    private final EnviarMensagemUseCase enviarMensagem;

    public MensagemController(EnviarMensagemUseCase enviarMensagem) {
        this.enviarMensagem = enviarMensagem;
    }

    @PostMapping
    @Operation(summary = "Envia uma mensagem de texto para um número",
            description = "200 se enviada, 502 se a Evolution falhou, 400 se o telefone ou o texto forem inválidos.")
    public ResponseEntity<ResultadoEnvio> enviar(@RequestBody EnviarMensagemRequest request) {
        ResultadoEnvio resultado = enviarMensagem.enviar(request.telefone(), request.texto());
        HttpStatus status = resultado.sucesso() ? HttpStatus.OK : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status).body(resultado);
    }
}
