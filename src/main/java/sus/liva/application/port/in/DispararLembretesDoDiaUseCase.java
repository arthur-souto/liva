package sus.liva.application.port.in;

import java.time.LocalDate;

public interface DispararLembretesDoDiaUseCase {
    int dispararPendentes(LocalDate hoje);
}
