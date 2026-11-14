package sus.liva.infrastructure.adapter.out.memoria;

import sus.liva.application.port.out.RegistroMensagemPort;
import sus.liva.domain.model.Telefone;
import sus.liva.domain.model.agendamento.TipoLembrete;
import sus.liva.domain.model.mensagem.RegistroMensagem;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class RegistroMensagemMemoriaAdapter implements RegistroMensagemPort {
    private final List<RegistroMensagem> dados = new CopyOnWriteArrayList<>();

    @Override
    public void salvarRegistro(RegistroMensagem registro) {
        dados.add(registro);
    }

    @Override
    public boolean jaEnviado(UUID agendamentoId, TipoLembrete tipo) {
        return dados.stream().anyMatch(r -> r.sucesso()
                && r.agendamentoId().equals(agendamentoId)
                && r.tipo() == tipo);
    }

    @Override
    public Optional<RegistroMensagem> ultimoEnvio(Telefone destino, TipoLembrete tipo) {
        String chave = destino.chaveComparacao();
        return dados.stream()
                .filter(r -> r.sucesso() && r.tipo() == tipo && r.destino().chaveComparacao().equals(chave))
                .max(Comparator.comparing(RegistroMensagem::enviadoEm));
    }
}
