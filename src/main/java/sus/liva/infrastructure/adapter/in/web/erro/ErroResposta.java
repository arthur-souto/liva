package sus.liva.infrastructure.adapter.in.web.erro;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

public record ErroResposta(Instant timestamp, int status, CodigoErro codigo, String mensagem, String caminho) {

    public static ErroResposta de(CodigoErro codigo, String mensagem, String caminho) {
        return new ErroResposta(Instant.now(), codigo.getStatus().value(), codigo, mensagem, caminho);
    }

    public static ResponseEntity<ErroResposta> resposta(HttpStatusCode status, String mensagem, String caminho) {
        return ResponseEntity.status(status)
                .body(new ErroResposta(Instant.now(), status.value(), CodigoErro.de(status), mensagem, caminho));
    }

    public static ResponseEntity<ErroResposta> resposta(CodigoErro codigo, String mensagem, String caminho) {
        return ResponseEntity.status(codigo.getStatus()).body(de(codigo, mensagem, caminho));
    }
}
