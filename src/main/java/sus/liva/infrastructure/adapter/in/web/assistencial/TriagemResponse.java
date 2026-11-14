package sus.liva.infrastructure.adapter.in.web.assistencial;

import sus.liva.domain.model.triagem.AvaliacaoClinica;
import sus.liva.domain.model.triagem.Prioridade;
import sus.liva.domain.model.triagem.SinaisVitais;
import sus.liva.domain.model.triagem.StatusTriagem;
import sus.liva.domain.model.triagem.Triagem;

import java.time.Instant;
import java.util.UUID;

public record TriagemResponse(UUID id, UUID pacienteId, StatusTriagem status, Instant chegadaEm, UUID profissionalId,
                              Instant iniciadaEm, Instant finalizadaEm, Instant encerradaEm, Instant canceladaEm,
                              String motivoCancelamento, AvaliacaoClinica avaliacao, SinaisVitais sinaisVitais,
                              Prioridade prioridade, String justificativaPrioridade, Long versao) {

    static TriagemResponse de(Triagem t) {
        return new TriagemResponse(t.getId(), t.getPacienteId(), t.getStatus(), t.getChegadaEm(), t.getProfissionalId(),
                t.getIniciadaEm(), t.getFinalizadaEm(), t.getEncerradaEm(), t.getCanceladaEm(), t.getMotivoCancelamento(),
                t.getAvaliacao(), t.getSinaisVitais(), t.getPrioridade(), t.getJustificativaPrioridade(), t.getVersao());
    }
}
