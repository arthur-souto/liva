import { requisitar } from "@/shared/api/cliente";
import type { Pagina } from "@/shared/api/pagina";
import type {
  Atendimentos,
  AvaliacaoClinica,
  Consulta,
  ConsultasDoPortal,
  ConsultaDaAgenda,
  DadosDoPaciente,
  Encaminhamento,
  EncaminhamentoRecebido,
  Especialidade,
  Evolucao,
  FiltrosDeAtendimentos,
  FiltrosDeConsultas,
  Historico,
  HorarioDisponivel,
  ItemDaFila,
  NovoEncaminhamento,
  Paciente,
  Parentesco,
  PessoaDeConfianca,
  Prioridade,
  Profissional,
  Ramo,
  Retificacao,
  SinaisVitais,
  Triagem,
} from "./tipos";

const id = encodeURIComponent;

function comoParametros(filtros: Record<string, string | number | undefined>): Record<string, string | undefined> {
  return Object.fromEntries(
    Object.entries(filtros).map(([nome, valor]) => [nome, valor === undefined || valor === "" ? undefined : String(valor)]),
  );
}

export function pesquisarPacientesPaginado(termo: string, pagina: number, tamanho = 10): Promise<Pagina<Paciente>> {
  return requisitar<Pagina<Paciente>>("/pacientes/pagina", { parametros: comoParametros({ termo, pagina, tamanho }) });
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

export function atendimentosDoPaciente(pacienteId: string, filtros: FiltrosDeAtendimentos): Promise<Atendimentos> {
  return requisitar<Atendimentos>(`/pacientes/${id(pacienteId)}/atendimentos`, { parametros: comoParametros(filtros) });
}

export function meusAtendimentos(filtros: FiltrosDeAtendimentos): Promise<Atendimentos> {
  return requisitar<Atendimentos>("/portal/atendimentos", { parametros: comoParametros(filtros) });
}

export function minhasConsultas(filtros: FiltrosDeConsultas): Promise<ConsultasDoPortal> {
  return requisitar<ConsultasDoPortal>("/portal/consultas", { parametros: comoParametros(filtros) });
}

export function meuHistorico(): Promise<Historico> {
  return requisitar<Historico>("/portal/historico");
}

export function pessoasDeConfianca(pacienteId: string): Promise<PessoaDeConfianca[]> {
  return requisitar<PessoaDeConfianca[]>(`/pacientes/${id(pacienteId)}/pessoas-de-confianca`);
}

export function vincularPessoaDeConfianca(
  pacienteId: string,
  dados: { nome: string; cpf: string; telefone: string; parentesco: Parentesco; autorizacaoRegistrada: boolean },
): Promise<PessoaDeConfianca> {
  return requisitar<PessoaDeConfianca>(`/pacientes/${id(pacienteId)}/pessoas-de-confianca`, { metodo: "POST", corpo: dados });
}

export function revogarPessoaDeConfianca(pacienteId: string, cuidadorId: string): Promise<void> {
  return requisitar<void>(`/pacientes/${id(pacienteId)}/pessoas-de-confianca/${id(cuidadorId)}`, { metodo: "DELETE" });
}

export function minhasRemarcacoes(): Promise<{ limite: number; usadas: number; restantes: number }> {
  return requisitar("/portal/remarcacoes");
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

export function remarcarConsulta(consultaId: string, profissionalId: string, inicio: string, versao: number): Promise<Consulta> {
  return requisitar<Consulta>(`/consultas/${id(consultaId)}/remarcar`, { metodo: "POST", corpo: { profissionalId, inicio, versao } });
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
