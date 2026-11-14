package sus.liva.application.port.in;

import sus.liva.domain.model.agendamento.TipoAtendimento;

import java.time.LocalDateTime;

public record NovoAtendimento(
        String nomePaciente,
        String telefone,
        String nomeCuidador,
        String telefoneCuidador,
        LocalDateTime dataHora,
        TipoAtendimento tipo,
        String unidade,
        String endereco,
        String preparo
) {}
