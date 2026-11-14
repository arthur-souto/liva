package sus.liva.domain.model.fila;

import java.time.Instant;
import java.util.UUID;

public class OfertaDeVaga {
    private final UUID id;
    private final UUID vagaId;
    private final UUID entradaId;
    private final UUID pacienteId;
    private final UUID consultaAnteriorId;
    private final Instant criadaEm;
    private final Instant expiraEm;
    private StatusOferta status;
    private Instant enviadaEm;
    private String idExterno;
    private Instant respondidaEm;
    private CanalDeResposta canal;
    private String motivo;
    private UUID consultaNovaId;

    public OfertaDeVaga(UUID id, UUID vagaId, UUID entradaId, UUID pacienteId, UUID consultaAnteriorId, StatusOferta status,
                        Instant criadaEm, Instant expiraEm, Instant enviadaEm, String idExterno, Instant respondidaEm,
                        CanalDeResposta canal, String motivo, UUID consultaNovaId) {
        this.id = id;
        this.vagaId = vagaId;
        this.entradaId = entradaId;
        this.pacienteId = pacienteId;
        this.consultaAnteriorId = consultaAnteriorId;
        this.status = status;
        this.criadaEm = criadaEm;
        this.expiraEm = expiraEm;
        this.enviadaEm = enviadaEm;
        this.idExterno = idExterno;
        this.respondidaEm = respondidaEm;
        this.canal = canal;
        this.motivo = motivo;
        this.consultaNovaId = consultaNovaId;
    }

    public static OfertaDeVaga nova(UUID id, VagaLiberada vaga, EntradaNaFila entrada, UUID consultaAnteriorId, Instant agora,
                                    Instant expiraEm) {
        if (!vaga.aberta()) throw new IllegalStateException("Só é possível ofertar uma vaga aberta");
        if (!entrada.podeReceberOferta()) throw new IllegalStateException("Esta entrada não está aguardando vaga");
        if (!entrada.disputa(vaga.getProfissionalId(), vaga.getEspecialidade()))
            throw new IllegalArgumentException("A vaga não atende o encaminhamento desta entrada");
        if (!expiraEm.isAfter(agora)) throw new IllegalArgumentException("O prazo da oferta precisa estar no futuro");

        return new OfertaDeVaga(id, vaga.getId(), entrada.getId(), entrada.getPacienteId(), consultaAnteriorId,
                StatusOferta.PENDENTE, agora, expiraEm, null, null, null, null, null, null);
    }

    public boolean ativa() {
        return status.ativa();
    }

    public boolean vencida(Instant agora) {
        return !agora.isBefore(expiraEm);
    }

    public boolean pertenceA(UUID paciente) {
        return pacienteId.equals(paciente);
    }

    public void enviada(String idDaMensagem, Instant em) {
        if (status != StatusOferta.PENDENTE) return;

        status = StatusOferta.ENVIADA;
        idExterno = idDaMensagem;
        enviadaEm = em;
    }

    public void aceitar(UUID consultaNova, CanalDeResposta pelo, Instant agora) {
        exigirRespondivel(agora);
        if (consultaNova == null) throw new IllegalArgumentException("A aceitação precisa gerar a nova consulta");

        status = StatusOferta.ACEITA;
        consultaNovaId = consultaNova;
        canal = pelo;
        respondidaEm = agora;
    }

    public void recusar(CanalDeResposta pelo, Instant agora) {
        exigirRespondivel(agora);

        status = StatusOferta.RECUSADA;
        canal = pelo;
        respondidaEm = agora;
    }

    public void expirar(Instant agora) {
        if (!ativa()) throw new IllegalStateException("Só ofertas em aberto expiram. Situação atual: " + status);
        if (!vencida(agora)) throw new IllegalStateException("O prazo desta oferta ainda não terminou");

        status = StatusOferta.EXPIRADA;
        motivo = "Prazo de resposta encerrado";
    }

    public void cancelar(String justificativa) {
        if (!ativa()) throw new IllegalStateException("Só ofertas em aberto podem ser canceladas. Situação atual: " + status);
        status = StatusOferta.CANCELADA;
        motivo = justificativa;
    }

    private void exigirRespondivel(Instant agora) {
        if (!ativa()) throw new IllegalStateException("Esta oferta não está mais disponível. Situação atual: " + status);
        if (vencida(agora)) throw new IllegalStateException("O prazo para responder esta oferta terminou");
    }

    public UUID getId() {
        return id;
    }

    public UUID getVagaId() {
        return vagaId;
    }

    public UUID getEntradaId() {
        return entradaId;
    }

    public UUID getPacienteId() {
        return pacienteId;
    }

    public UUID getConsultaAnteriorId() {
        return consultaAnteriorId;
    }

    public StatusOferta getStatus() {
        return status;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public Instant getExpiraEm() {
        return expiraEm;
    }

    public Instant getEnviadaEm() {
        return enviadaEm;
    }

    public String getIdExterno() {
        return idExterno;
    }

    public Instant getRespondidaEm() {
        return respondidaEm;
    }

    public CanalDeResposta getCanal() {
        return canal;
    }

    public String getMotivo() {
        return motivo;
    }

    public UUID getConsultaNovaId() {
        return consultaNovaId;
    }
}
