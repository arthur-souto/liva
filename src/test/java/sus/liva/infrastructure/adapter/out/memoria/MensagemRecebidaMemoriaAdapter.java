package sus.liva.infrastructure.adapter.out.memoria;

import sus.liva.application.port.out.MensagemRecebidaPort;
import sus.liva.domain.model.mensagem.MensagemRecebida;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class MensagemRecebidaMemoriaAdapter implements MensagemRecebidaPort {
    private final List<MensagemRecebida> dados = new CopyOnWriteArrayList<>();

    @Override
    public void salvarRecebida(MensagemRecebida mensagem) {
        dados.add(mensagem);
    }

    @Override
    public List<MensagemRecebida> recebidasDoAgendamento(UUID agendamentoId) {
        return dados.stream()
                .filter(m -> agendamentoId.equals(m.agendamentoId()))
                .sorted(Comparator.comparing(MensagemRecebida::recebidaEm))
                .toList();
    }

    public List<MensagemRecebida> todas() {
        return List.copyOf(dados);
    }
}
