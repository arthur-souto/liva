package sus.liva.application.port.in;

import sus.liva.domain.model.encaminhamento.Modalidade;
import sus.liva.domain.model.encaminhamento.StatusEncaminhamento;
import sus.liva.domain.model.triagem.Prioridade;

import java.time.Instant;
import java.util.UUID;

public record ResumoDoEncaminhamento(UUID id, String especialidade, Modalidade modalidade, Prioridade prioridade,
                                     StatusEncaminhamento status, Instant criadoEm, UUID profissionalDestinoId,
                                     String justificativa, String desfecho, long versao) {}
