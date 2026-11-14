package sus.liva.infrastructure.adapter.in.web.seguranca;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import sus.liva.infrastructure.adapter.in.web.erro.CodigoErro;
import sus.liva.infrastructure.adapter.in.web.erro.ErroResposta;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class RespostasDeSeguranca implements AuthenticationEntryPoint, AccessDeniedHandler {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException {
        escrever(response, request, CodigoErro.NAO_AUTORIZADO, "Sessão ausente ou expirada. Entre novamente.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e) throws IOException {
        escrever(response, request, CodigoErro.ACESSO_NEGADO, "Seu perfil não tem permissão para esta operação");
    }

    private static void escrever(HttpServletResponse response, HttpServletRequest request, CodigoErro codigo, String mensagem)
            throws IOException {
        response.setStatus(codigo.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        JSON.writeValue(response.getWriter(), ErroResposta.de(codigo, mensagem, request.getRequestURI()));
    }
}
