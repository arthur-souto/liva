package sus.liva.infrastructure.adapter.in.web.assistencial;

import sus.liva.application.port.in.EncaminhamentoRecebido;

public record EncaminhamentoRecebidoResponse(EncaminhamentoResponse encaminhamento, String nomePaciente) {

    static EncaminhamentoRecebidoResponse de(EncaminhamentoRecebido r) {
        return new EncaminhamentoRecebidoResponse(EncaminhamentoResponse.de(r.encaminhamento()), r.nomePaciente());
    }
}
