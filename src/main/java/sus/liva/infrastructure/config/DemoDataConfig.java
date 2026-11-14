package sus.liva.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sus.liva.application.port.in.Papel;
import sus.liva.application.port.out.AgendamentoRepositoryPort;
import sus.liva.application.port.out.CifradorPort;
import sus.liva.application.port.out.ContaDeAcesso;
import sus.liva.application.port.out.ContaDeAcessoPort;
import sus.liva.application.port.out.PacienteRepositoryPort;
import sus.liva.application.port.out.ProfissionalRepositoryPort;
import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.agendamento.StatusAgendamento;
import sus.liva.domain.model.agendamento.TipoAtendimento;
import sus.liva.domain.model.paciente.Endereco;
import sus.liva.domain.model.paciente.Paciente;
import sus.liva.domain.model.paciente.Sexo;
import sus.liva.domain.model.profissional.Disponibilidade;
import sus.liva.domain.model.profissional.Profissional;
import sus.liva.domain.model.profissional.TipoProfissional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Configuration
public class DemoDataConfig {
    private static final Logger log = LoggerFactory.getLogger(DemoDataConfig.class);
    private static final UUID PACIENTE_ID = UUID.fromString("a1b2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d");
    private static final UUID AGENDAMENTO_ID = UUID.fromString("3f2b8c1e-7a4d-4c9b-9e21-5d6f0a8b1c02");
    private static final String UNIDADE = "Policlínica Central";
    private static final String ENDERECO = "Av. Brasil, 500 - Centro";
    private static final Profissional CLINICO = new Profissional(UUID.fromString("0c1d2e3f-0000-4000-8000-000000000001"),
            "Dr. Paulo Mendes", "CRM-SP 100001", TipoProfissional.CLINICO_GERAL, null, UNIDADE, ENDERECO, true);
    private static final Profissional ORTOPEDISTA = new Profissional(UUID.fromString("0c1d2e3f-0000-4000-8000-000000000002"),
            "Dra. Ana Ribeiro", "CRM-SP 100002", TipoProfissional.ESPECIALISTA, "ORTOPEDIA", UNIDADE, ENDERECO, true);
    private static final Profissional CARDIOLOGISTA = new Profissional(UUID.fromString("0c1d2e3f-0000-4000-8000-000000000003"),
            "Dr. Carlos Lima", "CRM-SP 100003", TipoProfissional.ESPECIALISTA, "CARDIOLOGIA", UNIDADE, ENDERECO, true);
    private static final Profissional PLANTONISTA = new Profissional(UUID.fromString("0c1d2e3f-0000-4000-8000-000000000004"),
            "Dra. Júlia Costa", "CRM-SP 100004", TipoProfissional.ESPECIALISTA, "PRONTO_ATENDIMENTO", UNIDADE, ENDERECO, true);
    private static final List<DayOfWeek> DIAS_UTEIS = List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);

    private record Acesso(UUID id, String login, String nome, Papel papel, UUID profissionalId) {}

    private static final List<Acesso> ACESSOS = List.of(
            new Acesso(UUID.fromString("0a0b0c0d-0000-4000-8000-000000000001"), "admin@liva.demo", "Administração", Papel.ADMINISTRADOR, null),
            new Acesso(UUID.fromString("0a0b0c0d-0000-4000-8000-000000000002"), "recepcao@liva.demo", "Recepção", Papel.RECEPCAO, null),
            new Acesso(UUID.fromString("0a0b0c0d-0000-4000-8000-000000000003"), "clinico@liva.demo", CLINICO.nome(), Papel.CLINICO_GERAL, CLINICO.id()),
            new Acesso(UUID.fromString("0a0b0c0d-0000-4000-8000-000000000004"), "ortopedia@liva.demo", ORTOPEDISTA.nome(), Papel.ESPECIALISTA, ORTOPEDISTA.id()),
            new Acesso(UUID.fromString("0a0b0c0d-0000-4000-8000-000000000005"), "cardiologia@liva.demo", CARDIOLOGISTA.nome(), Papel.ESPECIALISTA, CARDIOLOGISTA.id()),
            new Acesso(UUID.fromString("0a0b0c0d-0000-4000-8000-000000000006"), "plantao@liva.demo", PLANTONISTA.nome(), Papel.ESPECIALISTA, PLANTONISTA.id()));

    @Bean
    public ApplicationRunner carregarEquipeDemo(ProfissionalRepositoryPort profissionais, ContaDeAcessoPort contas,
                                                CifradorPort cifrador, Clock clock,
                                                @Value("${confirmasus.demo.senha:}") String senha) {
        return args -> {
            for (Profissional p : List.of(CLINICO, ORTOPEDISTA, CARDIOLOGISTA, PLANTONISTA)) {
                if (profissionais.buscarProfissional(p.id()).isPresent()) continue;
                profissionais.salvarProfissional(p);
                if (p.tipo() == TipoProfissional.ESPECIALISTA && !p.especialidade().equals(PLANTONISTA.especialidade()))
                    agendaPadrao(profissionais, p);
            }
            if (senha == null || senha.isBlank()) {
                log.warn("DEMO_SENHA não definida: usuários de demonstração não foram criados");
                return;
            }
            String hash = cifrador.cifrar(senha);
            for (Acesso a : ACESSOS)
                if (contas.buscarPorLogin(a.login()).isEmpty())
                    contas.criarConta(new ContaDeAcesso(a.id(), a.login(), a.nome(), hash, a.papel(), a.profissionalId(),
                            null, true, 0, null), Instant.now(clock));
            log.info("Usuários de demonstração disponíveis: {}", ACESSOS.stream().map(Acesso::login).toList());
        };
    }

    private static void agendaPadrao(ProfissionalRepositoryPort profissionais, Profissional p) {
        for (DayOfWeek dia : DIAS_UTEIS) {
            profissionais.salvarDisponibilidade(new Disponibilidade(UUID.randomUUID(), p.id(), dia, LocalTime.of(8, 0),
                    LocalTime.of(12, 0), 30));
            profissionais.salvarDisponibilidade(new Disponibilidade(UUID.randomUUID(), p.id(), dia, LocalTime.of(13, 30),
                    LocalTime.of(17, 0), 30));
        }
    }

    @Bean
    public ApplicationRunner carregarDadosDemo(PacienteRepositoryPort pacientes,
                                               AgendamentoRepositoryPort agendamentos,
                                               Clock clock,
                                               @Value("${confirmasus.demo.telefone}") String telefone) {
        return args -> {
            if (agendamentos.buscarAgendamento(AGENDAMENTO_ID).isPresent()) {
                log.info("Agendamento demo já existe: {}", AGENDAMENTO_ID);
                return;
            }
            pacientes.salvarPaciente(new Paciente(
                    PACIENTE_ID,
                    "Maria da Silva",
                    null,
                    "12345678909",
                    "123456789012345",
                    LocalDate.of(1958, 3, 12),
                    Sexo.FEMININO,
                    "maria@exemplo.com",
                    Telefone.de(telefone),
                    new Endereco("01001000", "Praça da Sé", "100", null, "Sé", "São Paulo", "SP"),
                    null,
                    null,
                    null,
                    null,
                    null,
                    true
            ));
            agendamentos.salvarAgendamento(new Agendamento(
                    StatusAgendamento.AGENDADO,
                    "Jejum de 8 horas",
                    "Rua das Flores, 123 - Centro",
                    "UBS Centro",
                    TipoAtendimento.CONSULTA,
                    LocalDateTime.of(LocalDate.now(clock).plusDays(2), LocalTime.of(9, 30)),
                    PACIENTE_ID,
                    AGENDAMENTO_ID
            ));
            log.info("Agendamento demo criado: {}", AGENDAMENTO_ID);
        };
    }
}
