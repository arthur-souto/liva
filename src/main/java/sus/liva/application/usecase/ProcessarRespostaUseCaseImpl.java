package sus.liva.application.usecase;

import sus.liva.application.port.in.ProcessarRespostaUseCase;
import sus.liva.application.port.in.ResultadoResposta;
import sus.liva.application.port.out.AgendamentoRepositoryPort;
import sus.liva.application.port.out.EnviadorMensagemPort;
import sus.liva.application.port.out.MensagemSaida;
import sus.liva.application.port.out.PacienteRepositoryPort;
import sus.liva.application.port.out.RegistroMensagemPort;
import sus.liva.application.port.out.TransacaoPort;
import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.agendamento.RespostaPaciente;
import sus.liva.domain.model.agendamento.TipoLembrete;
import sus.liva.domain.model.mensagem.RegistroMensagem;

import java.util.Optional;

public class ProcessarRespostaUseCaseImpl implements ProcessarRespostaUseCase {
    private final RegistroMensagemPort registros;
    private final AgendamentoRepositoryPort agendamentos;
    private final PacienteRepositoryPort pacientes;
    private final EnviadorMensagemPort enviador;
    private final ModeloMensagens modelos;
    private final TransacaoPort transacao;

    public ProcessarRespostaUseCaseImpl(RegistroMensagemPort registros, AgendamentoRepositoryPort agendamentos,
                                        PacienteRepositoryPort pacientes, EnviadorMensagemPort enviador,
                                        ModeloMensagens modelos, TransacaoPort transacao) {
        this.registros = registros;
        this.agendamentos = agendamentos;
        this.pacientes = pacientes;
        this.enviador = enviador;
        this.modelos = modelos;
        this.transacao = transacao;
    }

    private record Aplicacao(ResultadoResposta ignorada, Agendamento agendamento, Telefone destino) {
        static Aplicacao ignorada(String motivo) {
            return new Aplicacao(ResultadoResposta.ignorada(motivo), null, null);
        }
    }

    @Override
    public ResultadoResposta processar(Telefone remetente, String texto) {
        Optional<RespostaPaciente> resposta = RespostaPaciente.interpretar(texto);
        if (resposta.isEmpty()) return ResultadoResposta.ignorada("Resposta não reconhecida");

        Aplicacao aplicacao = transacao.executar(() -> aplicar(remetente, resposta.get()));
        if (aplicacao.ignorada() != null) return aplicacao.ignorada();

        Agendamento a = aplicacao.agendamento();
        boolean retornoEnviado = enviarRetorno(resposta.get(), a, aplicacao.destino());
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

    private boolean enviarRetorno(RespostaPaciente resposta, Agendamento a, Telefone destino) {
        return pacientes.buscarPaciente(a.getPacienteId())
                .map(p -> modelos.retornoResposta(resposta, p, a))
                .map(texto -> enviador.enviar(new MensagemSaida(a.getId(), destino, null, texto)).sucesso())
                .orElse(false);
    }
}
