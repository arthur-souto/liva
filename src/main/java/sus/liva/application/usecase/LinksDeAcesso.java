package sus.liva.application.usecase;

import sus.liva.application.port.out.LinkDeAcesso;
import sus.liva.application.port.out.LinkDeAcessoPort;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

public class LinksDeAcesso {
    public static final Duration VALIDADE = Duration.ofMinutes(30);

    private final LinkDeAcessoPort links;
    private final Clock clock;
    private final String urlDoPortal;
    private final SecureRandom aleatorio = new SecureRandom();

    public LinksDeAcesso(LinkDeAcessoPort links, Clock clock, String urlDoPortal) {
        this.links = links;
        this.clock = clock;
        this.urlDoPortal = urlDoPortal.endsWith("/") ? urlDoPortal.substring(0, urlDoPortal.length() - 1) : urlDoPortal;
    }

    public String gerar(UUID pacienteId, String destino) {
        byte[] bytes = new byte[32];
        aleatorio.nextBytes(bytes);

        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant agora = Instant.now(clock);

        links.salvarLink(new LinkDeAcesso(hash(token), pacienteId, destino, agora, agora.plus(VALIDADE), null));
        return urlDoPortal + "/acesso/link?t=" + token;
    }

    static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
