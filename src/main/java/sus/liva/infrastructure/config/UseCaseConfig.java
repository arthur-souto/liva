package sus.liva.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sus.liva.application.port.in.AcessoPorLinkUseCase;
import sus.liva.application.port.in.PessoaDeConfiancaUseCase;
import sus.liva.application.port.out.ConversaPort;
import sus.liva.application.port.out.CuidadorRepositoryPort;
import sus.liva.application.port.out.LinkDeAcessoPort;
import sus.liva.application.port.out.RemarcacaoPort;
import sus.liva.application.usecase.AcessoPorLinkUseCaseImpl;
import sus.liva.application.usecase.AssistenteWhatsApp;
import sus.liva.application.usecase.LembretesParaPessoasDeConfianca;
import sus.liva.application.usecase.LimiteDeRemarcacoes;
import sus.liva.application.usecase.LinksDeAcesso;
import sus.liva.application.usecase.NotificacaoAoPaciente;
import sus.liva.application.usecase.PessoaDeConfiancaUseCaseImpl;
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

import sus.liva.application.port.in.ConfirmacaoDeEntregaUseCase;
import sus.liva.application.port.in.FilaDeEsperaUseCase;
import sus.liva.application.port.out.FilaDeEsperaRepositoryPort;
import sus.liva.application.usecase.ConfirmacaoDeEntregaUseCaseImpl;
import sus.liva.application.usecase.FilaDeEsperaUseCaseImpl;
import sus.liva.application.usecase.ProcessarFilaUseCaseImpl;
import sus.liva.application.usecase.ReservaDeVaga;
import sus.liva.application.usecase.RespostaDeOfertaPeloWhatsApp;
import sus.liva.domain.model.agendamento.AntecedenciaDosLembretes;
import sus.liva.domain.model.fila.AcaoSemResposta;
import sus.liva.domain.model.fila.PoliticaDaFila;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;

@Configuration
public class UseCaseConfig {

    @Bean
    public PoliticaDaFila politicaDaFila(@Value("${confirmasus.fila.prazo-confirmacao}") Duration prazoDeConfirmacao,
                                         @Value("${confirmasus.fila.aviso-final}") Duration avisoFinal,
                                         @Value("${confirmasus.fila.sem-resposta}") AcaoSemResposta semResposta,
                                         @Value("${confirmasus.fila.exigir-entrega}") boolean exigirEntrega,
                                         @Value("${confirmasus.fila.prazo-oferta}") Duration prazoDaOferta,
                                         @Value("${confirmasus.fila.antecedencia-minima}") Duration antecedenciaMinima,
                                         @Value("${confirmasus.fila.ofertas-por-vaga}") int ofertasPorVaga,
                                         @Value("${confirmasus.fila.espera-sem-candidato}") Duration esperaSemCandidato) {
        return new PoliticaDaFila(prazoDeConfirmacao, avisoFinal, semResposta, exigirEntrega, prazoDaOferta, antecedenciaMinima,
                ofertasPorVaga, esperaSemCandidato);
    }

    @Bean
    public ModeloMensagens modeloMensagens(PoliticaDaFila politica) {
        return new ModeloMensagens(politica.prazoDeConfirmacao(), politica.semResposta());
    }

    @Bean
    public ReservaDeVaga reservaDeVaga(FilaDeEsperaRepositoryPort fila, AgendamentoRepositoryPort agendamentos,
                                       EncaminhamentoRepositoryPort encaminhamentos, ProfissionalRepositoryPort profissionais,
                                       TransacaoPort transacao, NotificacaoAoPaciente notificacao, ModeloMensagens modelos,
                                       Clock clock) {
        return new ReservaDeVaga(fila, agendamentos, encaminhamentos, profissionais, transacao, notificacao, modelos, clock);
    }

