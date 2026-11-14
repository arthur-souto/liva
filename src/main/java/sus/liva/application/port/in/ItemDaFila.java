package sus.liva.application.port.in;

import sus.liva.domain.model.triagem.StatusTriagem;

import java.time.Instant;
import java.util.UUID;

public record ItemDaFila(UUID triagemId, UUID pacienteId, String nomePaciente, Instant chegadaEm, StatusTriagem status,
                         UUID profissionalId, long versao) {}
