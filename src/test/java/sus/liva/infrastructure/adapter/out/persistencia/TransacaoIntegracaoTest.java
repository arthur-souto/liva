package sus.liva.infrastructure.adapter.out.persistencia;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import sus.liva.TestcontainersConfig;
import sus.liva.application.port.in.AgendarAtendimentoUseCase;
import sus.liva.application.port.in.ConsultarAgendamentoUseCase;
import sus.liva.application.port.in.DispararLembreteUseCase;
import sus.liva.application.port.in.NovoAtendimento;
import sus.liva.application.port.in.ProcessarRespostaUseCase;
import sus.liva.application.port.in.ResultadoResposta;
import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.agendamento.StatusAgendamento;
import sus.liva.domain.model.agendamento.TipoAtendimento;
import sus.liva.domain.model.agendamento.TipoLembrete;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "confirmasus.whatsapp.provedor=console")
@Import(TestcontainersConfig.class)
class TransacaoIntegracaoTest {

    @Autowired
    private AgendarAtendimentoUseCase agendarAtendimento;
    @Autowired
    private DispararLembreteUseCase dispararLembrete;
    @Autowired
    private ProcessarRespostaUseCase processarResposta;
    @Autowired
    private ConsultarAgendamentoUseCase consultarAgendamento;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void desfazPacienteQuandoGravacaoDoAgendamentoFalha() {
        String telefone = telefoneAleatorio();
        int pacientesAntes = contar("pacientes");

        assertThatThrownBy(() -> agendarAtendimento.agendar(novo(telefone, "U".repeat(200))))
                .isNotInstanceOf(IllegalArgumentException.class);

        assertThat(contar("pacientes")).isEqualTo(pacientesAntes);
    }

    @Test
    void gravaPacienteEAgendamentoJuntos() {
        int pacientesAntes = contar("pacientes");
        int agendamentosAntes = contar("agendamentos");

        agendarAtendimento.agendar(novo(telefoneAleatorio(), "UBS Centro"));

        assertThat(contar("pacientes")).isEqualTo(pacientesAntes + 1);
        assertThat(contar("agendamentos")).isEqualTo(agendamentosAntes + 1);
    }

    @Test
    void aplicaSomenteUmaDeDuasRespostasSimultaneas() throws Exception {
        String telefone = telefoneAleatorio();
        Agendamento agendamento = agendarAtendimento.agendar(novo(telefone, "UBS Centro"));
        dispararLembrete.disparar(agendamento.getId(), TipoLembrete.PEDIDO_CONFIRMACAO, true);

        CountDownLatch largada = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<ResultadoResposta>> respostas = List.of("1", "2").stream()
                    .map(texto -> executor.submit(() -> {
                        largada.await();
                        return processarResposta.processar(Telefone.de(telefone), texto);
                    }))
                    .toList();
            largada.countDown();

            List<ResultadoResposta> resultados = respostas.stream().map(f -> {
                try {
                    return f.get();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }).toList();

            assertThat(resultados).filteredOn(ResultadoResposta::aplicada).hasSize(1);
            ResultadoResposta aplicada = resultados.stream().filter(ResultadoResposta::aplicada).findFirst().orElseThrow();
            assertThat(consultarAgendamento.consultar(agendamento.getId()).orElseThrow().getStatus())
                    .isEqualTo(aplicada.status())
                    .isIn(StatusAgendamento.CONFIRMADO, StatusAgendamento.CANCELADO);
        }
    }

    private int contar(String tabela) {
        return jdbc.queryForObject("select count(*) from " + tabela, Integer.class);
    }

    private static NovoAtendimento novo(String telefone, String unidade) {
        return new NovoAtendimento("Paciente Integração", telefone, null, null, LocalDateTime.now().plusDays(2),
                TipoAtendimento.CONSULTA, unidade, "Rua A, 1", null);
    }

    private static String telefoneAleatorio() {
        return "119" + ThreadLocalRandom.current().nextInt(10_000_000, 99_999_999);
    }
}
