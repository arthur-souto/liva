package sus.liva.infrastructure.adapter.in.web.assistencial;

import sus.liva.application.port.in.Pagina;
import sus.liva.domain.model.paciente.CadastroDePaciente;

import java.util.List;

public record PacientesResponse(List<PacienteResponse> itens, int pagina, int tamanho, long total, int totalPaginas) {

    static PacientesResponse de(Pagina<CadastroDePaciente> p) {
        return new PacientesResponse(p.itens().stream().map(PacienteResponse::de).toList(), p.numero(), p.tamanho(), p.total(),
                p.totalPaginas());
    }
}
