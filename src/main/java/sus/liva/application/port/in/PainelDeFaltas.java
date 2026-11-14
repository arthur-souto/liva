package sus.liva.application.port.in;

import java.time.Instant;
import java.util.List;

public record PainelDeFaltas(
        Instant agora,
        List<SemanaDeComparecimento> semanas,
        List<MotivoContado> motivos,
        List<FaltasPorEspecialidade> especialidades,
        List<ConsultaEmRisco> emRisco,
        long perguntasFeitas,
        long perguntasRespondidas
) {}
