package sus.liva.application.usecase;

import sus.liva.application.port.in.AgendamentoDetalhado;
import sus.liva.application.port.in.AgendamentosPaginados;
import sus.liva.application.port.in.Pagina;
import sus.liva.application.port.in.PedidoDePagina;
import sus.liva.application.port.in.ListarAgendamentosUseCase;
import sus.liva.application.port.out.AgendamentoRepositoryPort;
import sus.liva.application.port.out.PacienteRepositoryPort;
import sus.liva.domain.model.agendamento.StatusAgendamento;
import sus.liva.domain.model.paciente.Paciente;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class ListarAgendamentosUseCaseImpl implements ListarAgendamentosUseCase {
    private static final int MAXIMO_DE_DIAS = 31;

    private final AgendamentoRepositoryPort agendamentos;
    private final PacienteRepositoryPort pacientes;

    public ListarAgendamentosUseCaseImpl(AgendamentoRepositoryPort agendamentos, PacienteRepositoryPort pacientes) {
        this.agendamentos = agendamentos;
        this.pacientes = pacientes;
    }

    @Override
    public List<AgendamentoDetalhado> listar(LocalDate inicio, LocalDate fim, Set<StatusAgendamento> status) {
        if (inicio == null || fim == null) throw new IllegalArgumentException("Informe o período");
        if (fim.isBefore(inicio)) throw new IllegalArgumentException("A data final deve ser igual ou posterior à inicial");
        if (ChronoUnit.DAYS.between(inicio, fim) >= MAXIMO_DE_DIAS)
            throw new IllegalArgumentException("O período pode ter no máximo " + MAXIMO_DE_DIAS + " dias");

        Map<UUID, Optional<Paciente>> cache = new HashMap<>();
        return agendamentos.agendamentosEntre(inicio, fim).stream()
                .filter(a -> status == null || status.isEmpty() || status.contains(a.getStatus()))
                .map(a -> new AgendamentoDetalhado(a,
                        cache.computeIfAbsent(a.getPacienteId(), pacientes::buscarPaciente).orElse(null)))
                .toList();
    }

    @Override
    public AgendamentosPaginados listar(LocalDate inicio, LocalDate fim, Set<StatusAgendamento> status, String busca,
                                        PedidoDePagina pedido) {
        String termo = busca == null ? "" : busca.trim().toLowerCase();
        String digitos = termo.replaceAll("\\D", "");
        List<AgendamentoDetalhado> doPeriodo = listar(inicio, fim, null).stream()
                .filter(d -> termo.isEmpty() || corresponde(d.paciente(), termo, digitos))
                .toList();
        Map<StatusAgendamento, Long> porStatus = new LinkedHashMap<>();
        Arrays.stream(StatusAgendamento.values())
                .forEach(s -> porStatus.put(s, doPeriodo.stream().filter(d -> d.agendamento().getStatus() == s).count()));
        List<AgendamentoDetalhado> filtrados = doPeriodo.stream()
                .filter(d -> status == null || status.isEmpty() || status.contains(d.agendamento().getStatus()))
                .toList();
        return new AgendamentosPaginados(Pagina.de(filtrados, pedido), doPeriodo.size(), porStatus);
    }

    private static boolean corresponde(Paciente p, String termo, String digitos) {
        if (p == null) return false;
        boolean porNome = Stream.of(p.nomeCompleto(), p.nomeCuidador())
                .anyMatch(nome -> nome != null && nome.toLowerCase().contains(termo));
        boolean porTelefone = !digitos.isEmpty() && Stream.of(p.telefone(), p.telefoneCuidador())
                .anyMatch(t -> t != null && t.apenasDigitos().contains(digitos));
        return porNome || porTelefone;
    }
}
