package sus.liva.application.port.in;

import sus.liva.domain.model.agendamento.NaturezaConsulta;
import sus.liva.domain.model.fila.StatusEntrada;
import sus.liva.domain.model.triagem.Prioridade;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record PosicaoNaFila(
        UUID entradaId,
        UUID encaminhamentoId,
        UUID triagemId,
        int triagemNumero,
        Instant triagemEm,
        String especialidade,
        String nomeEspecialidade,
        String profissional,
        Prioridade prioridade,
        NaturezaConsulta natureza,
        StatusEntrada status,
        Integer posicao,
        Integer total,
        Instant entrouEm,
        LocalDateTime consultaAtual,
        String motivo,
        Instant atualizadaEm,
        Integer aFrenteMaisUrgentes,
        Double vagasPorSemana
) {}
