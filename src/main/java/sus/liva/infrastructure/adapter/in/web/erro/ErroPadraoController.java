package sus.liva.infrastructure.adapter.in.web.erro;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Hidden
@RestController
public class ErroPadraoController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<ErroResposta> erro(HttpServletRequest request) {
        Object codigo = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        HttpStatus status = codigo instanceof Integer c && HttpStatus.resolve(c) != null
                ? HttpStatus.resolve(c)
                : HttpStatus.INTERNAL_SERVER_ERROR;
        Object caminho = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        String mensagem = status.is5xxServerError() ? "Erro interno. Consulte o log da Liva." : status.getReasonPhrase();

        return ErroResposta.resposta(status, mensagem, caminho == null ? null : caminho.toString());
    }
}
