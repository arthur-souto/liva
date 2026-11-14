package sus.liva.application.usecase;

import sus.liva.application.port.in.AgendarAtendimentoUseCase;
import sus.liva.application.port.in.NovoAtendimento;
import sus.liva.application.port.out.AgendamentoRepositoryPort;
import sus.liva.application.port.out.PacienteRepositoryPort;
import sus.liva.application.port.out.TransacaoPort;
import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.agendamento.StatusAgendamento;
import sus.liva.domain.model.paciente.Paciente;
import sus.liva.domain.model.paciente.Sexo;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

public class AgendarAtendimentoUseCaseImpl implements AgendarAtendimentoUseCase {
    private final PacienteRepositoryPort pacientes;
    private final AgendamentoRepositoryPort agendamentos;
    private final TransacaoPort transacao;
    private final Clock clock;

    public AgendarAtendimentoUseCaseImpl(PacienteRepositoryPort pacientes, AgendamentoRepositoryPort agendamentos,
                                         TransacaoPort transacao, Clock clock) {
        this.pacientes = pacientes;
        this.agendamentos = agendamentos;
        this.transacao = transacao;
        this.clock = clock;
    }

    @Override
    public Agendamento agendar(NovoAtendimento novo) {
        exigir(novo.nomePaciente(), "Nome do paciente é obrigatório");
        exigir(novo.unidade(), "Unidade é obrigatória");
        exigir(novo.endereco(), "Endereço é obrigatório");
        if (novo.tipo() == null) throw new IllegalArgumentException("Tipo de atendimento é obrigatório");
        if (novo.dataHora() == null || !novo.dataHora().isAfter(LocalDateTime.now(clock)))
            throw new IllegalArgumentException("Data e hora do atendimento devem estar no futuro");

        Telefone telefone = telefone(novo.telefone(), "Telefone do paciente");
        Telefone telefoneCuidador = vazio(novo.telefoneCuidador()) ? null : telefone(novo.telefoneCuidador(), "Telefone do cuidador");
        if (telefoneCuidador != null) exigir(novo.nomeCuidador(), "Nome do cuidador é obrigatório quando há telefone do cuidador");

        Paciente paciente = new Paciente(UUID.randomUUID(), novo.nomePaciente().trim(), null, null, null, null,
                Sexo.NAO_INFORMADO, null, telefone, null, vazio(novo.nomeCuidador()) ? null : novo.nomeCuidador().trim(),
                telefoneCuidador, null, null, null, true);
        Agendamento agendamento = new Agendamento(StatusAgendamento.AGENDADO, vazio(novo.preparo()) ? null : novo.preparo(),
                novo.endereco(), novo.unidade(), novo.tipo(), novo.dataHora(), paciente.id(), UUID.randomUUID());

        return transacao.executar(() -> {
            pacientes.salvarPaciente(paciente);
            agendamentos.salvarAgendamento(agendamento);
            return agendamento;
        });
    }

    private static Telefone telefone(String valor, String campo) {
        try {
            return Telefone.de(valor);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(campo + " inválido: \"" + valor + "\". Use DDD + número, ex.: 51999999999");
        }
    }

    private static void exigir(String valor, String mensagem) {
        if (vazio(valor)) throw new IllegalArgumentException(mensagem);
    }

    private static boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }
}
