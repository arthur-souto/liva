package sus.liva.infrastructure.adapter.in.web.assistencial;

import sus.liva.application.port.in.DadosCadastrais;
import sus.liva.domain.model.paciente.Endereco;
import sus.liva.domain.model.paciente.Sexo;

import java.time.LocalDate;

public record PacienteRequest(String nomeCompleto, String nomeSocial, String cpf, String cartaoSus, LocalDate dataNascimento,
                              Sexo sexo, String email, String telefone, Endereco endereco, String nomeCuidador,
                              String telefoneCuidador, String contatoEmergenciaNome, String contatoEmergenciaTelefone,
                              String observacoes, Boolean consentimento, Long versao) {

    DadosCadastrais paraDados() {
        return new DadosCadastrais(nomeCompleto, nomeSocial, cpf, cartaoSus, dataNascimento, sexo, email, telefone, endereco,
                nomeCuidador, telefoneCuidador, contatoEmergenciaNome, contatoEmergenciaTelefone, observacoes,
                Boolean.TRUE.equals(consentimento));
    }
}
