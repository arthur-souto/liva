import { requisitar } from "@/shared/api/cliente";
import type {
  AvaliacaoClinica,
  Consulta,
  ConsultaDaAgenda,
  DadosDoPaciente,
  Encaminhamento,
  EncaminhamentoRecebido,
  Especialidade,
  Evolucao,
  Historico,
  HorarioDisponivel,
  ItemDaFila,
  NovoEncaminhamento,
  Paciente,
  Prioridade,
  Profissional,
  Ramo,
  Retificacao,
  SinaisVitais,
  Triagem,
} from "./tipos";

const id = encodeURIComponent;

export function pesquisarPacientes(termo: string): Promise<Paciente[]> {
  return requisitar<Paciente[]>("/pacientes", { parametros: { termo } });
}

export function buscarPaciente(pacienteId: string): Promise<Paciente> {
  return requisitar<Paciente>(`/pacientes/${id(pacienteId)}`);
}

export function cadastrarPaciente(dados: DadosDoPaciente): Promise<Paciente> {
  return requisitar<Paciente>("/pacientes", { metodo: "POST", corpo: dados });
}

export function atualizarPaciente(pacienteId: string, dados: DadosDoPaciente, versao: number): Promise<Paciente> {
  return requisitar<Paciente>(`/pacientes/${id(pacienteId)}`, { metodo: "PUT", corpo: { ...dados, versao } });
}

export function historicoDoPaciente(pacienteId: string): Promise<Historico> {
  return requisitar<Historico>(`/pacientes/${id(pacienteId)}/historico`);
}

export function meuHistorico(): Promise<Historico> {
  return requisitar<Historico>("/portal/historico");
}

export function registrarChegada(pacienteId: string): Promise<Triagem> {
  return requisitar<Triagem>("/triagens", { metodo: "POST", corpo: { pacienteId } });
}

export function filaDeTriagem(): Promise<ItemDaFila[]> {
  return requisitar<ItemDaFila[]>("/triagens/fila");
}

export function detalharTriagem(triagemId: string): Promise<Ramo> {
  return requisitar<Ramo>(`/triagens/${id(triagemId)}`);
}

export function iniciarTriagem(triagemId: string): Promise<Triagem> {
  return requisitar<Triagem>(`/triagens/${id(triagemId)}/iniciar`, { metodo: "POST" });
}

export function registrarAvaliacao(
  triagemId: string,
  avaliacao: AvaliacaoClinica,
  sinaisVitais: SinaisVitais,
  prioridade: Prioridade | null,
  justificativaPrioridade: string | null,
  versao: number,
): Promise<Triagem> {
  return requisitar<Triagem>(`/triagens/${id(triagemId)}/avaliacao`, {
    metodo: "PUT",
    corpo: { avaliacao, sinaisVitais, prioridade, justificativaPrioridade, versao },
  });
}

export function encaminhar(triagemId: string, novo: NovoEncaminhamento): Promise<Encaminhamento> {
  return requisitar<Encaminhamento>(`/triagens/${id(triagemId)}/encaminhamentos`, { metodo: "POST", corpo: novo });
}

export function finalizarTriagem(triagemId: string, versao: number): Promise<Triagem> {
  return requisitar<Triagem>(`/triagens/${id(triagemId)}/finalizar`, { metodo: "POST", corpo: { versao } });
}

export function cancelarTriagem(triagemId: string, motivo: string): Promise<Triagem> {
  return requisitar<Triagem>(`/triagens/${id(triagemId)}/cancelar`, { metodo: "POST", corpo: { motivo } });
}

export function retificarTriagem(triagemId: string, texto: string, justificativa: string): Promise<Retificacao> {
  return requisitar<Retificacao>(`/triagens/${id(triagemId)}/retificacoes`, { metodo: "POST", corpo: { texto, justificativa } });
}

export function encerrarTriagem(triagemId: string): Promise<Triagem> {
  return requisitar<Triagem>(`/triagens/${id(triagemId)}/encerrar`, { metodo: "POST" });
}

export function encaminhamentosRecebidos(): Promise<EncaminhamentoRecebido[]> {
  return requisitar<EncaminhamentoRecebido[]>("/encaminhamentos/recebidos");
}

export function receberNoPlantao(encaminhamentoId: string): Promise<Encaminhamento> {
  return requisitar<Encaminhamento>(`/encaminhamentos/${id(encaminhamentoId)}/receber`, { metodo: "POST" });
}

export function autorizarRetorno(encaminhamentoId: string): Promise<Encaminhamento> {
  return requisitar<Encaminhamento>(`/encaminhamentos/${id(encaminhamentoId)}/retorno`, { metodo: "POST" });
}

export function concluirEncaminhamento(encaminhamentoId: string, desfecho: string): Promise<Encaminhamento> {
  return requisitar<Encaminhamento>(`/encaminhamentos/${id(encaminhamentoId)}/concluir`, { metodo: "POST", corpo: { desfecho } });
}

export function cancelarEncaminhamento(encaminhamentoId: string, motivo: string): Promise<Encaminhamento> {
  return requisitar<Encaminhamento>(`/encaminhamentos/${id(encaminhamentoId)}/cancelar`, { metodo: "POST", corpo: { motivo } });
}

export function horariosDisponiveis(encaminhamentoId: string, inicio: string, fim: string): Promise<HorarioDisponivel[]> {
  return requisitar<HorarioDisponivel[]>(`/encaminhamentos/${id(encaminhamentoId)}/horarios`, { parametros: { inicio, fim } });
}

export function agendarConsulta(encaminhamentoId: string, profissionalId: string, inicio: string, chave: string): Promise<Consulta> {
  return requisitar<Consulta>(`/encaminhamentos/${id(encaminhamentoId)}/consultas`, {
    metodo: "POST",
    corpo: { profissionalId, inicio },
    cabecalhos: { "Idempotency-Key": chave },
  });
}

export function cancelarConsulta(consultaId: string, versao: number): Promise<Consulta> {
  return requisitar<Consulta>(`/consultas/${id(consultaId)}/cancelar`, { metodo: "POST", corpo: { versao } });
}

export function agendaDoEspecialista(inicio: string, fim: string): Promise<ConsultaDaAgenda[]> {
  return requisitar<ConsultaDaAgenda[]>("/consultas/agenda", { parametros: { inicio, fim } });
}

export function iniciarConsulta(consultaId: string, versao: number): Promise<Consulta> {
  return requisitar<Consulta>(`/consultas/${id(consultaId)}/iniciar`, { metodo: "POST", corpo: { versao } });
}

export function finalizarConsulta(consultaId: string, evolucao: string, autorizarRetorno: boolean, versao: number): Promise<Consulta> {
  return requisitar<Consulta>(`/consultas/${id(consultaId)}/finalizar`, { metodo: "POST", corpo: { evolucao, autorizarRetorno, versao } });
}

export function registrarEvolucao(consultaId: string, texto: string): Promise<Evolucao> {
  return requisitar<Evolucao>(`/consultas/${id(consultaId)}/evolucoes`, { metodo: "POST", corpo: { texto } });
}

export function registrarFalta(consultaId: string, versao: number): Promise<Consulta> {
  return requisitar<Consulta>(`/consultas/${id(consultaId)}/falta`, { metodo: "POST", corpo: { versao } });
}

export function especialidades(): Promise<Especialidade[]> {
  return requisitar<Especialidade[]>("/catalogo/especialidades");
}

export function especialistas(especialidade: string): Promise<Profissional[]> {
  return requisitar<Profissional[]>("/catalogo/especialistas", { parametros: { especialidade } });
}
