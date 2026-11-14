package sus.liva.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sus.liva.application.port.in.AgendaDeConsultasUseCase;
import sus.liva.application.port.in.AgendarAtendimentoUseCase;
import sus.liva.application.port.in.AtendimentoEspecializadoUseCase;
import sus.liva.application.port.in.AutenticacaoUseCase;
import sus.liva.application.port.in.CadastroDePacienteUseCase;
import sus.liva.application.port.in.CatalogoUseCase;
import sus.liva.application.port.in.ConsultarAgendamentoUseCase;
import sus.liva.application.port.in.ConsultarHistoricoUseCase;
import sus.liva.application.port.in.DispararLembreteUseCase;
import sus.liva.application.port.in.DispararLembretesDoDiaUseCase;
import sus.liva.application.port.in.EncaminhamentoUseCase;
import sus.liva.application.port.in.EnviarMensagemUseCase;
import sus.liva.application.port.in.HistoricoDoPacienteUseCase;
import sus.liva.application.port.in.ListarAgendamentosUseCase;
import sus.liva.application.port.in.ProcessarRespostaUseCase;
import sus.liva.application.port.in.TriagemUseCase;
import sus.liva.application.port.out.AgendamentoRepositoryPort;
import sus.liva.application.port.out.AuditoriaPort;
import sus.liva.application.port.out.CifradorPort;
import sus.liva.application.port.out.CodigoDoPortalPort;
import sus.liva.application.port.out.ContaDeAcessoPort;
import sus.liva.application.port.out.EmissorDeTokenPort;
import sus.liva.application.port.out.EncaminhamentoRepositoryPort;
import sus.liva.application.port.out.EnviadorMensagemPort;
import sus.liva.application.port.out.EvolucaoClinicaPort;
import sus.liva.application.port.out.MensagemRecebidaPort;
import sus.liva.application.port.out.PacienteRepositoryPort;
import sus.liva.application.port.out.ProfissionalRepositoryPort;
import sus.liva.application.port.out.RegistroMensagemPort;
import sus.liva.application.port.out.TransacaoPort;
import sus.liva.application.port.out.TriagemRepositoryPort;
import sus.liva.application.usecase.AgendaDeConsultasUseCaseImpl;
import sus.liva.application.usecase.AgendarAtendimentoUseCaseImpl;
import sus.liva.application.usecase.AtendimentoEspecializadoUseCaseImpl;
import sus.liva.application.usecase.AutenticacaoUseCaseImpl;
import sus.liva.application.usecase.CadastroDePacienteUseCaseImpl;
import sus.liva.application.usecase.CatalogoUseCaseImpl;
import sus.liva.application.usecase.ConsultarAgendamentoUseCaseImpl;
import sus.liva.application.usecase.ConsultarHistoricoUseCaseImpl;
import sus.liva.application.usecase.DispararLembreteUseCaseImpl;
import sus.liva.application.usecase.DispararLembretesDoDiaUseCaseImpl;
import sus.liva.application.usecase.EncaminhamentoUseCaseImpl;
import sus.liva.application.usecase.EnviarMensagemUseCaseImpl;
import sus.liva.application.usecase.HistoricoDoPacienteUseCaseImpl;
import sus.liva.application.usecase.ListarAgendamentosUseCaseImpl;
import sus.liva.application.usecase.ModeloMensagens;
import sus.liva.application.usecase.ProcessarRespostaUseCaseImpl;
import sus.liva.application.usecase.TriagemUseCaseImpl;

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
                                                             EncaminhamentoRepositoryPort encaminhamentos,
                                                             PacienteRepositoryPort pacientes,
                                                             EnviadorMensagemPort enviador,
                                                             ModeloMensagens modelos,
                                                             TransacaoPort transacao,
                                                             Clock clock) {
        return new ProcessarRespostaUseCaseImpl(registros, recebidas, agendamentos, encaminhamentos, pacientes, enviador,
                modelos, transacao, clock);
    }

    @Bean
    public CadastroDePacienteUseCase cadastroDePacienteUseCase(PacienteRepositoryPort pacientes, AuditoriaPort auditoria,
                                                               Clock clock) {
        return new CadastroDePacienteUseCaseImpl(pacientes, auditoria, clock);
    }

    @Bean
    public TriagemUseCase triagemUseCase(TriagemRepositoryPort triagens, EncaminhamentoRepositoryPort encaminhamentos,
                                         AgendamentoRepositoryPort agendamentos, EvolucaoClinicaPort evolucoes,
                                         PacienteRepositoryPort pacientes, ProfissionalRepositoryPort profissionais,
                                         TransacaoPort transacao, AuditoriaPort auditoria, Clock clock) {
        return new TriagemUseCaseImpl(triagens, encaminhamentos, agendamentos, evolucoes, pacientes, profissionais,
                transacao, auditoria, clock);
    }

    @Bean
    public EncaminhamentoUseCase encaminhamentoUseCase(EncaminhamentoRepositoryPort encaminhamentos,
                                                       AgendamentoRepositoryPort agendamentos,
                                                       PacienteRepositoryPort pacientes,
                                                       ProfissionalRepositoryPort profissionais,
                                                       TransacaoPort transacao, AuditoriaPort auditoria, Clock clock) {
        return new EncaminhamentoUseCaseImpl(encaminhamentos, agendamentos, pacientes, profissionais, transacao, auditoria, clock);
    }

    @Bean
    public AgendaDeConsultasUseCase agendaDeConsultasUseCase(EncaminhamentoRepositoryPort encaminhamentos,
                                                             TriagemRepositoryPort triagens,
                                                             AgendamentoRepositoryPort agendamentos,
                                                             ProfissionalRepositoryPort profissionais,
                                                             DispararLembreteUseCase dispararLembrete,
                                                             TransacaoPort transacao, AuditoriaPort auditoria, Clock clock) {
        return new AgendaDeConsultasUseCaseImpl(encaminhamentos, triagens, agendamentos, profissionais, dispararLembrete,
                transacao, auditoria, clock);
    }

    @Bean
    public AtendimentoEspecializadoUseCase atendimentoEspecializadoUseCase(AgendamentoRepositoryPort agendamentos,
                                                                           EncaminhamentoRepositoryPort encaminhamentos,
                                                                           EvolucaoClinicaPort evolucoes,
                                                                           PacienteRepositoryPort pacientes,
                                                                           TransacaoPort transacao,
                                                                           AuditoriaPort auditoria, Clock clock) {
        return new AtendimentoEspecializadoUseCaseImpl(agendamentos, encaminhamentos, evolucoes, pacientes, transacao,
                auditoria, clock);
    }

    @Bean
    public HistoricoDoPacienteUseCase historicoDoPacienteUseCase(PacienteRepositoryPort pacientes,
                                                                 TriagemRepositoryPort triagens,
                                                                 EncaminhamentoRepositoryPort encaminhamentos,
                                                                 AgendamentoRepositoryPort agendamentos,
                                                                 ProfissionalRepositoryPort profissionais,
                                                                 AuditoriaPort auditoria, Clock clock) {
        return new HistoricoDoPacienteUseCaseImpl(pacientes, triagens, encaminhamentos, agendamentos, profissionais,
                auditoria, clock);
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

    @Bean
    public AutenticacaoUseCase autenticacaoUseCase(ContaDeAcessoPort contas, CodigoDoPortalPort codigos,
                                                   PacienteRepositoryPort pacientes, TriagemRepositoryPort triagens,
                                                   CifradorPort cifrador, EmissorDeTokenPort emissor,
                                                   EnviadorMensagemPort enviador, TransacaoPort transacao,
                                                   AuditoriaPort auditoria, Clock clock) {
        return new AutenticacaoUseCaseImpl(contas, codigos, pacientes, triagens, cifrador, emissor, enviador, transacao,
                auditoria, clock);
    }

    @Bean
    public CatalogoUseCase catalogoUseCase(ProfissionalRepositoryPort profissionais) {
        return new CatalogoUseCaseImpl(profissionais);
    }
}
