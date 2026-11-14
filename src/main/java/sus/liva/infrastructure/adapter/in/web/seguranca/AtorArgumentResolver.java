package sus.liva.infrastructure.adapter.in.web.seguranca;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import sus.liva.application.exception.NaoAutenticadoException;
import sus.liva.application.port.in.Ator;
import sus.liva.application.port.in.Papel;

import java.util.UUID;

public class AtorArgumentResolver implements HandlerMethodArgumentResolver {
    public static final String PAPEL = "papel";
    public static final String PROFISSIONAL = "profissional";
    public static final String PACIENTE = "paciente";
    public static final String NOME = "nome";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return Ator.class.equals(parameter.getParameterType());
    }

    @Override
    public Ator resolveArgument(MethodParameter parameter, ModelAndViewContainer mav, NativeWebRequest request,
                                WebDataBinderFactory binderFactory) {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (!(autenticacao instanceof JwtAuthenticationToken token))
            throw new NaoAutenticadoException("Entre com um usuário para continuar");
        return deToken(token.getToken());
    }

    public static Ator deToken(Jwt jwt) {
        return new Ator(UUID.fromString(jwt.getSubject()), Papel.valueOf(jwt.getClaimAsString(PAPEL)),
                uuid(jwt.getClaimAsString(PROFISSIONAL)), uuid(jwt.getClaimAsString(PACIENTE)));
    }

    private static UUID uuid(String valor) {
        return valor == null ? null : UUID.fromString(valor);
    }
}
