package sus.liva.infrastructure.adapter.out.seguranca;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;
import sus.liva.application.port.in.Ator;
import sus.liva.application.port.out.EmissorDeTokenPort;
import sus.liva.application.port.out.TokenEmitido;
import sus.liva.infrastructure.adapter.in.web.seguranca.AtorArgumentResolver;
import sus.liva.infrastructure.config.SegurancaConfig;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Component
public class JwtEmissorAdapter implements EmissorDeTokenPort {
    private final JwtEncoder encoder;
    private final Clock clock;

    JwtEmissorAdapter(JwtEncoder encoder, Clock clock) {
        this.encoder = encoder;
        this.clock = clock;
    }

    @Override
    public TokenEmitido emitir(Ator ator, String nome, Duration validade) {
        Instant agora = Instant.now(clock);
        Instant expiraEm = agora.plus(validade);
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(SegurancaConfig.EMISSOR)
                .id(UUID.randomUUID().toString())
                .subject(ator.usuarioId().toString())
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .claim(AtorArgumentResolver.PAPEL, ator.papel().name())
                .claim(AtorArgumentResolver.NOME, nome);

        if (ator.profissionalId() != null) claims.claim(AtorArgumentResolver.PROFISSIONAL, ator.profissionalId().toString());
        if (ator.pacienteId() != null) claims.claim(AtorArgumentResolver.PACIENTE, ator.pacienteId().toString());
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build()))
                .getTokenValue();
        return new TokenEmitido(token, expiraEm);
    }
}
