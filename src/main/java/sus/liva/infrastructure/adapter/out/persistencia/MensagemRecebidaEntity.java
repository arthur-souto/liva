package sus.liva.infrastructure.adapter.out.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.agendamento.RespostaPaciente;
import sus.liva.domain.model.mensagem.MensagemRecebida;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mensagens_recebidas")
public class MensagemRecebidaEntity {
    private static final int TAMANHO_TEXTO = 4000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "agendamento_id")
    private UUID agendamentoId;
    @Column(nullable = false, length = 20)
    private String remetente;
    @Column(nullable = false, length = TAMANHO_TEXTO)
    private String texto;
    @Column(name = "id_externo", length = 100)
    private String idExterno;
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RespostaPaciente resposta;
    @Column(name = "recebida_em", nullable = false)
    private Instant recebidaEm;

    protected MensagemRecebidaEntity() {
    }

    static MensagemRecebidaEntity de(MensagemRecebida m) {
        MensagemRecebidaEntity e = new MensagemRecebidaEntity();
        e.agendamentoId = m.agendamentoId();
        e.remetente = m.remetente().e164();
        e.texto = m.texto().length() <= TAMANHO_TEXTO ? m.texto() : m.texto().substring(0, TAMANHO_TEXTO);
        e.idExterno = m.idExterno();
        e.resposta = m.resposta();
        e.recebidaEm = m.recebidaEm();
        return e;
    }

    MensagemRecebida paraDominio() {
        return new MensagemRecebida(agendamentoId, new Telefone(remetente), texto, idExterno, resposta, recebidaEm);
    }
}
