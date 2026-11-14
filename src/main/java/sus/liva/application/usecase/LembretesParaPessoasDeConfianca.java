package sus.liva.application.usecase;

import sus.liva.application.port.in.DispararLembreteUseCase;
import sus.liva.application.port.in.ResultadoDisparo;
import sus.liva.application.port.out.AgendamentoRepositoryPort;
import sus.liva.application.port.out.CuidadorRepositoryPort;
import sus.liva.application.port.out.EnviadorMensagemPort;
import sus.liva.application.port.out.MensagemSaida;
import sus.liva.application.port.out.PacienteRepositoryPort;
import sus.liva.application.port.out.RegistroMensagemPort;
import sus.liva.application.port.out.ResultadoEnvio;
import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.agendamento.TipoLembrete;
import sus.liva.domain.model.cuidador.CuidadorVinculado;
import sus.liva.domain.model.mensagem.RegistroMensagem;
import sus.liva.domain.model.paciente.Paciente;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class LembretesParaPessoasDeConfianca implements DispararLembreteUseCase {
    private final DispararLembreteUseCase delegado;
    private final AgendamentoRepositoryPort agendamentos;
    private final PacienteRepositoryPort pacientes;
    private final CuidadorRepositoryPort cuidadores;
    private final RegistroMensagemPort registros;
    private final EnviadorMensagemPort enviador;
    private final ModeloMensagens modelos;
    private final Clock clock;

    public LembretesParaPessoasDeConfianca(DispararLembreteUseCase delegado, AgendamentoRepositoryPort agendamentos,
                                           PacienteRepositoryPort pacientes, CuidadorRepositoryPort cuidadores,
                                           RegistroMensagemPort registros, EnviadorMensagemPort enviador, ModeloMensagens modelos,
                                           Clock clock) {
        this.delegado = delegado;
        this.agendamentos = agendamentos;
        this.pacientes = pacientes;
        this.cuidadores = cuidadores;
        this.registros = registros;
        this.enviador = enviador;
        this.modelos = modelos;
        this.clock = clock;
    }

    @Override
    public ResultadoDisparo disparar(UUID agendamentoId, TipoLembrete tipo, boolean forcar) {
        ResultadoDisparo resultado = delegado.disparar(agendamentoId, tipo, forcar);
        if (resultado.envios().isEmpty()) return resultado;

        Optional<Agendamento> agendamento = agendamentos.buscarAgendamento(agendamentoId);
        Optional<Paciente> paciente = agendamento.flatMap(a -> pacientes.buscarPaciente(a.getPacienteId()));
        if (paciente.isEmpty()) return resultado;

        Set<String> jaAvisados = new HashSet<>();
        resultado.envios().forEach(r -> jaAvisados.add(r.destino().chaveComparacao()));

        String texto = modelos.montar(tipo, paciente.get(), agendamento.get());
        List<RegistroMensagem> envios = new ArrayList<>(resultado.envios());

        for (CuidadorVinculado vinculado : cuidadores.cuidadoresDoPaciente(paciente.get().id())) {
            if (!jaAvisados.add(vinculado.cuidador().telefone().chaveComparacao())) continue;

            String mensagem = modelos.paraPessoaDeConfianca(vinculado.cuidador().nome(), paciente.get(), texto);
            ResultadoEnvio r = enviador.enviar(new MensagemSaida(agendamentoId, vinculado.cuidador().telefone(), tipo, mensagem));
            RegistroMensagem registro = new RegistroMensagem(agendamentoId, vinculado.cuidador().telefone(), tipo, mensagem, r.sucesso(),
                    r.idExterno(), r.erro(), Instant.now(clock));

            registros.salvarRegistro(registro);
            envios.add(registro);
        }

        return ResultadoDisparo.enviado(envios);
    }
}
