package sus.liva.infrastructure.adapter.in.web.assistencial;

import sus.liva.domain.model.encaminhamento.Encaminhamento;
import sus.liva.domain.model.encaminhamento.Modalidade;
import sus.liva.domain.model.encaminhamento.StatusEncaminhamento;
import sus.liva.domain.model.triagem.Prioridade;

import java.time.Instant;
import java.util.UUID;

public record EncaminhamentoResponse(UUID id, UUID triagemId, UUID pacienteId, String especialidade, UUID profissionalDestinoId,
                                     Modalidade modalidade, Prioridade prioridade, String justificativa,
                                     StatusEncaminhamento status, UUID criadoPor, Instant criadoEm, UUID recebidoPor,
                                     Instant recebidoEm, Instant encerradoEm, String desfecho, Long versao) {

    static EncaminhamentoResponse de(Encaminhamento e) {
        return new EncaminhamentoResponse(e.getId(), e.getTriagemId(), e.getPacienteId(), e.getEspecialidade(),
                e.getProfissionalDestinoId(), e.getModalidade(), e.getPrioridade(), e.getJustificativa(), e.getStatus(),
                e.getCriadoPor(), e.getCriadoEm(), e.getRecebidoPor(), e.getRecebidoEm(), e.getEncerradoEm(), e.getDesfecho(),
                e.getVersao());
    }
}
