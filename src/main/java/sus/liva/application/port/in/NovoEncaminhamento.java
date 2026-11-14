package sus.liva.application.port.in;

import sus.liva.domain.model.encaminhamento.Modalidade;
import sus.liva.domain.model.triagem.Prioridade;

import java.util.UUID;

public record NovoEncaminhamento(String especialidade, UUID profissionalDestinoId, Modalidade modalidade,
                                 Prioridade prioridade, String justificativa) {}
