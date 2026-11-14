package sus.liva.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sus.liva.application.port.in.AgendarAtendimentoUseCase;
import sus.liva.application.port.in.ConsultarAgendamentoUseCase;
import sus.liva.application.port.in.ConsultarHistoricoUseCase;
import sus.liva.application.port.in.DispararLembreteUseCase;
import sus.liva.application.port.in.DispararLembretesDoDiaUseCase;
import sus.liva.application.port.in.EnviarMensagemUseCase;
import sus.liva.application.port.in.ListarAgendamentosUseCase;
import sus.liva.application.port.in.ProcessarRespostaUseCase;
import sus.liva.application.port.out.AgendamentoRepositoryPort;
import sus.liva.application.port.out.EnviadorMensagemPort;
import sus.liva.application.port.out.MensagemRecebidaPort;
import sus.liva.application.port.out.PacienteRepositoryPort;
import sus.liva.application.port.out.RegistroMensagemPort;
import sus.liva.application.port.out.TransacaoPort;
import sus.liva.application.usecase.AgendarAtendimentoUseCaseImpl;
import sus.liva.application.usecase.ConsultarAgendamentoUseCaseImpl;
import sus.liva.application.usecase.ConsultarHistoricoUseCaseImpl;
import sus.liva.application.usecase.DispararLembreteUseCaseImpl;
import sus.liva.application.usecase.DispararLembretesDoDiaUseCaseImpl;
import sus.liva.application.usecase.EnviarMensagemUseCaseImpl;
import sus.liva.application.usecase.ListarAgendamentosUseCaseImpl;
import sus.liva.application.usecase.ModeloMensagens;
import sus.liva.application.usecase.ProcessarRespostaUseCaseImpl;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class UseCaseConfig {

    @Bean
    public ModeloMensagens modeloMensagens() {
        return new ModeloMensagens();
    }

    @Bean
    public DispararLembreteUseCase dispararLembreteUseCase(AgendamentoRepositoryPort agendamentos,
                                                           PacienteRepositoryPort pacientes,
                                                           RegistroMensagemPort registros,
                                                           EnviadorMensagemPort enviador,
                                                           ModeloMensagens modelos) {
        return new DispararLembreteUseCaseImpl(agendamentos, pacientes, registros, enviador, modelos);
    }

    @Bean
    public Clock clock(@Value("${confirmasus.fuso}") String fuso) {
        return Clock.system(ZoneId.of(fuso));
    }

    @Bean
    public AgendarAtendimentoUseCase agendarAtendimentoUseCase(PacienteRepositoryPort pacientes,
                                                               AgendamentoRepositoryPort agendamentos,
                                                               DispararLembreteUseCase dispararLembrete,
                                                               TransacaoPort transacao,
                                                               Clock clock) {
        return new AgendarAtendimentoUseCaseImpl(pacientes, agendamentos, dispararLembrete, transacao, clock);
    }

    @Bean
    public ConsultarAgendamentoUseCase consultarAgendamentoUseCase(AgendamentoRepositoryPort agendamentos) {
        return new ConsultarAgendamentoUseCaseImpl(agendamentos);
    }

    @Bean
    public ProcessarRespostaUseCase processarRespostaUseCase(RegistroMensagemPort registros,
                                                             MensagemRecebidaPort recebidas,
                                                             AgendamentoRepositoryPort agendamentos,
                                                             PacienteRepositoryPort pacientes,
                                                             EnviadorMensagemPort enviador,
                                                             ModeloMensagens modelos,
                                                             TransacaoPort transacao,
                                                             Clock clock) {
        return new ProcessarRespostaUseCaseImpl(registros, recebidas, agendamentos, pacientes, enviador, modelos, transacao, clock);
    }

    @Bean
    public ConsultarHistoricoUseCase consultarHistoricoUseCase(AgendamentoRepositoryPort agendamentos,
                                                               RegistroMensagemPort registros,
                                                               MensagemRecebidaPort recebidas) {
        return new ConsultarHistoricoUseCaseImpl(agendamentos, registros, recebidas);
    }

    @Bean
    public EnviarMensagemUseCase enviarMensagemUseCase(EnviadorMensagemPort enviador, RegistroMensagemPort registros,
                                                       Clock clock) {
        return new EnviarMensagemUseCaseImpl(enviador, registros, clock);
    }

    @Bean
    public DispararLembretesDoDiaUseCase dispararLembretesDoDiaUseCase(AgendamentoRepositoryPort agendamentos,
                                                                       DispararLembreteUseCase dispararLembrete) {
        return new DispararLembretesDoDiaUseCaseImpl(agendamentos, dispararLembrete);
    }

    @Bean
    public ListarAgendamentosUseCase listarAgendamentosUseCase(AgendamentoRepositoryPort agendamentos,
                                                               PacienteRepositoryPort pacientes) {
        return new ListarAgendamentosUseCaseImpl(agendamentos, pacientes);
    }
}
