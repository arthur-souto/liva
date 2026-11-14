package sus.liva.infrastructure.adapter.in.web.seguranca;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import sus.liva.infrastructure.adapter.in.web.erro.CodigoErro;
import sus.liva.infrastructure.adapter.in.web.erro.ErroResposta;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class ChaveApiFiltro extends OncePerRequestFilter {
    public static final String CABECALHO = "X-API-Key";
    private static final String MENSAGEM_NEGADA = "Chave de API ausente ou inválida. Envie o header " + CABECALHO + ".";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final byte[] chave;

    public ChaveApiFiltro(@Value("${confirmasus.seguranca.api-key:}") String chave) {
        if (chave == null || chave.isBlank())
            throw new IllegalStateException("API_KEY não configurada. Defina API_KEY no .env (ex.: openssl rand -hex 32)");
        this.chave = chave.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String caminho = request.getRequestURI().substring(request.getContextPath().length());
        return !caminho.startsWith("/api/") || caminho.startsWith("/api/webhooks/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String recebida = request.getHeader(CABECALHO);
        if (recebida != null && MessageDigest.isEqual(chave, recebida.getBytes(StandardCharsets.UTF_8))) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        JSON.writeValue(response.getWriter(), ErroResposta.de(CodigoErro.NAO_AUTORIZADO, MENSAGEM_NEGADA, request.getRequestURI()));
    }
}
