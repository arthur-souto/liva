package sus.liva.application.port.in;

import sus.liva.domain.model.paciente.Endereco;
import sus.liva.domain.model.paciente.Sexo;

import java.time.LocalDate;

public record DadosCadastrais(
        String nomeCompleto,
        String nomeSocial,
        String cpf,
        String cartaoSus,
        LocalDate dataNascimento,
        Sexo sexo,
        String email,
        String telefone,
        Endereco endereco,
        String nomeCuidador,
        String telefoneCuidador,
        String contatoEmergenciaNome,
        String contatoEmergenciaTelefone,
        String observacoes,
        boolean consentimento
) {}
