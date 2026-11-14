package sus.liva.infrastructure.adapter.out.whatsapp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import sus.liva.application.port.out.EnviadorMensagemPort;
import sus.liva.application.port.out.MensagemSaida;
import sus.liva.application.port.out.ResultadoEnvio;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "confirmasus.whatsapp.provedor", havingValue = "evolution")
public class EvolutionEnviadorAdapter implements EnviadorMensagemPort {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final RestClient restClient;
    private final String instancia;

    @Autowired
    public EvolutionEnviadorAdapter(@Value("${evolution.url}") String url,
                                    @Value("${evolution.api-key}") String apiKey,
                                    @Value("${evolution.instancia}") String instancia) {
        this(RestClient.builder().requestFactory(requestFactory()), url, apiKey, instancia);
    }

    EvolutionEnviadorAdapter(RestClient.Builder builder, String url, String apiKey, String instancia) {
        this.restClient = builder
                .baseUrl(url)
                .defaultHeader("apikey", apiKey)
                .build();
        this.instancia = instancia;
    }

    @Override
    public ResultadoEnvio enviar(MensagemSaida mensagem) {
        try {
            ResponseEntity<String> resposta = restClient.post()
                    .uri("/message/sendText/{instancia}", instancia)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(Map.of("number", mensagem.destino().apenasDigitos(), "text", mensagem.texto()))
                    .retrieve()
                    .toEntity(String.class);
            return interpretar(resposta);
        } catch (RestClientResponseException e) {
            return ResultadoEnvio.falha("HTTP " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
        } catch (RestClientException e) {
            return ResultadoEnvio.falha(e.getMessage());
        }
    }

    private static ResultadoEnvio interpretar(ResponseEntity<String> resposta) {
        String corpo = resposta.getBody();
        String status = "HTTP " + resposta.getStatusCode().value();
        if (corpo == null || corpo.isBlank())
            return ResultadoEnvio.falha(status + ": Evolution respondeu sem corpo");
        try {
            JsonNode id = JSON.readTree(corpo).path("key").path("id");
            return ResultadoEnvio.ok(id.isValueNode() ? id.asString() : null);
        } catch (JacksonException e) {
            return ResultadoEnvio.falha(status + ": resposta inesperada da Evolution: " + corpo);
        }
    }

    private static SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(15));
        return factory;
    }
}
