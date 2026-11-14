package sus.liva.application.usecase;

import sus.liva.application.exception.NaoEncontradoException;
import sus.liva.application.port.in.DispararLembreteUseCase;
import sus.liva.application.port.in.ResultadoDisparo;
import sus.liva.application.port.out.AgendamentoRepositoryPort;
import sus.liva.application.port.out.EnviadorMensagemPort;
import sus.liva.application.port.out.MensagemSaida;
import sus.liva.application.port.out.PacienteRepositoryPort;
import sus.liva.application.port.out.RegistroMensagemPort;
import sus.liva.application.port.out.ResultadoEnvio;
import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.agendamento.TipoLembrete;
import sus.liva.domain.model.mensagem.RegistroMensagem;
import sus.liva.domain.model.paciente.Paciente;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DispararLembreteUseCaseImpl implements DispararLembreteUseCase {
    private final AgendamentoRepositoryPort agendamentos;
    private final PacienteRepositoryPort pacientes;
    private final RegistroMensagemPort registros;
    private final EnviadorMensagemPort enviador;
    private final ModeloMensagens modelos;

    public DispararLembreteUseCaseImpl(AgendamentoRepositoryPort agendamentos, PacienteRepositoryPort pacientes,
                                       RegistroMensagemPort registros, EnviadorMensagemPort enviador,
                                       ModeloMensagens modelos) {
        this.agendamentos = agendamentos;
        this.pacientes = pacientes;
        this.registros = registros;
        this.enviador = enviador;
        this.modelos = modelos;
    }

    @Override
    public ResultadoDisparo disparar(UUID agendamentoId, TipoLembrete tipo, boolean forcar) {
        Agendamento a = agendamentos.buscarAgendamento(agendamentoId)
                .orElseThrow(() -> new NaoEncontradoException("Agendamento não encontrado: " + agendamentoId));
        Paciente p = pacientes.buscarPaciente(a.getPacienteId())
                .orElseThrow(() -> new IllegalStateException("Paciente não encontrado"));

        if (!p.consentimento()) return ResultadoDisparo.ignorado("Paciente sem consentimento");
        if (!a.podeReceber(tipo)) return ResultadoDisparo.ignorado("Status não permite: " + a.getStatus());
        if (!forcar && registros.jaEnviado(agendamentoId, tipo)) return ResultadoDisparo.ignorado("Já enviado");

        String texto = modelos.montar(tipo, p, a);
        List<RegistroMensagem> envios = new ArrayList<>();
        envios.add(enviar(a, p.telefone(), tipo, texto));
        if (p.temCuidador())
            envios.add(enviar(a, p.telefoneCuidador(), tipo, modelos.paraCuidador(p, texto)));

        if (tipo == TipoLembrete.PEDIDO_CONFIRMACAO && envios.get(0).sucesso()) {
            a.aguardarResposta();
            agendamentos.salvarAgendamento(a);
        }
        return ResultadoDisparo.enviado(envios);
    }

    private RegistroMensagem enviar(Agendamento a, Telefone destino, TipoLembrete tipo, String texto) {
        ResultadoEnvio r = enviador.enviar(new MensagemSaida(a.getId(), destino, tipo, texto));
        RegistroMensagem reg = new RegistroMensagem(a.getId(), destino, tipo, texto,
                r.sucesso(), r.idExterno(), r.erro(), Instant.now());
        registros.salvarRegistro(reg);
        return reg;
    }
}
