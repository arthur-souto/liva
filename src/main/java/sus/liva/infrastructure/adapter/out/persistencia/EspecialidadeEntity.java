package sus.liva.infrastructure.adapter.out.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import sus.liva.domain.model.profissional.Especialidade;

@Entity
@Table(name = "especialidades")
public class EspecialidadeEntity {
    @Id
    @Column(length = 40)
    private String codigo;
    @Column(nullable = false, length = 100)
    private String nome;
    @Column(nullable = false)
    private boolean plantao;

    protected EspecialidadeEntity() {
    }

    Especialidade paraDominio() {
        return new Especialidade(codigo, nome, plantao);
    }
}
