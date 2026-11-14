package sus.liva.infrastructure.adapter.out.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import sus.liva.domain.model.profissional.Profissional;
import sus.liva.domain.model.profissional.TipoProfissional;

import java.util.UUID;

@Entity
@Table(name = "profissionais")
public class ProfissionalEntity {
    @Id
    private UUID id;
    @Column(nullable = false, length = 150)
    private String nome;
    @Column(name = "registro_conselho", nullable = false, length = 30)
    private String registroConselho;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoProfissional tipo;
    @Column(name = "especialidade_codigo", length = 40)
    private String especialidade;
    @Column(nullable = false, length = 150)
    private String unidade;
    @Column(nullable = false, length = 255)
    private String endereco;
    @Column(nullable = false)
    private boolean ativo;

    protected ProfissionalEntity() {
    }

    static ProfissionalEntity de(Profissional p) {
        ProfissionalEntity e = new ProfissionalEntity();
        e.id = p.id();
        e.nome = p.nome();
        e.registroConselho = p.registroConselho();
        e.tipo = p.tipo();
        e.especialidade = p.especialidade();
        e.unidade = p.unidade();
        e.endereco = p.endereco();
        e.ativo = p.ativo();
        return e;
    }

    Profissional paraDominio() {
        return new Profissional(id, nome, registroConselho, tipo, especialidade, unidade, endereco, ativo);
    }
}
