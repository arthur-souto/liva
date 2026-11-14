package sus.liva.application.usecase;

import sus.liva.application.port.in.ProcessarRespostaUseCase;
import sus.liva.application.port.in.ResultadoResposta;
import sus.liva.application.port.out.AgendamentoRepositoryPort;
import sus.liva.application.port.out.EncaminhamentoRepositoryPort;
import sus.liva.application.port.out.EnviadorMensagemPort;
import sus.liva.application.port.out.MensagemRecebidaPort;
import sus.liva.application.port.out.MensagemSaida;
import sus.liva.application.port.out.PacienteRepositoryPort;
import sus.liva.application.port.out.RegistroMensagemPort;
import sus.liva.application.port.out.ResultadoEnvio;
import sus.liva.application.port.out.TransacaoPort;
import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.agendamento.RespostaPaciente;
import sus.liva.domain.model.agendamento.TipoLembrete;
import sus.liva.domain.model.mensagem.MensagemRecebida;
import sus.liva.domain.model.mensagem.RegistroMensagem;
import sus.liva.domain.model.paciente.Paciente;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class ProcessarRespostaUseCaseImpl implements ProcessarRespostaUseCase {
    private static final Duration INTERVALO_ENTRE_ORIENTACOES = Duration.ofMinutes(5);

    private final RegistroMensagemPort registros;
    private final MensagemRecebidaPort recebidas;
    private final AgendamentoRepositoryPort agendamentos;
    private final EncaminhamentoRepositoryPort encaminhamentos;
    private final PacienteRepositoryPort pacientes;
    private final EnviadorMensagemPort enviador;
    private final ModeloMensagens modelos;
    private final TransacaoPort transacao;
    private final Clock clock;
    private final AssistenteWhatsApp assistente;
    private final RespostaDeOfertaPeloWhatsApp ofertas;
    private final PerguntaDeMotivo motivos;

    public ProcessarRespostaUseCaseImpl(RegistroMensagemPort registros, MensagemRecebidaPort recebidas,
                                        AgendamentoRepositoryPort agendamentos, EncaminhamentoRepositoryPort encaminhamentos,
                                        PacienteRepositoryPort pacientes, EnviadorMensagemPort enviador,
                                        ModeloMensagens modelos, TransacaoPort transacao, Clock clock) {
        this(registros, recebidas, agendamentos, encaminhamentos, pacientes, enviador, modelos, transacao, clock, null);
    }

    public ProcessarRespostaUseCaseImpl(RegistroMensagemPort registros, MensagemRecebidaPort recebidas,
                                        AgendamentoRepositoryPort agendamentos, EncaminhamentoRepositoryPort encaminhamentos,
                                        PacienteRepositoryPort pacientes, EnviadorMensagemPort enviador,
                                        ModeloMensagens modelos, TransacaoPort transacao, Clock clock,
                                        AssistenteWhatsApp assistente) {
        this(registros, recebidas, agendamentos, encaminhamentos, pacientes, enviador, modelos, transacao, clock, assistente, null);
    }

    public ProcessarRespostaUseCaseImpl(RegistroMensagemPort registros, MensagemRecebidaPort recebidas,
                                        AgendamentoRepositoryPort agendamentos, EncaminhamentoRepositoryPort encaminhamentos,
                                        PacienteRepositoryPort pacientes, EnviadorMensagemPort enviador,
                                        ModeloMensagens modelos, TransacaoPort transacao, Clock clock,
                                        AssistenteWhatsApp assistente, RespostaDeOfertaPeloWhatsApp ofertas) {
        this(registros, recebidas, agendamentos, encaminhamentos, pacientes, enviador, modelos, transacao, clock, assistente, ofertas, null);
    }

    public ProcessarRespostaUseCaseImpl(RegistroMensagemPort registros, MensagemRecebidaPort recebidas,
                                        AgendamentoRepositoryPort agendamentos, EncaminhamentoRepositoryPort encaminhamentos,
                                        PacienteRepositoryPort pacientes, EnviadorMensagemPort enviador,
                                        ModeloMensagens modelos, TransacaoPort transacao, Clock clock,
                                        AssistenteWhatsApp assistente, RespostaDeOfertaPeloWhatsApp ofertas,
                                        PerguntaDeMotivo motivos) {
        this.registros = registros;
        this.recebidas = recebidas;
        this.agendamentos = agendamentos;
        this.encaminhamentos = encaminhamentos;
        this.pacientes = pacientes;
        this.enviador = enviador;
        this.modelos = modelos;
        this.transacao = transacao;
        this.clock = clock;
        this.assistente = assistente;
        this.ofertas = ofertas;
        this.motivos = motivos;
    }

    private record Aplicacao(ResultadoResposta ignorada, Agendamento agendamento, Telefone destino) {
        static Aplicacao ignorada(String motivo) {
            return new Aplicacao(ResultadoResposta.ignorada(motivo), null, null);
        }
    }

    @Override
    public ResultadoResposta processar(Telefone remetente, String texto, String idMensagem) {
        if (idMensagem != null && registros.enviadaPelaLiva(idMensagem))
            return ResultadoResposta.ignorada("Mensagem enviada pela própria Liva");

        Instant recebidaEm = Instant.now(clock);
        Optional<RespostaPaciente> resposta = RespostaPaciente.interpretar(texto);
        Optional<ResultadoResposta> daOferta = ofertas == null ? Optional.empty() : ofertas.responder(remetente, texto);
        ResultadoResposta resultado;
        if (daOferta.isEmpty()) daOferta = motivoDaAusencia(remetente, texto);
        if (daOferta.isPresent()) resultado = daOferta.get();
        else if (paraOAssistente(remetente, texto, resposta)) resultado = assistente.atender(remetente, texto);
        else resultado = resposta.isEmpty() ? orientar(remetente) : responder(remetente, resposta.get());
        recebidas.salvarRecebida(new MensagemRecebida(vinculo(remetente, resultado), remetente,
                Objects.requireNonNullElse(texto, ""), idMensagem, resposta.orElse(null), recebidaEm));
        return resultado;
    }

    private Optional<ResultadoResposta> motivoDaAusencia(Telefone remetente, String texto) {
        if (motivos == null || pedidoPendente(remetente).isPresent()) return Optional.empty();
        if (assistente != null && assistente.conversaAtiva(remetente)) return Optional.empty();
        List<Paciente> doTelefone = assistente != null ? assistente.pacientesDoTelefone(remetente) : pacientes.buscarPorTelefone(remetente);
        return motivos.responder(remetente, texto, doTelefone, assistente != null && assistente.pedidoDeMenu(texto));
    }

    private boolean paraOAssistente(Telefone remetente, String texto, Optional<RespostaPaciente> resposta) {
        if (assistente == null) return false;
        if (assistente.conversaAtiva(remetente)) return true;
        if (pedidoPendente(remetente).isEmpty()) return true;
        return resposta.isEmpty() && assistente.pedidoDeMenu(texto);
    }

    private Optional<Agendamento> pedidoPendente(Telefone remetente) {
        return registros.ultimoEnvio(remetente, TipoLembrete.PEDIDO_CONFIRMACAO)
                .flatMap(r -> agendamentos.buscarAgendamento(r.agendamentoId()))
                .filter(Agendamento::aguardandoResposta);
    }

    private UUID vinculo(Telefone remetente, ResultadoResposta resultado) {
        if (resultado.agendamentoId() != null) return resultado.agendamentoId();
        return registros.ultimoEnvio(remetente, TipoLembrete.PEDIDO_CONFIRMACAO).map(RegistroMensagem::agendamentoId).orElse(null);
    }

    private ResultadoResposta responder(Telefone remetente, RespostaPaciente resposta) {
        if (assistente != null && resposta == RespostaPaciente.REAGENDAR) {
            Optional<Agendamento> pendente = pedidoPendente(remetente).filter(Agendamento::doEncaminhamento);
            Optional<Paciente> paciente = pendente.flatMap(a -> pacientes.buscarPaciente(a.getPacienteId()));
            if (paciente.isPresent() && !assistente.podeRemarcar(paciente.get().id())) {
                assistente.avisarLimite(remetente, paciente.get());
                return ResultadoResposta.ignorada("Limite de remarcações do mês atingido");
            }
        }
        Aplicacao aplicacao = transacao.executar(() -> aplicar(remetente, resposta));
        if (aplicacao.ignorada() != null) return aplicacao.ignorada();

        Agendamento a = aplicacao.agendamento();
        boolean retornoEnviado = pacientes.buscarPaciente(a.getPacienteId())
                .map(p -> enviar(a.getId(), aplicacao.destino(), retorno(resposta, p, a)))
                .orElse(false);
        if (motivos != null && resposta == RespostaPaciente.CANCELAR) motivos.aposCancelamento(a, aplicacao.destino());
        return ResultadoResposta.aplicada(a.getId(), a.getStatus(), retornoEnviado);
    }

    private String retorno(RespostaPaciente resposta, Paciente paciente, Agendamento a) {
        if (assistente != null && resposta == RespostaPaciente.REAGENDAR && a.doEncaminhamento())
            return assistente.retornoDePedidoDeOutroHorario(paciente, a);
        return modelos.retornoResposta(resposta, paciente, a);
    }

    private Aplicacao aplicar(Telefone remetente, RespostaPaciente resposta) {
        Optional<RegistroMensagem> pedido = registros.ultimoEnvio(remetente, TipoLembrete.PEDIDO_CONFIRMACAO);
        if (pedido.isEmpty()) return Aplicacao.ignorada("Nenhum pedido de confirmação enviado para este telefone");

        Optional<Agendamento> encontrado = agendamentos.buscarAgendamentoParaAtualizar(pedido.get().agendamentoId());
        if (encontrado.isEmpty()) return Aplicacao.ignorada("Agendamento não encontrado");

        Agendamento a = encontrado.get();
        if (!a.aguardandoResposta())
            return Aplicacao.ignorada("Agendamento não está aguardando resposta: " + a.getStatus());

        a.responder(resposta);
        agendamentos.salvarAgendamento(a);
        if (a.doEncaminhamento() && !a.getStatus().ocupaAgenda())
            encaminhamentos.buscarEncaminhamentoParaAtualizar(a.getEncaminhamentoId()).ifPresent(e -> {
                e.consultaDesmarcada();
                encaminhamentos.salvarEncaminhamento(e);
            });
        return new Aplicacao(null, a, pedido.get().destino());
    }

    private ResultadoResposta orientar(Telefone remetente) {
        Optional<RegistroMensagem> pedido = registros.ultimoEnvio(remetente, TipoLembrete.PEDIDO_CONFIRMACAO);
        Optional<Agendamento> pendente = pedido.flatMap(r -> agendamentos.buscarAgendamento(r.agendamentoId()))
                .filter(Agendamento::aguardandoResposta);
        if (pendente.isEmpty()) return ResultadoResposta.ignorada("Resposta não reconhecida");

        Agendamento a = pendente.get();
        Optional<Paciente> paciente = pacientes.buscarPaciente(a.getPacienteId());
        if (paciente.isEmpty()) return ResultadoResposta.ignorada("Resposta não reconhecida");

        Telefone destino = pedido.get().destino();
        String texto = modelos.naoEntendi(paciente.get(), a);
        if (registros.enviadaDesde(destino, texto, Instant.now(clock).minus(INTERVALO_ENTRE_ORIENTACOES)))
            return ResultadoResposta.ignorada("Resposta não reconhecida; orientação já enviada há pouco");

        return ResultadoResposta.naoReconhecida(a.getId(), a.getStatus(), enviar(a.getId(), destino, texto));
    }

    private boolean enviar(UUID agendamentoId, Telefone destino, String texto) {
        ResultadoEnvio r = enviador.enviar(new MensagemSaida(agendamentoId, destino, null, texto));
        registros.salvarRegistro(new RegistroMensagem(agendamentoId, destino, null, texto,
                r.sucesso(), r.idExterno(), r.erro(), Instant.now(clock)));
        return r.sucesso();
    }
}
