package sus.liva.application.port.in;

import sus.liva.domain.model.ausencia.MotivoDeAusencia;

public record MotivoContado(MotivoDeAusencia motivo, long faltas, long cancelamentos) {}
