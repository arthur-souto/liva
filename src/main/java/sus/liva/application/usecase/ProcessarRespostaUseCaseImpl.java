package sus.liva.application.usecase;

import sus.liva.application.port.in.ProcessarRespostaUseCase;
import sus.liva.application.port.in.ResultadoResposta;
import sus.liva.application.port.out.AgendamentoRepositoryPort;
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
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class ProcessarRespostaUseCaseImpl implements ProcessarRespostaUseCase {
    private static final Duration INTERVALO_ENTRE_ORIENTACOES = Duration.ofMinutes(5);

    private final RegistroMensagemPort registros;
    private final MensagemRecebidaPort recebidas;
    private final AgendamentoRepositoryPort agendamentos;
    private final PacienteRepositoryPort pacientes;
    private final EnviadorMensagemPort enviador;
    private final ModeloMensagens modelos;
    private final TransacaoPort transacao;
    private final Clock clock;

    public ProcessarRespostaUseCaseImpl(RegistroMensagemPort registros, MensagemRecebidaPort recebidas,
                                        AgendamentoRepositoryPort agendamentos, PacienteRepositoryPort pacientes,
                                        EnviadorMensagemPort enviador, ModeloMensagens modelos,
                                        TransacaoPort transacao, Clock clock) {
        this.registros = registros;
        this.recebidas = recebidas;
        this.agendamentos = agendamentos;
        this.pacientes = pacientes;
        this.enviador = enviador;
        this.modelos = modelos;
        this.transacao = transacao;
        this.clock = clock;
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
        ResultadoResposta resultado = resposta.isEmpty() ? orientar(remetente) : responder(remetente, resposta.get());
        recebidas.salvarRecebida(new MensagemRecebida(vinculo(remetente, resultado), remetente,
                Objects.requireNonNullElse(texto, ""), idMensagem, resposta.orElse(null), recebidaEm));
        return resultado;
    }

    private UUID vinculo(Telefone remetente, ResultadoResposta resultado) {
        if (resultado.agendamentoId() != null) return resultado.agendamentoId();
        return registros.ultimoEnvio(remetente, TipoLembrete.PEDIDO_CONFIRMACAO).map(RegistroMensagem::agendamentoId).orElse(null);
    }

    private ResultadoResposta responder(Telefone remetente, RespostaPaciente resposta) {
        Aplicacao aplicacao = transacao.executar(() -> aplicar(remetente, resposta));
        if (aplicacao.ignorada() != null) return aplicacao.ignorada();

        Agendamento a = aplicacao.agendamento();
        boolean retornoEnviado = pacientes.buscarPaciente(a.getPacienteId())
                .map(p -> enviar(a.getId(), aplicacao.destino(), modelos.retornoResposta(resposta, p, a)))
                .orElse(false);
        return ResultadoResposta.aplicada(a.getId(), a.getStatus(), retornoEnviado);
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
