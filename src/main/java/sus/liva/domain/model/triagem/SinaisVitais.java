package sus.liva.domain.model.triagem;

import java.math.BigDecimal;

public record SinaisVitais(
        Integer pressaoSistolica,
        Integer pressaoDiastolica,
        Integer frequenciaCardiaca,
        Integer frequenciaRespiratoria,
        BigDecimal temperatura,
        Integer saturacao
) {
    public static SinaisVitais naoColetados() {
        return new SinaisVitais(null, null, null, null, null, null);
    }
}
