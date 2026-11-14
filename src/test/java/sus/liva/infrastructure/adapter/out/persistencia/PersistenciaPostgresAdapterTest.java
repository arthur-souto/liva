package sus.liva.infrastructure.adapter.out.persistencia;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import sus.liva.TestcontainersConfig;
import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.agendamento.StatusAgendamento;
import sus.liva.domain.model.agendamento.TipoAtendimento;
import sus.liva.domain.model.agendamento.TipoLembrete;
import sus.liva.domain.model.mensagem.RegistroMensagem;
import sus.liva.domain.model.paciente.Endereco;
import sus.liva.domain.model.paciente.Paciente;
import sus.liva.domain.model.paciente.Sexo;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfig.class, PacientePostgresAdapter.class, AgendamentoPostgresAdapter.class,
        RegistroMensagemPostgresAdapter.class})
class PersistenciaPostgresAdapterTest {

    private static final LocalDate DIA = LocalDate.of(2026, 11, 15);

    @Autowired
    private PacientePostgresAdapter pacientes;
    @Autowired
    private AgendamentoPostgresAdapter agendamentos;
    @Autowired
    private RegistroMensagemPostgresAdapter registros;

    private Paciente paciente;

    @BeforeEach
    void setUp() {
        paciente = new Paciente(UUID.randomUUID(), "Maria da Silva", "Maria", "12345678909", "123456789012345",
                LocalDate.of(1958, 3, 12), Sexo.FEMININO, "maria@exemplo.com", Telefone.de("51993953661"),
                new Endereco("01001000", "Praça da Sé", "100", "Apto 1", "Sé", "São Paulo", "SP"),
                "João", Telefone.de("11987654321"), "Ana", Telefone.de("1133334444"), "Alergia a dipirona", true);
        pacientes.salvarPaciente(paciente);
    }

    @Test
    void salvaEBuscaPacienteCompleto() {
        assertThat(pacientes.buscarPaciente(paciente.id())).contains(paciente);
    }

    @Test
    void salvaPacienteSemEnderecoNemCuidador() {
        Paciente simples = new Paciente(UUID.randomUUID(), "José", null, null, null, null, Sexo.NAO_INFORMADO,
                null, Telefone.de("11999998888"), null, null, null, null, null, null, false);
        pacientes.salvarPaciente(simples);

        Paciente lido = pacientes.buscarPaciente(simples.id()).orElseThrow();
        assertThat(lido).isEqualTo(simples);
        assertThat(lido.endereco()).isNull();
        assertThat(lido.temCuidador()).isFalse();
    }

    @Test
    void retornaVazioParaPacienteInexistente() {
        assertThat(pacientes.buscarPaciente(UUID.randomUUID())).isEmpty();
    }

    @Test
    void salvaEAtualizaStatusDoAgendamento() {
        Agendamento a = agendamento(DIA.atTime(9, 30));
        agendamentos.salvarAgendamento(a);

        a.aguardarResposta();
        agendamentos.salvarAgendamento(a);

        Agendamento lido = agendamentos.buscarAgendamento(a.getId()).orElseThrow();
        assertThat(lido.getStatus()).isEqualTo(StatusAgendamento.AGUARDANDO_RESPOSTA);
        assertThat(lido.getPacienteId()).isEqualTo(paciente.id());
        assertThat(lido.getDataHora()).isEqualTo(DIA.atTime(9, 30));
        assertThat(lido.getPreparo()).isEqualTo("Jejum de 8 horas");
    }

    @Test
    void listaAgendamentosDoDiaRespeitandoLimites() {
        Agendamento inicioDoDia = agendamento(DIA.atStartOfDay());
        Agendamento fimDoDia = agendamento(DIA.atTime(23, 59, 59));
        agendamentos.salvarAgendamento(agendamento(DIA.minusDays(1).atTime(23, 59, 59)));
        agendamentos.salvarAgendamento(fimDoDia);
        agendamentos.salvarAgendamento(inicioDoDia);
        agendamentos.salvarAgendamento(agendamento(DIA.plusDays(1).atStartOfDay()));

        assertThat(agendamentos.agendamentosDoDia(DIA))
                .extracting(Agendamento::getId)
                .containsExactly(inicioDoDia.getId(), fimDoDia.getId());
    }

