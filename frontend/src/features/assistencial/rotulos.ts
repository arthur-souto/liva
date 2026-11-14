import type { Tom } from "@/shared/ui/Etiqueta";
import type { Modalidade, NaturezaConsulta, Prioridade, StatusConsulta, StatusEncaminhamento, StatusTriagem } from "./tipos";

type Rotulo = { texto: string; tom: Tom };

export const STATUS_DA_TRIAGEM: Record<StatusTriagem, Rotulo> = {
  AGUARDANDO_ATENDIMENTO: { texto: "Aguardando", tom: "alerta" },
  EM_ANDAMENTO: { texto: "Em triagem", tom: "info" },
  FINALIZADA: { texto: "Finalizada", tom: "sucesso" },
  CANCELADA: { texto: "Cancelada", tom: "neutro" },
  ENCERRADA: { texto: "Encerrada", tom: "marca" },
};

export const STATUS_DO_ENCAMINHAMENTO: Record<StatusEncaminhamento, Rotulo> = {
  PENDENTE: { texto: "Pendente", tom: "neutro" },
  AGUARDANDO_AGENDAMENTO: { texto: "Aguardando agendamento", tom: "alerta" },
  AGENDADO: { texto: "Agendado", tom: "info" },
  EM_ATENDIMENTO: { texto: "Em atendimento", tom: "marca" },
  CONCLUIDO: { texto: "Concluído", tom: "sucesso" },
  CANCELADO: { texto: "Cancelado", tom: "neutro" },
};

export const STATUS_DA_CONSULTA: Record<StatusConsulta, Rotulo> = {
  AGENDADO: { texto: "Agendada", tom: "info" },
  AGUARDANDO_RESPOSTA: { texto: "Aguardando confirmação", tom: "alerta" },
  CONFIRMADO: { texto: "Confirmada", tom: "sucesso" },
  CANCELADO: { texto: "Cancelada", tom: "neutro" },
  REAGENDADO: { texto: "Pediu outro horário", tom: "destaque" },
  COMPARECEU: { texto: "Realizada", tom: "sucesso" },
  FALTOU: { texto: "Faltou", tom: "perigo" },
  EM_ANDAMENTO: { texto: "Em atendimento", tom: "marca" },
};

export const PRIORIDADES: Record<Prioridade, Rotulo> = {
  EMERGENCIA: { texto: "Emergência", tom: "perigo" },
  MUITO_URGENTE: { texto: "Muito urgente", tom: "destaque" },
  URGENTE: { texto: "Urgente", tom: "alerta" },
  POUCO_URGENTE: { texto: "Pouco urgente", tom: "sucesso" },
  NAO_URGENTE: { texto: "Não urgente", tom: "info" },
};

export const ORDEM_DAS_PRIORIDADES: Prioridade[] = ["EMERGENCIA", "MUITO_URGENTE", "URGENTE", "POUCO_URGENTE", "NAO_URGENTE"];

export const MODALIDADES: Record<Modalidade, string> = {
  ELETIVO: "Eletivo",
  IMEDIATO: "Imediato (plantão)",
};

export const NATUREZAS: Record<NaturezaConsulta, string> = {
  AVULSA: "Atendimento",
  INICIAL: "Primeira consulta",
  RETORNO: "Retorno",
};

export function prioridadeGrave(prioridade: Prioridade | null): boolean {
  return prioridade === "EMERGENCIA" || prioridade === "MUITO_URGENTE";
}

export function nomeDaEspecialidade(codigo: string): string {
  const nome = codigo.toLowerCase().replace(/_/g, " ");
  return nome.charAt(0).toUpperCase() + nome.slice(1);
}
