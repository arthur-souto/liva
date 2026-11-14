package sus.liva.application.port.out;

import sus.liva.application.port.in.FaltasPorEspecialidade;
import sus.liva.application.port.in.MotivoContado;
import sus.liva.application.port.in.SemanaDeComparecimento;
import sus.liva.domain.model.ausencia.LigacaoRegistrada;
import sus.liva.domain.model.ausencia.MotivoRegistrado;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AusenciaRepositoryPort {
    void salvarMotivo(MotivoRegistrado motivo);
    void salvarLigacao(LigacaoRegistrada ligacao);
    Optional<MotivoRegistrado> buscarMotivo(UUID agendamentoId);
    Optional<MotivoRegistrado> perguntaPendente(Collection<UUID> pacientes, Instant desde);
    List<SemanaDeComparecimento> semanas(LocalDate primeiraSegunda, int quantidade);
    List<MotivoContado> motivos(Instant desde);
    List<FaltasPorEspecialidade> porEspecialidade(LocalDateTime desde, LocalDateTime ate);
    List<ConsultaComSinais> proximasConsultas(LocalDateTime de, LocalDateTime ate, int limite);
    long perguntasFeitas(Instant desde);
    long perguntasRespondidas(Instant desde);
}
