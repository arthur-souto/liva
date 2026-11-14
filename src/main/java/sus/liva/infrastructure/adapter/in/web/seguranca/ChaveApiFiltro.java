package sus.liva.infrastructure.adapter.in.web.seguranca;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import sus.liva.infrastructure.adapter.in.web.erro.CodigoErro;
import sus.liva.infrastructure.adapter.in.web.erro.ErroResposta;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

public class ChaveApiFiltro extends OncePerRequestFilter {
    public static final String CABECALHO = "X-API-Key";
    public static final String PAPEL_SISTEMA = "SISTEMA";
    public static final List<String> ROTAS_DE_SISTEMA = List.of("/api/agendamentos", "/api/mensagens", "/api/disparos", "/api/teste");
    private static final String MENSAGEM_NEGADA = "Chave de API ausente ou inválida. Envie o header " + CABECALHO + " ou entre com um usuário da equipe.";
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final byte[] chave;

    public ChaveApiFiltro(String chave) {
        if (chave == null || chave.isBlank())
            throw new IllegalStateException("API_KEY não configurada. Defina API_KEY no .env (ex.: openssl rand -hex 32)");
        this.chave = chave.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String caminho = request.getRequestURI().substring(request.getContextPath().length());
        String autorizacao = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (autorizacao != null && autorizacao.regionMatches(true, 0, "Bearer ", 0, 7)) return true;
        return ROTAS_DE_SISTEMA.stream().noneMatch(r -> caminho.equals(r) || caminho.startsWith(r + "/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String recebida = request.getHeader(CABECALHO);
        if (recebida != null && MessageDigest.isEqual(chave, recebida.getBytes(StandardCharsets.UTF_8))) {
            SecurityContext contexto = SecurityContextHolder.createEmptyContext();
            contexto.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(PAPEL_SISTEMA, null,
                    AuthorityUtils.createAuthorityList("ROLE_" + PAPEL_SISTEMA)));
            SecurityContextHolder.setContext(contexto);
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        JSON.writeValue(response.getWriter(), ErroResposta.de(CodigoErro.NAO_AUTORIZADO, MENSAGEM_NEGADA, request.getRequestURI()));
    }
}
