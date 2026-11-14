package sus.liva.application.port.in;

import java.time.Instant;
import java.util.List;

public record PainelDaFila(
        Instant agora,
        FiltroDaFila filtro,
        boolean podeIntervir,
        IndicadoresDaFila indicadores,
        Pagina<PacienteNaFila> fila,
        List<VagaNoPainel> vagas,
        List<OfertaNoPainel> ofertas,
        List<ConsultaSemConfirmacao> semConfirmacao,
        List<MovimentacaoDaFila> movimentacoes
) {}
