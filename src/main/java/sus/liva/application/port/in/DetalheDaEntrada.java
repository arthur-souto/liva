package sus.liva.application.port.in;

import java.util.List;
import java.util.UUID;

public record DetalheDaEntrada(UUID pacienteId, String nomePaciente, PosicaoNaFila posicao, List<OfertaNoPainel> ofertas,
                               List<MovimentacaoDaFila> movimentacoes) {}
