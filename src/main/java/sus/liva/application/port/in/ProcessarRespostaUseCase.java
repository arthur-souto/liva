package sus.liva.application.port.in;

import sus.liva.domain.model.Telefone;

public interface ProcessarRespostaUseCase {
    ResultadoResposta processar(Telefone remetente, String texto);
}
