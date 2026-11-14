package sus.liva.infrastructure.adapter.in.web.seguranca;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import sus.liva.TestcontainersConfig;
import sus.liva.application.port.in.Papel;
import sus.liva.application.port.out.CifradorPort;
import sus.liva.application.port.out.ContaDeAcesso;
import sus.liva.application.port.out.ContaDeAcessoPort;
import sus.liva.application.port.out.EnviadorMensagemPort;
import sus.liva.application.port.out.MensagemSaida;
import sus.liva.application.port.out.ProfissionalRepositoryPort;
import sus.liva.application.port.out.ResultadoEnvio;
import sus.liva.domain.model.profissional.Disponibilidade;
import sus.liva.domain.model.profissional.Profissional;
import sus.liva.domain.model.profissional.TipoProfissional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"confirmasus.whatsapp.provedor=console", "confirmasus.disparo-diario.cron=-",
        "confirmasus.seguranca.api-key=chave-de-teste",
        "confirmasus.seguranca.jwt-segredo=segredo-de-teste-com-pelo-menos-32-caracteres"})
@Import({TestcontainersConfig.class, SegurancaIntegracaoTest.WhatsappDeTeste.class})
class SegurancaIntegracaoTest {

    private static final String SENHA = "senha-forte-1";
    private static final Pattern CODIGO = Pattern.compile("é (\\d{6})\\.");

    @TestConfiguration(proxyBeanMethods = false)
    static class WhatsappDeTeste {
        @Bean
        @Primary
        Caixa caixa() {
            return new Caixa();
        }
    }

    static class Caixa implements EnviadorMensagemPort {
        final List<MensagemSaida> enviadas = new CopyOnWriteArrayList<>();

        @Override
        public ResultadoEnvio enviar(MensagemSaida mensagem) {
            enviadas.add(mensagem);
            return ResultadoEnvio.ok("teste-" + UUID.randomUUID());
        }

        List<MensagemSaida> para(String telefone) {
            return enviadas.stream().filter(m -> m.destino().apenasDigitos().endsWith(telefone.substring(2))).toList();
        }
    }

    @Autowired private WebApplicationContext contexto;
    @Autowired private ContaDeAcessoPort contas;
    @Autowired private CifradorPort cifrador;
    @Autowired private ProfissionalRepositoryPort profissionais;
    @Autowired private Caixa caixa;
    @Autowired private Clock clock;

