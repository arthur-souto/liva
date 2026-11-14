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
import sus.liva.domain.model.agendamento.TipoLembrete;
import sus.liva.domain.model.mensagem.RegistroMensagem;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "registros_mensagem")
public class RegistroMensagemEntity {
    private static final int TAMANHO_TEXTO = 4000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "agendamento_id")
    private UUID agendamentoId;
    @Column(nullable = false, length = 20)
    private String destino;
    @Column(name = "chave_destino", nullable = false, length = 10)
    private String chaveDestino;
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private TipoLembrete tipo;
    @Column(nullable = false, length = TAMANHO_TEXTO)
    private String texto;
    @Column(nullable = false)
    private boolean sucesso;
    @Column(name = "id_externo", length = 100)
    private String idExterno;
    @Column(length = TAMANHO_TEXTO)
    private String erro;
    @Column(name = "enviado_em", nullable = false)
    private Instant enviadoEm;

    protected RegistroMensagemEntity() {
    }

    static RegistroMensagemEntity de(RegistroMensagem r) {
        RegistroMensagemEntity e = new RegistroMensagemEntity();
        e.agendamentoId = r.agendamentoId();
        e.destino = r.destino().e164();
        e.chaveDestino = r.destino().chaveComparacao();
        e.tipo = r.tipo();
        e.texto = limitar(r.texto());
        e.sucesso = r.sucesso();
        e.idExterno = r.idExterno();
        e.erro = limitar(r.erro());
        e.enviadoEm = r.enviadoEm();
        return e;
    }

    RegistroMensagem paraDominio() {
        return new RegistroMensagem(agendamentoId, new Telefone(destino), tipo, texto, sucesso, idExterno, erro, enviadoEm);
    }

    private static String limitar(String valor) {
        return valor == null || valor.length() <= TAMANHO_TEXTO ? valor : valor.substring(0, TAMANHO_TEXTO);
    }
}
