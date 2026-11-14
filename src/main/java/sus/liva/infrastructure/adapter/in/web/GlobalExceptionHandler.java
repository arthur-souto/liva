package sus.liva.infrastructure.adapter.in.web;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import sus.liva.application.exception.NaoEncontradoException;
import sus.liva.infrastructure.adapter.in.web.erro.CodigoErro;
import sus.liva.infrastructure.adapter.in.web.erro.ErroResposta;

import java.util.Arrays;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroResposta> argumentoInvalido(IllegalArgumentException e, HttpServletRequest request) {
        return ErroResposta.resposta(CodigoErro.REQUISICAO_INVALIDA, e.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> corpoInvalido(HttpMessageNotReadableException e, HttpServletRequest request) {
        return ErroResposta.resposta(CodigoErro.REQUISICAO_INVALIDA,
                "Corpo da requisição ausente ou inválido. Confira o JSON, as datas (AAAA-MM-DDTHH:MM:SS) e os valores permitidos.",
                request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResposta> parametroInvalido(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String esperado = e.getRequiredType() == null ? "" : descreverTipo(e.getRequiredType());
        return ErroResposta.resposta(CodigoErro.REQUISICAO_INVALIDA,
                "Valor inválido para '" + e.getName() + "': \"" + e.getValue() + "\"" + esperado, request.getRequestURI());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErroResposta> parametroAusente(MissingServletRequestParameterException e, HttpServletRequest request) {
        return ErroResposta.resposta(CodigoErro.REQUISICAO_INVALIDA,
                "Parâmetro obrigatório ausente: " + e.getParameterName(), request.getRequestURI());
    }

    @ExceptionHandler(NaoEncontradoException.class)
    public ResponseEntity<ErroResposta> naoEncontrado(NaoEncontradoException e, HttpServletRequest request) {
        return ErroResposta.resposta(CodigoErro.NAO_ENCONTRADO, e.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResposta> rotaInexistente(NoResourceFoundException e, HttpServletRequest request) {
        return ErroResposta.resposta(CodigoErro.NAO_ENCONTRADO, "Rota não encontrada", request.getRequestURI());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResposta> metodoNaoPermitido(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        return ErroResposta.resposta(CodigoErro.METODO_NAO_PERMITIDO,
                "Método " + e.getMethod() + " não é aceito nesta rota", request.getRequestURI());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErroResposta> tipoNaoSuportado(HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        return ErroResposta.resposta(CodigoErro.TIPO_DE_CONTEUDO_NAO_SUPORTADO,
                "Envie o corpo como application/json", request.getRequestURI());
    }

    @ExceptionHandler({IllegalStateException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ErroResposta> conflito(RuntimeException e, HttpServletRequest request) {
        log.warn("Conflito em {}: {}", request.getRequestURI(), e.getMessage());
        String mensagem = e instanceof DataIntegrityViolationException
                ? "Os dados conflitam com um registro existente (ex.: CPF já cadastrado)"
                : e.getMessage();
        return ErroResposta.resposta(CodigoErro.CONFLITO, mensagem, request.getRequestURI());
    }

    @ExceptionHandler(DataAccessResourceFailureException.class)
    public ResponseEntity<ErroResposta> bancoIndisponivel(DataAccessResourceFailureException e, HttpServletRequest request) {
        log.error("Banco indisponível em {}", request.getRequestURI(), e);
        return ErroResposta.resposta(CodigoErro.SERVICO_INDISPONIVEL,
                "Banco de dados indisponível. Tente novamente em instantes.", request.getRequestURI());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErroResposta> comStatus(ResponseStatusException e, HttpServletRequest request) {
        return ErroResposta.resposta(e.getStatusCode(), e.getReason(), request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResposta> inesperado(Exception e, HttpServletRequest request) {
        log.error("Erro inesperado em {} {}", request.getMethod(), request.getRequestURI(), e);
        return ErroResposta.resposta(CodigoErro.ERRO_INTERNO,
                "Erro interno. Consulte o log da Liva.", request.getRequestURI());
    }

    private static String descreverTipo(Class<?> tipo) {
        if (tipo.isEnum())
            return ". Valores aceitos: " + Arrays.stream(tipo.getEnumConstants()).map(Object::toString).collect(Collectors.joining(", "));
        return switch (tipo.getSimpleName()) {
            case "UUID" -> ". Esperado um UUID, ex.: 3f2b8c1e-7a4d-4c9b-9e21-5d6f0a8b1c02";
            case "LocalDate" -> ". Esperada uma data AAAA-MM-DD";
            default -> "";
        };
    }
}
