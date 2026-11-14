package sus.liva.application.port.in;

import java.util.UUID;

public record PacienteNaFila(UUID pacienteId, String nomePaciente, PosicaoNaFila posicao, OfertaParaPaciente ofertaAtiva) {}