    private MockMvc mvc;
    private Profissional clinico;
    private Profissional ortopedista;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        clinico = profissional(TipoProfissional.CLINICO_GERAL, null);
        ortopedista = profissional(TipoProfissional.ESPECIALISTA, "ORTOPEDIA");
        LocalDate amanha = LocalDate.now(clock).plusDays(1);
        profissionais.salvarDisponibilidade(new Disponibilidade(UUID.randomUUID(), ortopedista.id(), amanha.getDayOfWeek(),
                LocalTime.of(8, 0), LocalTime.of(12, 0), 30));
    }

    @Test
    void rotaClinicaSemTokenResponde401() throws Exception {
        mvc.perform(get("/api/triagens/fila"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTORIZADO"));
    }

    @Test
    void chaveDeApiValeSomenteParaAsRotasDeSistema() throws Exception {
        mvc.perform(get("/api/agendamentos").header(ChaveApiFiltro.CABECALHO, "chave-de-teste")).andExpect(status().isOk());
        mvc.perform(get("/api/triagens/fila").header(ChaveApiFiltro.CABECALHO, "chave-de-teste")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/agendamentos")).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenAdulteradoEhRecusado() throws Exception {
        String token = entrar(conta(Papel.RECEPCAO, null));
        String adulterado = token.substring(0, token.length() - 4) + (token.endsWith("AAAA") ? "BBBB" : "AAAA");

        mvc.perform(get("/api/triagens/fila").header(HttpHeaders.AUTHORIZATION, "Bearer " + adulterado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cadaPapelAcessaApenasOQueLheCabe() throws Exception {
        String recepcao = entrar(conta(Papel.RECEPCAO, null));
        String medico = entrar(conta(Papel.CLINICO_GERAL, clinico.id()));

        mvc.perform(autenticado(get("/api/agendamentos"), recepcao)).andExpect(status().isOk());
        mvc.perform(autenticado(get("/api/agendamentos"), medico)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
        mvc.perform(autenticado(get("/api/triagens/fila"), medico)).andExpect(status().isOk());
        mvc.perform(autenticado(post("/api/pacientes").content(pacienteJson(cpfValido(), telefoneAleatorio())), medico))
                .andExpect(status().isForbidden());
        mvc.perform(autenticado(get("/api/auth/eu"), medico))
                .andExpect(jsonPath("$.papel").value("CLINICO_GERAL"))
                .andExpect(jsonPath("$.profissionalId").value(clinico.id().toString()));
    }

    @Test
    void cincoSenhasErradasBloqueiamOAcesso() throws Exception {
        String login = conta(Papel.RECEPCAO, null);
        for (int i = 0; i < 5; i++)
            mvc.perform(json(post("/api/auth/entrar"), "{\"login\":\"%s\",\"senha\":\"errada-%d\"}".formatted(login, i)))
                    .andExpect(status().isUnauthorized());

        mvc.perform(json(post("/api/auth/entrar"), "{\"login\":\"%s\",\"senha\":\"%s\"}".formatted(login, SENHA)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value(containsString("bloqueado")));
    }

    @Test
    void portalNaoEnviaCodigoAntesDaPrimeiraTriagem() throws Exception {
        String recepcao = entrar(conta(Papel.RECEPCAO, null));
        String cpf = cpfValido();
        String telefone = telefoneAleatorio();
        mvc.perform(autenticado(post("/api/pacientes").content(pacienteJson(cpf, telefone)), recepcao)).andExpect(status().isCreated());

        mvc.perform(json(post("/api/auth/portal/codigo"), "{\"documento\":\"%s\",\"dataNascimento\":\"1975-04-02\"}".formatted(cpf)))
                .andExpect(status().isAccepted());

        assertThat(caixa.para(telefone)).isEmpty();
    }

    @Test
    void fluxoCompletoPelaApiComPortalDoPaciente() throws Exception {
        String recepcao = entrar(conta(Papel.RECEPCAO, null));
        String medico = entrar(conta(Papel.CLINICO_GERAL, clinico.id()));
        String especialista = entrar(conta(Papel.ESPECIALISTA, ortopedista.id()));
        String cpf = cpfValido();
        String telefone = telefoneAleatorio();

        String pacienteId = ler(mvc.perform(autenticado(post("/api/pacientes").content(pacienteJson(cpf, telefone)), recepcao))
                .andExpect(status().isCreated()), "$.id");
        String triagemId = ler(mvc.perform(autenticado(post("/api/triagens").content("{\"pacienteId\":\"%s\"}".formatted(pacienteId)), recepcao))
                .andExpect(status().isCreated()), "$.id");

        mvc.perform(autenticado(post("/api/triagens/{id}/iniciar", triagemId), medico)).andExpect(status().isOk());
        String versao = ler(mvc.perform(autenticado(put("/api/triagens/{id}/avaliacao", triagemId).content("""
                        {"avaliacao":{"queixaPrincipal":"Dor no joelho"},"sinaisVitais":{"pressaoSistolica":120,"pressaoDiastolica":80,
                         "frequenciaCardiaca":72,"frequenciaRespiratoria":16,"temperatura":36.5,"saturacao":98},
                         "prioridade":"POUCO_URGENTE","versao":1}"""), medico)).andExpect(status().isOk()), "$.versao");
        String encaminhamentoId = ler(mvc.perform(autenticado(post("/api/triagens/{id}/encaminhamentos", triagemId).content("""
                        {"especialidade":"ORTOPEDIA","profissionalDestinoId":"%s","modalidade":"ELETIVO","prioridade":"POUCO_URGENTE",
                         "justificativa":"Avaliar lesão"}""".formatted(ortopedista.id())), medico))
                .andExpect(status().isCreated()), "$.id");
        mvc.perform(autenticado(post("/api/triagens/{id}/finalizar", triagemId).content("{\"versao\":%s}".formatted(versao)), medico))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADA"));

        String solicitar = "{\"documento\":\"%s\",\"dataNascimento\":\"1975-04-02\"}".formatted(cpf);
        mvc.perform(json(post("/api/auth/portal/codigo"), "{\"documento\":\"%s\",\"dataNascimento\":\"1975-04-03\"}".formatted(cpf)))
                .andExpect(status().isAccepted());
        assertThat(caixa.para(telefone)).isEmpty();
        mvc.perform(json(post("/api/auth/portal/codigo"), solicitar)).andExpect(status().isAccepted());
        mvc.perform(json(post("/api/auth/portal/codigo"), solicitar)).andExpect(status().isAccepted());
        assertThat(caixa.para(telefone)).hasSize(1);
        String codigo = codigoEnviado(telefone);

        String errado = codigo.equals("000000") ? "111111" : "000000";
        mvc.perform(json(post("/api/auth/portal/ativar"), ativacao(cpf, errado))).andExpect(status().isBadRequest());
        String paciente = ler(mvc.perform(json(post("/api/auth/portal/ativar"), ativacao(cpf, codigo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.papel").value("PACIENTE")), "$.token");
        mvc.perform(json(post("/api/auth/portal/ativar"), ativacao(cpf, codigo))).andExpect(status().isBadRequest());
        assertThat(entrarComo(cpf, SENHA)).isNotBlank();

        mvc.perform(autenticado(get("/api/portal/historico"), paciente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ramos", hasSize(1)))
                .andExpect(jsonPath("$.ramos[0].triagem.queixaPrincipal").value("Dor no joelho"));
        mvc.perform(autenticado(get("/api/triagens/fila"), paciente)).andExpect(status().isForbidden());
        String outroPaciente = ler(mvc.perform(autenticado(post("/api/pacientes").content(pacienteJson(cpfValido(), telefoneAleatorio())), recepcao)), "$.id");
        mvc.perform(autenticado(get("/api/pacientes/{id}/historico", outroPaciente), paciente)).andExpect(status().isForbidden());

        LocalDate amanha = LocalDate.now(clock).plusDays(1);
        mvc.perform(autenticado(get("/api/encaminhamentos/{id}/horarios", encaminhamentoId)
                        .param("inicio", amanha.toString()).param("fim", amanha.toString()), paciente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(8)));
        String corpo = "{\"profissionalId\":\"%s\",\"inicio\":\"%sT09:00:00\"}".formatted(ortopedista.id(), amanha);
        String chave = "portal-" + UUID.randomUUID();
        String consultaId = ler(mvc.perform(autenticado(post("/api/encaminhamentos/{id}/consultas", encaminhamentoId)
                .header("Idempotency-Key", chave).content(corpo), paciente)).andExpect(status().isCreated()), "$.id");
        mvc.perform(autenticado(post("/api/encaminhamentos/{id}/consultas", encaminhamentoId).header("Idempotency-Key", chave)
                        .content(corpo), paciente))
                .andExpect(jsonPath("$.id").value(consultaId));

        mvc.perform(autenticado(get("/api/consultas/agenda").param("inicio", amanha.toString()).param("fim", amanha.toString()), especialista))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].consulta.id").value(consultaId))
                .andExpect(jsonPath("$[0].nomePaciente").value("Paciente da API"));
        mvc.perform(autenticado(post("/api/consultas/{id}/cancelar", consultaId).content("{}"), paciente))
                .andExpect(status().isBadRequest());
        mvc.perform(autenticado(post("/api/consultas/{id}/cancelar", consultaId).content("{\"versao\":0}"), paciente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));
    }

    private String codigoEnviado(String telefone) {
        Matcher m = CODIGO.matcher(caixa.para(telefone).getLast().texto());
        assertThat(m.find()).isTrue();
        return m.group(1);
    }

    private static String ativacao(String cpf, String codigo) {
        return "{\"documento\":\"%s\",\"dataNascimento\":\"1975-04-02\",\"codigo\":\"%s\",\"senha\":\"%s\"}".formatted(cpf, codigo, SENHA);
    }

    private String conta(Papel papel, UUID profissionalId) {
        String login = papel.name().toLowerCase() + "-" + UUID.randomUUID() + "@liva.teste";
        contas.criarConta(new ContaDeAcesso(UUID.randomUUID(), login, "Usuário " + papel, cifrador.cifrar(SENHA), papel,
                profissionalId, null, true, 0, null), Instant.now());
        return login;
    }

    private String entrar(String login) throws Exception {
        return entrarComo(login, SENHA);
    }

    private String entrarComo(String login, String senha) throws Exception {
        return ler(mvc.perform(json(post("/api/auth/entrar"), "{\"login\":\"%s\",\"senha\":\"%s\"}".formatted(login, senha)))
                .andExpect(status().isOk()), "$.token");
    }

    private static MockHttpServletRequestBuilder autenticado(MockHttpServletRequestBuilder requisicao, String token) {
        return requisicao.header(HttpHeaders.AUTHORIZATION, "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder requisicao, String corpo) {
        return requisicao.contentType(MediaType.APPLICATION_JSON).content(corpo);
    }

    private static String ler(ResultActions resultado, String caminho) throws Exception {
        MvcResult r = resultado.andReturn();
        return String.valueOf((Object) JsonPath.read(r.getResponse().getContentAsString(), caminho));
    }

    private Profissional profissional(TipoProfissional tipo, String especialidade) {
        Profissional p = new Profissional(UUID.randomUUID(), "Profissional " + tipo, "CRM-" + UUID.randomUUID().toString().substring(0, 12),
                tipo, especialidade, "Policlínica", "Rua A, 1", true);
        profissionais.salvarProfissional(p);
        return p;
    }

    private static String pacienteJson(String cpf, String telefone) {
        return """
                {"nomeCompleto":"Paciente da API","cpf":"%s","dataNascimento":"1975-04-02","sexo":"MASCULINO",
                 "telefone":"%s","consentimento":true}""".formatted(cpf, telefone);
    }

    private static String telefoneAleatorio() {
        return "119" + ThreadLocalRandom.current().nextInt(10_000_000, 99_999_999);
    }

    private static String cpfValido() {
        int[] d = new int[11];
        for (int i = 0; i < 9; i++) d[i] = ThreadLocalRandom.current().nextInt(10);
        d[9] = digito(d, 9);
        d[10] = digito(d, 10);
        StringBuilder cpf = new StringBuilder();
        for (int n : d) cpf.append(n);
        return cpf.chars().distinct().count() == 1 ? cpfValido() : cpf.toString();
    }

    private static int digito(int[] d, int posicao) {
        int soma = 0;
        for (int i = 0; i < posicao; i++) soma += d[i] * (posicao + 1 - i);
        int resto = soma * 10 % 11;
        return resto == 10 ? 0 : resto;
    }
}
