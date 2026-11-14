package sus.liva.domain.model.profissional;

import java.util.UUID;

public record Profissional(
        UUID id,
        String nome,
        String registroConselho,
        TipoProfissional tipo,
        String especialidade,
        String unidade,
        String endereco,
        boolean ativo
) {
    public boolean atende(String codigoEspecialidade) {
        return ativo && tipo == TipoProfissional.ESPECIALISTA && codigoEspecialidade.equals(especialidade);
    }
}
