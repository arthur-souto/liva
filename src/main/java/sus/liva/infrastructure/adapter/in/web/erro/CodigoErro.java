package sus.liva.infrastructure.adapter.in.web.erro;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.util.Arrays;

public enum CodigoErro {
    REQUISICAO_INVALIDA(HttpStatus.BAD_REQUEST),
    NAO_AUTORIZADO(HttpStatus.UNAUTHORIZED),
    ACESSO_NEGADO(HttpStatus.FORBIDDEN),
    NAO_ENCONTRADO(HttpStatus.NOT_FOUND),
    METODO_NAO_PERMITIDO(HttpStatus.METHOD_NOT_ALLOWED),
    CONFLITO(HttpStatus.CONFLICT),
    TIPO_DE_CONTEUDO_NAO_SUPORTADO(HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    ERRO_INTERNO(HttpStatus.INTERNAL_SERVER_ERROR),
    FALHA_WHATSAPP(HttpStatus.BAD_GATEWAY),
    SERVICO_INDISPONIVEL(HttpStatus.SERVICE_UNAVAILABLE);

    private final HttpStatus status;

    CodigoErro(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static CodigoErro de(HttpStatusCode status) {
        return Arrays.stream(values())
                .filter(c -> c.status.value() == status.value())
                .findFirst()
                .orElse(status.is4xxClientError() ? REQUISICAO_INVALIDA : ERRO_INTERNO);
    }
}
