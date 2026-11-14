package sus.liva.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sus.liva.application.port.in.AgendarAtendimentoUseCase;
import sus.liva.application.port.in.ConsultarAgendamentoUseCase;
import sus.liva.application.port.in.DispararLembreteUseCase;
import sus.liva.application.port.in.DispararLembretesDoDiaUseCase;
import sus.liva.application.port.in.EnviarMensagemUseCase;
import sus.liva.application.port.in.ProcessarRespostaUseCase;
import sus.liva.application.port.out.AgendamentoRepositoryPort;
import sus.liva.application.port.out.EnviadorMensagemPort;
import sus.liva.application.port.out.PacienteRepositoryPort;
import sus.liva.application.port.out.RegistroMensagemPort;
import sus.liva.application.port.out.TransacaoPort;
import sus.liva.application.usecase.AgendarAtendimentoUseCaseImpl;
import sus.liva.application.usecase.ConsultarAgendamentoUseCaseImpl;
import sus.liva.application.usecase.DispararLembreteUseCaseImpl;
import sus.liva.application.usecase.DispararLembretesDoDiaUseCaseImpl;
import sus.liva.application.usecase.EnviarMensagemUseCaseImpl;
import sus.liva.application.usecase.ModeloMensagens;
import sus.liva.application.usecase.ProcessarRespostaUseCaseImpl;

import java.time.Clock;

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
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public AgendarAtendimentoUseCase agendarAtendimentoUseCase(PacienteRepositoryPort pacientes,
                                                               AgendamentoRepositoryPort agendamentos,
                                                               TransacaoPort transacao,
                                                               Clock clock) {
        return new AgendarAtendimentoUseCaseImpl(pacientes, agendamentos, transacao, clock);
    }

    @Bean
    public ConsultarAgendamentoUseCase consultarAgendamentoUseCase(AgendamentoRepositoryPort agendamentos) {
        return new ConsultarAgendamentoUseCaseImpl(agendamentos);
    }

    @Bean
    public ProcessarRespostaUseCase processarRespostaUseCase(RegistroMensagemPort registros,
                                                             AgendamentoRepositoryPort agendamentos,
                                                             PacienteRepositoryPort pacientes,
                                                             EnviadorMensagemPort enviador,
                                                             ModeloMensagens modelos,
                                                             TransacaoPort transacao) {
        return new ProcessarRespostaUseCaseImpl(registros, agendamentos, pacientes, enviador, modelos, transacao);
    }

    @Bean
    public EnviarMensagemUseCase enviarMensagemUseCase(EnviadorMensagemPort enviador) {
        return new EnviarMensagemUseCaseImpl(enviador);
    }

    @Bean
    public DispararLembretesDoDiaUseCase dispararLembretesDoDiaUseCase(AgendamentoRepositoryPort agendamentos,
                                                                       DispararLembreteUseCase dispararLembrete) {
        return new DispararLembretesDoDiaUseCaseImpl(agendamentos, dispararLembrete);
    }
}
