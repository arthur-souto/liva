package sus.liva.application.usecase;

import sus.liva.application.port.in.Ator;
import sus.liva.application.port.in.Papel;
import sus.liva.application.port.out.ProfissionalRepositoryPort;
import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.encaminhamento.Encaminhamento;
import sus.liva.domain.model.profissional.Profissional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class AcessoAoRamo {
    private final ProfissionalRepositoryPort profissionais;

    AcessoAoRamo(ProfissionalRepositoryPort profissionais) {
        this.profissionais = profissionais;
    }

    boolean podeLerClinico(Ator ator, UUID pacienteId, List<Encaminhamento> encaminhamentos, List<Agendamento> consultas) {
        return switch (ator.papel()) {
            case CLINICO_GERAL -> true;
            case ESPECIALISTA -> especialistaVinculado(ator.profissionalId(), encaminhamentos, consultas);
            case PACIENTE -> pacienteId.equals(ator.pacienteId());
            default -> false;
        };
    }

    void exigirLeituraClinica(Ator ator, UUID pacienteId, List<Encaminhamento> encaminhamentos, List<Agendamento> consultas) {
        if (!podeLerClinico(ator, pacienteId, encaminhamentos, consultas)) throw Permissoes.negado();
    }

    boolean especialistaVinculado(UUID profissionalId, List<Encaminhamento> encaminhamentos, List<Agendamento> consultas) {
        if (profissionalId == null) return false;
        if (consultas.stream().anyMatch(c -> c.doProfissional(profissionalId))) return true;

        Optional<Profissional> profissional = profissionais.buscarProfissional(profissionalId);
        return encaminhamentos.stream().anyMatch(e -> e.destinadoA(profissionalId)
                || (e.getProfissionalDestinoId() == null && profissional.filter(p -> p.atende(e.getEspecialidade())).isPresent()));
    }

    void exigirEspecialistaDoEncaminhamento(Ator ator, Encaminhamento encaminhamento, List<Agendamento> consultas) {
        Permissoes.exigir(ator, Papel.ESPECIALISTA);
        if (!especialistaVinculado(Permissoes.profissional(ator), List.of(encaminhamento), consultas)) throw Permissoes.negado();
    }
}
