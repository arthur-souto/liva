package sus.liva.application.usecase;

import sus.liva.application.port.in.AgendarAtendimentoUseCase;
import sus.liva.application.port.in.AtendimentoAgendado;
import sus.liva.application.port.in.DispararLembreteUseCase;
import sus.liva.application.port.in.NovoAtendimento;
import sus.liva.application.port.in.ResultadoDisparo;
import sus.liva.application.port.out.AgendamentoRepositoryPort;
import sus.liva.application.port.out.PacienteRepositoryPort;
import sus.liva.application.port.out.TransacaoPort;
import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.agendamento.AntecedenciaDosLembretes;
import sus.liva.domain.model.agendamento.StatusAgendamento;
import sus.liva.domain.model.agendamento.TipoLembrete;
import sus.liva.domain.model.paciente.Paciente;
import sus.liva.domain.model.paciente.Sexo;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static sus.liva.application.usecase.ValidacaoCadastral.cpf;
import static sus.liva.application.usecase.ValidacaoCadastral.exigir;
import static sus.liva.application.usecase.ValidacaoCadastral.normalizar;
import static sus.liva.application.usecase.ValidacaoCadastral.telefone;
import static sus.liva.application.usecase.ValidacaoCadastral.vazio;

public class AgendarAtendimentoUseCaseImpl implements AgendarAtendimentoUseCase {
    private final PacienteRepositoryPort pacientes;
    private final AgendamentoRepositoryPort agendamentos;
    private final DispararLembreteUseCase dispararLembrete;
    private final TransacaoPort transacao;
    private final Clock clock;
    private final AntecedenciaDosLembretes antecedencia;

    public AgendarAtendimentoUseCaseImpl(PacienteRepositoryPort pacientes, AgendamentoRepositoryPort agendamentos,
                                         DispararLembreteUseCase dispararLembrete, TransacaoPort transacao, Clock clock) {
        this(pacientes, agendamentos, dispararLembrete, transacao, clock, AntecedenciaDosLembretes.padrao());
    }

    public AgendarAtendimentoUseCaseImpl(PacienteRepositoryPort pacientes, AgendamentoRepositoryPort agendamentos,
                                         DispararLembreteUseCase dispararLembrete, TransacaoPort transacao, Clock clock,
                                         AntecedenciaDosLembretes antecedencia) {
        this.antecedencia = antecedencia;
        this.pacientes = pacientes;
        this.agendamentos = agendamentos;
        this.dispararLembrete = dispararLembrete;
        this.transacao = transacao;
        this.clock = clock;
    }

    @Override
    public AtendimentoAgendado agendar(NovoAtendimento novo) {
        exigir(novo.nomePaciente(), "Nome do paciente é obrigatório");
        exigir(novo.unidade(), "Unidade é obrigatória");
        exigir(novo.endereco(), "Endereço é obrigatório");
        if (novo.tipo() == null) throw new IllegalArgumentException("Tipo de atendimento é obrigatório");
        if (novo.dataHora() == null || !novo.dataHora().isAfter(LocalDateTime.now(clock)))
            throw new IllegalArgumentException("Data e hora do atendimento devem estar no futuro");

        String nome = novo.nomePaciente().trim();
        String cpf = vazio(novo.cpf()) ? null : cpf(novo.cpf());
        Telefone telefone = telefone(novo.telefone(), "Telefone do paciente");
        Telefone telefoneCuidador = vazio(novo.telefoneCuidador()) ? null : telefone(novo.telefoneCuidador(), "Telefone do cuidador");
        if (telefoneCuidador != null) exigir(novo.nomeCuidador(), "Nome do cuidador é obrigatório quando há telefone do cuidador");

        String nomeCuidador = telefoneCuidador == null ? null : novo.nomeCuidador().trim();

        UUID agendamentoId = UUID.randomUUID();

        Cadastro cadastro = transacao.executar(() -> {
            Optional<Paciente> existente = buscarExistente(cpf, telefone, nome);
            Paciente paciente = existente
                    .map(p -> atualizar(p, cpf, telefone, nomeCuidador, telefoneCuidador))
                    .orElseGet(() -> new Paciente(UUID.randomUUID(), nome, null, cpf, null, null, Sexo.NAO_INFORMADO,
                            null, telefone, null, nomeCuidador, telefoneCuidador, null, null, null, true));
            Agendamento agendamento = new Agendamento(StatusAgendamento.AGENDADO,
                    vazio(novo.preparo()) ? null : novo.preparo().trim(), novo.endereco().trim(), novo.unidade().trim(),
                    novo.tipo(), novo.dataHora(), paciente.id(), agendamentoId);

            pacientes.salvarPaciente(paciente);
            agendamentos.salvarAgendamento(agendamento);
            return new Cadastro(agendamento, existente.isEmpty());
        });

        ResultadoDisparo confirmacao = notificar(agendamentoId, TipoLembrete.CONFIRMACAO_AGENDAMENTO);
        ResultadoDisparo pedido = dentroDoPrazoDoPedido(novo.dataHora().toLocalDate())
                ? notificar(agendamentoId, TipoLembrete.PEDIDO_CONFIRMACAO)
                : null;

        Agendamento atual = agendamentos.buscarAgendamento(agendamentoId).orElse(cadastro.agendamento());

        return new AtendimentoAgendado(atual, cadastro.pacienteNovo(), confirmacao, pedido);
    }

    private Optional<Paciente> buscarExistente(String cpf, Telefone telefone, String nome) {
        if (cpf != null) {
            Optional<Paciente> porCpf = pacientes.buscarPorCpf(cpf);
            if (porCpf.isPresent()) return porCpf;
        }
        return pacientes.buscarPorTelefone(telefone).stream()
                .filter(p -> normalizar(p.nomeCompleto()).equals(normalizar(nome)))
                .filter(p -> cpf == null || p.cpf() == null)
                .findFirst();
    }

    private static Paciente atualizar(Paciente p, String cpf, Telefone telefone, String nomeCuidador, Telefone telefoneCuidador) {
        boolean novoCuidador = telefoneCuidador != null;
        return new Paciente(p.id(), p.nomeCompleto(), p.nomeSocial(), cpf != null ? cpf : p.cpf(), p.cartaoSus(),
                p.dataNascimento(), p.sexo(), p.email(), telefone, p.endereco(),
                novoCuidador ? nomeCuidador : p.nomeCuidador(), novoCuidador ? telefoneCuidador : p.telefoneCuidador(),
                p.contatoEmergenciaNome(), p.contatoEmergenciaTelefone(), p.observacoes(), p.consentimento());
    }

    private boolean dentroDoPrazoDoPedido(LocalDate data) {
        return !data.isAfter(LocalDate.now(clock).plusDays(antecedencia.pedidoDeConfirmacao()));
    }

    private ResultadoDisparo notificar(UUID agendamentoId, TipoLembrete tipo) {
        try {
            return dispararLembrete.disparar(agendamentoId, tipo, false);
        } catch (RuntimeException e) {
            return ResultadoDisparo.ignorado("Falha ao enviar: " + e.getMessage());
        }
    }

    private record Cadastro(Agendamento agendamento, boolean pacienteNovo) {}
}
