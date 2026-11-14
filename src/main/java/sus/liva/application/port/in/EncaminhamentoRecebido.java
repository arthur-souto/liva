package sus.liva.application.port.in;

import sus.liva.domain.model.encaminhamento.Encaminhamento;

public record EncaminhamentoRecebido(Encaminhamento encaminhamento, String nomePaciente) {}
