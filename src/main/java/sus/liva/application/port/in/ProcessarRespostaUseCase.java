package sus.liva.application.port.in;

import sus.liva.domain.model.Telefone;

public interface ProcessarRespostaUseCase {
    ResultadoResposta processar(Telefone remetente, String texto, String idMensagem);

    default ResultadoResposta processar(Telefone remetente, String texto) {
        return processar(remetente, texto, null);
    }
}