    @Test
    void rejeitaAgendamentoDePacienteInexistente() {
        Agendamento orfao = new Agendamento(StatusAgendamento.AGENDADO, null, "Rua A", "UBS", TipoAtendimento.CONSULTA,
                DIA.atTime(9, 0), UUID.randomUUID(), UUID.randomUUID());

        assertThatThrownBy(() -> {
            agendamentos.salvarAgendamento(orfao);
            agendamentos.agendamentosDoDia(DIA);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void indicaSeLembreteJaFoiEnviadoComSucesso() {
        Agendamento a = agendamento(DIA.atTime(9, 30));
        agendamentos.salvarAgendamento(a);
        registros.salvarRegistro(registro(a, "51993953661", TipoLembrete.PEDIDO_CONFIRMACAO, false, Instant.now()));

        assertThat(registros.jaEnviado(a.getId(), TipoLembrete.PEDIDO_CONFIRMACAO)).isFalse();

        registros.salvarRegistro(registro(a, "51993953661", TipoLembrete.PEDIDO_CONFIRMACAO, true, Instant.now()));

        assertThat(registros.jaEnviado(a.getId(), TipoLembrete.PEDIDO_CONFIRMACAO)).isTrue();
        assertThat(registros.jaEnviado(a.getId(), TipoLembrete.LEMBRETE_VESPERA)).isFalse();
    }

    @Test
    void encontraUltimoEnvioPeloTelefoneMesmoSemNonoDigito() {
        Agendamento antigo = agendamento(DIA.atTime(8, 0));
        Agendamento recente = agendamento(DIA.atTime(10, 0));
        agendamentos.salvarAgendamento(antigo);
        agendamentos.salvarAgendamento(recente);
        Instant agora = Instant.now().truncatedTo(ChronoUnit.MICROS);
        registros.salvarRegistro(registro(antigo, "51993953661", TipoLembrete.PEDIDO_CONFIRMACAO, true, agora.minusSeconds(60)));
        registros.salvarRegistro(registro(recente, "51993953661", TipoLembrete.PEDIDO_CONFIRMACAO, true, agora));
        registros.salvarRegistro(registro(recente, "51993953661", TipoLembrete.LEMBRETE_VESPERA, true, agora.plusSeconds(60)));

        RegistroMensagem ultimo = registros.ultimoEnvio(Telefone.de("555193953661"), TipoLembrete.PEDIDO_CONFIRMACAO).orElseThrow();

        assertThat(ultimo.agendamentoId()).isEqualTo(recente.getId());
        assertThat(ultimo.destino()).isEqualTo(Telefone.de("51993953661"));
        assertThat(ultimo.enviadoEm()).isEqualTo(agora);
        assertThat(registros.ultimoEnvio(Telefone.de("11987654321"), TipoLembrete.PEDIDO_CONFIRMACAO)).isEmpty();
    }

    @Test
    void salvaRegistroSemAgendamentoComTextoLongo() {
        registros.salvarRegistro(new RegistroMensagem(null, Telefone.de("51993953661"), null, "x".repeat(5000),
                false, null, "e".repeat(5000), Instant.now()));

        assertThat(registros.ultimoEnvio(Telefone.de("51993953661"), TipoLembrete.PEDIDO_CONFIRMACAO)).isEmpty();
    }

    private Agendamento agendamento(LocalDateTime dataHora) {
        return new Agendamento(StatusAgendamento.AGENDADO, "Jejum de 8 horas", "Rua das Flores, 123", "UBS Centro",
                TipoAtendimento.CONSULTA, dataHora, paciente.id(), UUID.randomUUID());
    }

    private static RegistroMensagem registro(Agendamento a, String telefone, TipoLembrete tipo, boolean sucesso, Instant enviadoEm) {
        return new RegistroMensagem(a.getId(), Telefone.de(telefone), tipo, "texto", sucesso,
                sucesso ? "id-" + UUID.randomUUID() : null, sucesso ? null : "erro", enviadoEm);
    }
}
