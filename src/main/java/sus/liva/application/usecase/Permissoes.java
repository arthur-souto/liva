package sus.liva.application.usecase;

import sus.liva.application.exception.AcessoNegadoException;
import sus.liva.application.port.in.Ator;
import sus.liva.application.port.in.Papel;

import java.util.UUID;

final class Permissoes {
    private Permissoes() {
    }

    static void exigir(Ator ator, Papel... papeis) {
        if (ator == null || !ator.eh(papeis)) throw new AcessoNegadoException("Seu perfil não tem permissão para esta operação");
    }

    static UUID profissional(Ator ator) {
        if (ator.profissionalId() == null) throw new AcessoNegadoException("Usuário sem vínculo com um profissional de saúde");
        return ator.profissionalId();
    }

    static void exigirPacienteProprio(Ator ator, UUID pacienteId) {
        if (ator.eh(Papel.PACIENTE) && !pacienteId.equals(ator.pacienteId()))
            throw new AcessoNegadoException("Você só pode acessar os seus próprios atendimentos");
    }

    static AcessoNegadoException negado() {
        return new AcessoNegadoException("Você não tem acesso a este atendimento");
    }
}
