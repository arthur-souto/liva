package sus.liva.domain.model.paciente;

import sus.liva.domain.model.Telefone;
import java.time.LocalDate;
import java.util.UUID;

public record Paciente(
        UUID id,
        String nomeCompleto,
        String nomeSocial,
        String cpf,
        String cartaoSus,
        LocalDate dataNascimento,
        Sexo sexo,
        String email,
        Telefone telefone,
        Endereco endereco,
        String nomeCuidador,
        Telefone telefoneCuidador,
        String contatoEmergenciaNome,
        Telefone contatoEmergenciaTelefone,
        String observacoes,
        boolean consentimento
) {
    public boolean temCuidador() {
        return telefoneCuidador != null;
    }
}
