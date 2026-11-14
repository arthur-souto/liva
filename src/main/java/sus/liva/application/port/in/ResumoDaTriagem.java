package sus.liva.application.port.in;

import sus.liva.domain.model.triagem.Prioridade;
import sus.liva.domain.model.triagem.SinaisVitais;
import sus.liva.domain.model.triagem.StatusTriagem;

import java.time.Instant;
import java.util.UUID;

public record ResumoDaTriagem(UUID id, StatusTriagem status, Instant chegadaEm, Instant finalizadaEm,
                              Prioridade prioridade, String queixaPrincipal, UUID profissionalId,
                              SinaisVitais sinaisVitais) {}
