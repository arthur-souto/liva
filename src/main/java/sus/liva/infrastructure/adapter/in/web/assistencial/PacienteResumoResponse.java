package sus.liva.infrastructure.adapter.in.web.assistencial;

import sus.liva.domain.model.paciente.Paciente;

import java.time.LocalDate;
import java.util.UUID;

public record PacienteResumoResponse(UUID id, String nomeCompleto, String nomeSocial, LocalDate dataNascimento, String telefone) {

    static PacienteResumoResponse de(Paciente p) {
        return new PacienteResumoResponse(p.id(), p.nomeCompleto(), p.nomeSocial(), p.dataNascimento(),
                PacienteResponse.digitos(p.telefone()));
    }
}
