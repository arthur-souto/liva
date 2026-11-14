package sus.liva.infrastructure.adapter.out.persistencia;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import sus.liva.application.exception.ConflitoException;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

final class Restricoes {
    private static final Map<String, String> MENSAGENS = Map.of(
            "ex_agenda_do_profissional", "Este horário acabou de ser reservado. Escolha outro horário.",
            "uk_pacientes_cpf", "Já existe um paciente cadastrado com este CPF.",
            "uk_pacientes_cartao_sus", "Já existe um paciente cadastrado com este Cartão SUS.",
            "uk_triagem_aberta_por_paciente", "Este paciente já está na fila ou em triagem.",
            "uk_agendamentos_idempotencia", "Esta solicitação de agendamento já foi processada.",
            "fk_consulta_do_ramo", "A consulta precisa pertencer à mesma triagem e ao mesmo paciente do encaminhamento.",
            "fk_encaminhamento_triagem_do_paciente", "O encaminhamento precisa pertencer à triagem do mesmo paciente.",
            "uk_usuarios_login", "Já existe um usuário com este login.",
            "uk_usuarios_paciente", "Este paciente já possui acesso ao portal.");

    private Restricoes() {
    }

    static <T> T traduzir(Supplier<T> operacao) {
        try {
            return operacao.get();
        } catch (DataIntegrityViolationException e) {
            String detalhe = Objects.toString(e.getMostSpecificCause().getMessage(), "");
            throw MENSAGENS.entrySet().stream()
                    .filter(m -> detalhe.contains(m.getKey()))
                    .findFirst()
                    .<RuntimeException>map(m -> new ConflitoException(m.getValue()))
                    .orElse(e);
        }
    }

    static void conferirVersao(Long esperada, Long atual, Class<?> entidade, Object id) {
        if (esperada != null && atual != null && !esperada.equals(atual))
            throw new ObjectOptimisticLockingFailureException(entidade, id);
    }
}
