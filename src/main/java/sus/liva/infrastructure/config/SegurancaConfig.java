package sus.liva.infrastructure.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import sus.liva.infrastructure.adapter.in.web.seguranca.AtorArgumentResolver;
import sus.liva.infrastructure.adapter.in.web.seguranca.ChaveApiFiltro;
import sus.liva.infrastructure.adapter.in.web.seguranca.RespostasDeSeguranca;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

@Configuration
public class SegurancaConfig implements WebMvcConfigurer {
    public static final String EMISSOR = "liva";
    private static final int TAMANHO_MINIMO_DO_SEGREDO = 32;

    private final SecretKey chaveJwt;

    public SegurancaConfig(@Value("${confirmasus.seguranca.jwt-segredo:}") String segredo) {
        if (segredo == null || segredo.getBytes(StandardCharsets.UTF_8).length < TAMANHO_MINIMO_DO_SEGREDO)
            throw new IllegalStateException("JWT_SEGREDO ausente ou curto. Defina no .env ao menos 32 caracteres (ex.: openssl rand -hex 32)");
        this.chaveJwt = new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chaveJwt));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chaveJwt).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(EMISSOR));
        return decoder;
    }

    @Bean
    public SecurityFilterChain seguranca(HttpSecurity http, JwtDecoder decoder,
                                         @Value("${confirmasus.seguranca.api-key:}") String chaveApi) {
        RespostasDeSeguranca respostas = new RespostasDeSeguranca();
        JwtGrantedAuthoritiesConverter papeis = new JwtGrantedAuthoritiesConverter();
        papeis.setAuthoritiesClaimName(AtorArgumentResolver.PAPEL);
        papeis.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(papeis);
        String[] rotasDeSistema = ChaveApiFiltro.ROTAS_DE_SISTEMA.stream()
                .flatMap(r -> Stream.of(r, r + "/**")).toArray(String[]::new);

        http.csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/api/auth/entrar", "/api/auth/link", "/api/auth/portal/**", "/api/auth/cuidador/**", "/api/webhooks/**").permitAll()
                        .requestMatchers(rotasDeSistema).hasAnyRole(ChaveApiFiltro.PAPEL_SISTEMA, "ADMINISTRADOR", "RECEPCAO")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .oauth2ResourceServer(o -> o
                        .jwt(j -> j.decoder(decoder).jwtAuthenticationConverter(conversor))
                        .authenticationEntryPoint(respostas)
                        .accessDeniedHandler(respostas))
                .exceptionHandling(e -> e.authenticationEntryPoint(respostas).accessDeniedHandler(respostas))
                .addFilterBefore(new ChaveApiFiltro(chaveApi), BearerTokenAuthenticationFilter.class);
        return http.build();
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new AtorArgumentResolver());
    }
}
