package sus.liva.application.usecase;

import sus.liva.application.port.out.CuidadorRepositoryPort;
import sus.liva.application.port.out.EnviadorMensagemPort;
import sus.liva.application.port.out.MensagemSaida;
import sus.liva.application.port.out.PacienteRepositoryPort;
import sus.liva.application.port.out.RegistroMensagemPort;
import sus.liva.application.port.out.ResultadoEnvio;
import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.cuidador.CuidadorVinculado;
import sus.liva.domain.model.mensagem.RegistroMensagem;
import sus.liva.domain.model.paciente.Paciente;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

public class NotificacaoAoPaciente {
    private static final System.Logger LOG = System.getLogger(NotificacaoAoPaciente.class.getName());

    private final PacienteRepositoryPort pacientes;
    private final EnviadorMensagemPort enviador;
    private final RegistroMensagemPort registros;
    private final ModeloMensagens modelos;
    private final CuidadorRepositoryPort cuidadores;
    private final Clock clock;

    public NotificacaoAoPaciente(PacienteRepositoryPort pacientes, EnviadorMensagemPort enviador, RegistroMensagemPort registros,
                                 ModeloMensagens modelos, CuidadorRepositoryPort cuidadores, Clock clock) {
        this.cuidadores = cuidadores;
        this.pacientes = pacientes;
        this.enviador = enviador;
        this.registros = registros;
        this.modelos = modelos;
        this.clock = clock;
    }

    public void notificar(UUID agendamentoId, UUID pacienteId, Function<Paciente, String> texto) {
        notificar(agendamentoId, pacienteId, texto, texto);
    }

    public boolean notificar(UUID agendamentoId, UUID pacienteId, Function<Paciente, String> paraPaciente,
                             Function<Paciente, String> paraAcompanhantes) {
        try {
            Optional<Paciente> paciente = pacientes.buscarPaciente(pacienteId).filter(Paciente::consentimento);
            if (paciente.isEmpty()) return false;

            Paciente p = paciente.get();
            Set<String> avisados = new HashSet<>();

            avisados.add(p.telefone().chaveComparacao());

            boolean enviada = enviar(agendamentoId, p.telefone(), paraPaciente.apply(p));
            String mensagem = paraAcompanhantes.apply(p);

            if (p.temCuidador() && avisados.add(p.telefoneCuidador().chaveComparacao()))
                enviar(agendamentoId, p.telefoneCuidador(), modelos.paraCuidador(p, mensagem));

            for (CuidadorVinculado vinculado : cuidadores.cuidadoresDoPaciente(p.id()))
                if (avisados.add(vinculado.cuidador().telefone().chaveComparacao()))
                    enviada |= enviar(agendamentoId, vinculado.cuidador().telefone(),
                            modelos.paraPessoaDeConfianca(vinculado.cuidador().nome(), p, mensagem));

            return enviada;
        } catch (RuntimeException e) {
            LOG.log(System.Logger.Level.WARNING, "Falha ao notificar o paciente " + pacienteId + " pelo WhatsApp", e);
            return false;
        }
    }

    public boolean enviar(UUID agendamentoId, Telefone destino, String texto) {
        ResultadoEnvio r = enviador.enviar(new MensagemSaida(agendamentoId, destino, null, texto));
        registros.salvarRegistro(new RegistroMensagem(agendamentoId, destino, null, texto, r.sucesso(), r.idExterno(), r.erro(),
                Instant.now(clock)));
        if (!r.sucesso()) LOG.log(System.Logger.Level.WARNING, "Mensagem ao paciente não enviada: " + r.erro());
        return r.sucesso();
    }
}
