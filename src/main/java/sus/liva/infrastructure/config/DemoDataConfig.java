package sus.liva.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sus.liva.application.port.out.AgendamentoRepositoryPort;
import sus.liva.application.port.out.PacienteRepositoryPort;
import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.agendamento.StatusAgendamento;
import sus.liva.domain.model.agendamento.TipoAtendimento;
import sus.liva.domain.model.paciente.Endereco;
import sus.liva.domain.model.paciente.Paciente;
import sus.liva.domain.model.paciente.Sexo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Configuration
public class DemoDataConfig {
    private static final Logger log = LoggerFactory.getLogger(DemoDataConfig.class);
    private static final UUID PACIENTE_ID = UUID.fromString("a1b2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d");
    private static final UUID AGENDAMENTO_ID = UUID.fromString("3f2b8c1e-7a4d-4c9b-9e21-5d6f0a8b1c02");

    @Bean
    public ApplicationRunner carregarDadosDemo(PacienteRepositoryPort pacientes,
                                               AgendamentoRepositoryPort agendamentos,
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
                    LocalDateTime.of(LocalDate.now().plusDays(2), LocalTime.of(9, 30)),
                    PACIENTE_ID,
                    AGENDAMENTO_ID
            ));
            log.info("Agendamento demo criado: {}", AGENDAMENTO_ID);
        };
    }
}