    @Bean
    public ProcessarFilaUseCaseImpl processarFilaUseCase(FilaDeEsperaRepositoryPort fila, AgendamentoRepositoryPort agendamentos,
                                                         EncaminhamentoRepositoryPort encaminhamentos,
                                                         ProfissionalRepositoryPort profissionais,
                                                         DispararLembreteUseCase dispararLembrete, NotificacaoAoPaciente notificacao,
                                                         LinksDeAcesso links, ModeloMensagens modelos, TransacaoPort transacao,
                                                         PoliticaDaFila politica, Clock clock,
                                                         @Value("${confirmasus.portal.url}") String urlDoPortal,
                                                         @Value("${confirmasus.fila.oferta-imediata}") boolean ofertaImediata) {
        return new ProcessarFilaUseCaseImpl(fila, agendamentos, encaminhamentos, profissionais, dispararLembrete, notificacao, links,
                modelos, transacao, politica, clock, urlDoPortal, ofertaImediata);
    }

    @Bean
    public FilaDeEsperaUseCase filaDeEsperaUseCase(FilaDeEsperaRepositoryPort fila, AgendamentoRepositoryPort agendamentos,
                                                   PacienteRepositoryPort pacientes, ProfissionalRepositoryPort profissionais,
                                                   ReservaDeVaga reserva, ProcessarFilaUseCaseImpl processamento, TransacaoPort transacao,
                                                   PoliticaDaFila politica, AuditoriaPort auditoria, Clock clock) {
        return new FilaDeEsperaUseCaseImpl(fila, agendamentos, pacientes, profissionais, reserva, processamento, transacao, politica,
                auditoria, clock);
    }

    @Bean
    public RespostaDeOfertaPeloWhatsApp respostaDeOfertaPeloWhatsApp(FilaDeEsperaRepositoryPort fila, AssistenteWhatsApp assistente,
                                                                     ReservaDeVaga reserva, ProcessarFilaUseCaseImpl processamento,
                                                                     NotificacaoAoPaciente notificacao, RegistroMensagemPort registros,
                                                                     ModeloMensagens modelos, Clock clock) {
        return new RespostaDeOfertaPeloWhatsApp(fila, assistente, reserva, processamento, notificacao, registros, modelos, clock);
    }

    @Bean
    public ConfirmacaoDeEntregaUseCase confirmacaoDeEntregaUseCase(RegistroMensagemPort registros, Clock clock) {
        return new ConfirmacaoDeEntregaUseCaseImpl(registros, clock);
    }

    @Bean
    public DispararLembreteUseCase dispararLembreteUseCase(AgendamentoRepositoryPort agendamentos,
                                                           PacienteRepositoryPort pacientes,
                                                           RegistroMensagemPort registros,
                                                           EnviadorMensagemPort enviador,
                                                           ModeloMensagens modelos,
                                                           CuidadorRepositoryPort cuidadores,
                                                           Clock clock) {
        return new LembretesParaPessoasDeConfianca(new DispararLembreteUseCaseImpl(agendamentos, pacientes, registros, enviador, modelos),
                agendamentos, pacientes, cuidadores, registros, enviador, modelos, clock);
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
                                                               Clock clock,
                                                               AntecedenciaDosLembretes antecedencia) {
        return new AgendarAtendimentoUseCaseImpl(pacientes, agendamentos, dispararLembrete, transacao, clock, antecedencia);
    }

    @Bean
    public AntecedenciaDosLembretes antecedenciaDosLembretes(@Value("${confirmasus.lembretes.pedido-dias-antes}") int pedido,
                                                             @Value("${confirmasus.lembretes.vespera-dias-antes}") int vespera) {
        return new AntecedenciaDosLembretes(pedido, vespera);
    }

    @Bean
    public ConsultarAgendamentoUseCase consultarAgendamentoUseCase(AgendamentoRepositoryPort agendamentos) {
        return new ConsultarAgendamentoUseCaseImpl(agendamentos);
    }

