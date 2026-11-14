package sus.liva.infrastructure.adapter.in.web.assistencial;

import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.paciente.CadastroDePaciente;
import sus.liva.domain.model.paciente.Endereco;
import sus.liva.domain.model.paciente.Paciente;
import sus.liva.domain.model.paciente.Sexo;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PacienteResponse(UUID id, String nomeCompleto, String nomeSocial, String cpf, String cartaoSus,
                               LocalDate dataNascimento, Sexo sexo, String email, String telefone, Endereco endereco,
                               String nomeCuidador, String telefoneCuidador, String contatoEmergenciaNome,
                               String contatoEmergenciaTelefone, String observacoes, boolean consentimento, long versao,
                               Instant criadoEm, Instant atualizadoEm) {

    static PacienteResponse de(CadastroDePaciente c) {
        Paciente p = c.paciente();
        return new PacienteResponse(p.id(), p.nomeCompleto(), p.nomeSocial(), p.cpf(), p.cartaoSus(), p.dataNascimento(),
                p.sexo(), p.email(), digitos(p.telefone()), p.endereco(), p.nomeCuidador(), digitos(p.telefoneCuidador()),
                p.contatoEmergenciaNome(), digitos(p.contatoEmergenciaTelefone()), p.observacoes(), p.consentimento(),
                c.versao(), c.criadoEm(), c.atualizadoEm());
    }

    static String digitos(Telefone telefone) {
        return telefone == null ? null : telefone.apenasDigitos();
    }
}
