package sus.liva.application.port.in;

import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.agendamento.EvolucaoClinica;
import sus.liva.domain.model.encaminhamento.Encaminhamento;
import sus.liva.domain.model.triagem.Retificacao;
import sus.liva.domain.model.triagem.Triagem;

import java.util.List;

public record RamoDeAtendimento(Triagem triagem, List<Encaminhamento> encaminhamentos, List<Agendamento> consultas,
                                List<Retificacao> retificacoes, List<EvolucaoClinica> evolucoes) {}