    @Bean
    public NotificacaoAoPaciente notificacaoAoPaciente(PacienteRepositoryPort pacientes, EnviadorMensagemPort enviador,
                                                       RegistroMensagemPort registros, ModeloMensagens modelos,
                                                       CuidadorRepositoryPort cuidadores, Clock clock) {
        return new NotificacaoAoPaciente(pacientes, enviador, registros, modelos, cuidadores, clock);
    }

    @Bean
    public LimiteDeRemarcacoes limiteDeRemarcacoes(RemarcacaoPort remarcacoes, Clock clock) {
        return new LimiteDeRemarcacoes(remarcacoes, clock);
    }

    @Bean
    public LinksDeAcesso linksDeAcesso(LinkDeAcessoPort links, Clock clock, @Value("${confirmasus.portal.url}") String urlDoPortal) {
        return new LinksDeAcesso(links, clock, urlDoPortal);
    }

    @Bean
    public AcessoPorLinkUseCase acessoPorLinkUseCase(LinkDeAcessoPort links, ContaDeAcessoPort contas, PacienteRepositoryPort pacientes,
                                                     EmissorDeTokenPort emissor, TransacaoPort transacao, AuditoriaPort auditoria,
                                                     Clock clock) {
        return new AcessoPorLinkUseCaseImpl(links, contas, pacientes, emissor, transacao, auditoria, clock);
    }

    @Bean
    public AssistenteWhatsApp assistenteWhatsApp(PacienteRepositoryPort pacientes, EncaminhamentoRepositoryPort encaminhamentos,
                                                 AgendamentoRepositoryPort agendamentos, ProfissionalRepositoryPort profissionais,
                                                 ContaDeAcessoPort contas, ConversaPort conversas, RegistroMensagemPort registros,
                                                 AgendaDeConsultasUseCase agenda, LinksDeAcesso links, LimiteDeRemarcacoes limite,
                                                 NotificacaoAoPaciente notificacao, ModeloMensagens modelos,
                                                 CuidadorRepositoryPort cuidadores, Clock clock) {
        return new AssistenteWhatsApp(pacientes, encaminhamentos, agendamentos, profissionais, contas, conversas, registros,
                agenda, links, limite, notificacao, modelos, cuidadores, clock);
    }

    @Bean
    public PessoaDeConfiancaUseCase pessoaDeConfiancaUseCase(CuidadorRepositoryPort cuidadores, PacienteRepositoryPort pacientes,
                                                             TriagemRepositoryPort triagens, CifradorPort cifrador,
                                                             EmissorDeTokenPort emissor, EnviadorMensagemPort enviador,
                                                             NotificacaoAoPaciente notificacao, ModeloMensagens modelos,
                                                             TransacaoPort transacao, AuditoriaPort auditoria, Clock clock,
                                                             @Value("${confirmasus.portal.url}") String urlDoPortal) {
        return new PessoaDeConfiancaUseCaseImpl(cuidadores, pacientes, triagens, cifrador, emissor, enviador, notificacao, modelos,
                transacao, auditoria, clock, urlDoPortal);
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
                                                             Clock clock,
                                                             AssistenteWhatsApp assistente,
                                                             RespostaDeOfertaPeloWhatsApp ofertas) {
        return new ProcessarRespostaUseCaseImpl(registros, recebidas, agendamentos, encaminhamentos, pacientes, enviador,
                modelos, transacao, clock, assistente, ofertas);
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
                                                             TransacaoPort transacao, AuditoriaPort auditoria,
                                                             NotificacaoAoPaciente notificacao, ModeloMensagens modelos,
                                                             LimiteDeRemarcacoes limite, Clock clock,
                                                             FilaDeEsperaRepositoryPort fila) {
        return new AgendaDeConsultasUseCaseImpl(encaminhamentos, triagens, agendamentos, profissionais, dispararLembrete,
                transacao, auditoria, notificacao, modelos, limite, clock, fila);
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
                                                                       DispararLembreteUseCase dispararLembrete,
                                                                       AntecedenciaDosLembretes antecedencia) {
        return new DispararLembretesDoDiaUseCaseImpl(agendamentos, dispararLembrete, antecedencia);
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
