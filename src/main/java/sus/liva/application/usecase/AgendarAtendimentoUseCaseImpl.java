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
import sus.liva.domain.model.agendamento.StatusAgendamento;
import sus.liva.domain.model.agendamento.TipoLembrete;
import sus.liva.domain.model.paciente.Paciente;
import sus.liva.domain.model.paciente.Sexo;

import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class AgendarAtendimentoUseCaseImpl implements AgendarAtendimentoUseCase {
    private final PacienteRepositoryPort pacientes;
    private final AgendamentoRepositoryPort agendamentos;
    private final DispararLembreteUseCase dispararLembrete;
    private final TransacaoPort transacao;
    private final Clock clock;

    public AgendarAtendimentoUseCaseImpl(PacienteRepositoryPort pacientes, AgendamentoRepositoryPort agendamentos,
                                         DispararLembreteUseCase dispararLembrete, TransacaoPort transacao, Clock clock) {
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
        return !data.isAfter(LocalDate.now(clock).plusDays(TipoLembrete.PEDIDO_CONFIRMACAO.getDiasAntes()));
    }

    private ResultadoDisparo notificar(UUID agendamentoId, TipoLembrete tipo) {
        try {
            return dispararLembrete.disparar(agendamentoId, tipo, false);
        } catch (RuntimeException e) {
            return ResultadoDisparo.ignorado("Falha ao enviar: " + e.getMessage());
        }
    }

    private static String cpf(String valor) {
        String d = valor.replaceAll("\\D", "");
        if (d.length() != 11 || d.chars().distinct().count() == 1 || !digitosConferem(d))
            throw new IllegalArgumentException("CPF inválido: \"" + valor + "\"");
        return d;
    }

    private static boolean digitosConferem(String d) {
        return digito(d, 9) == d.charAt(9) - '0' && digito(d, 10) == d.charAt(10) - '0';
    }

    private static int digito(String d, int posicao) {
        int soma = 0;
        for (int i = 0; i < posicao; i++) soma += (d.charAt(i) - '0') * (posicao + 1 - i);
        int resto = soma * 10 % 11;
        return resto == 10 ? 0 : resto;
    }

    private static String normalizar(String nome) {
        return Normalizer.normalize(nome, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
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

    private record Cadastro(Agendamento agendamento, boolean pacienteNovo) {}
}
