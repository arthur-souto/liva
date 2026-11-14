package sus.liva.infrastructure.adapter.out.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import sus.liva.domain.model.triagem.Retificacao;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "retificacoes_triagem")
public class RetificacaoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "triagem_id", nullable = false, updatable = false)
    private UUID triagemId;
    @Column(name = "autor_id", nullable = false, updatable = false)
    private UUID autorId;
    @Column(nullable = false, length = 2000, updatable = false)
    private String texto;
    @Column(nullable = false, length = 1000, updatable = false)
    private String justificativa;
    @Column(name = "registrada_em", nullable = false, updatable = false)
    private Instant registradaEm;

    protected RetificacaoEntity() {
    }

    static RetificacaoEntity de(Retificacao r) {
        RetificacaoEntity e = new RetificacaoEntity();
        e.triagemId = r.triagemId();
        e.autorId = r.autorId();
        e.texto = r.texto();
        e.justificativa = r.justificativa();
        e.registradaEm = r.registradaEm();
        return e;
    }

    Retificacao paraDominio() {
        return new Retificacao(triagemId, autorId, texto, justificativa, registradaEm);
    }
}
