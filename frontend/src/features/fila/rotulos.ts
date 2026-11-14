import type { Tom } from "@/shared/ui/Etiqueta";
import type { DecisaoSemResposta, EventoDaFila, OrigemDaVaga, StatusEntrada, StatusOferta, StatusVaga } from "./tipos";

type Rotulo = { texto: string; tom: Tom };

export const STATUS_DA_ENTRADA: Record<StatusEntrada, Rotulo> = {
  AGUARDANDO: { texto: "Na fila", tom: "info" },
  PAUSADA: { texto: "Pausada", tom: "alerta" },
  ATENDIDA: { texto: "Vaga garantida", tom: "sucesso" },
  ENCERRADA: { texto: "Fora da fila", tom: "neutro" },
};

export const STATUS_DA_OFERTA: Record<StatusOferta, Rotulo> = {
  PENDENTE: { texto: "Enviando", tom: "neutro" },
  ENVIADA: { texto: "Aguardando resposta", tom: "alerta" },
  ACEITA: { texto: "Aceita", tom: "sucesso" },
  RECUSADA: { texto: "Recusada", tom: "neutro" },
  EXPIRADA: { texto: "Expirada", tom: "perigo" },
  CANCELADA: { texto: "Cancelada", tom: "neutro" },
};

export const STATUS_DA_VAGA: Record<StatusVaga, Rotulo> = {
  ABERTA: { texto: "Aberta", tom: "destaque" },
  PREENCHIDA: { texto: "Reaproveitada", tom: "sucesso" },
  ENCERRADA: { texto: "Encerrada", tom: "neutro" },
  INDISPONIVEL: { texto: "Indisponível", tom: "perigo" },
};

export const ORIGEM_DA_VAGA: Record<OrigemDaVaga, string> = {
  CANCELAMENTO: "Cancelamento",
  SEM_CONFIRMACAO: "Sem confirmação",
  ANTECIPACAO: "Antecipação",
};

export const DECISAO_SEM_RESPOSTA: Record<DecisaoSemResposta, Rotulo> = {
  MANTIDA_SEM_RESPOSTA: { texto: "Mantida sem resposta", tom: "alerta" },
  SEM_CONTATO: { texto: "Sem contato", tom: "perigo" },
  LIBERADA: { texto: "Vaga liberada", tom: "neutro" },
  CONFIRMADA_NA_RECEPCAO: { texto: "Confirmada pela equipe", tom: "sucesso" },
};

export const EVENTOS: Record<EventoDaFila, string> = {
  ENTROU_NA_FILA: "Entrou na fila",
  SAIU_DA_FILA: "Saiu da fila",
  PAUSADA: "Fila pausada",
  RETOMADA: "Fila retomada",
  VAGA_LIBERADA: "Vaga liberada",
  VAGA_ENCERRADA: "Vaga encerrada",
  VAGA_INDISPONIVEL: "Vaga indisponível",
  OFERTA_CRIADA: "Vaga oferecida",
  OFERTA_ENVIADA: "Oferta enviada",
  OFERTA_NAO_ENVIADA: "Oferta não enviada",
  OFERTA_ACEITA: "Oferta aceita",
  OFERTA_RECUSADA: "Oferta recusada",
  OFERTA_EXPIRADA: "Oferta expirada",
  OFERTA_CANCELADA: "Oferta cancelada",
  CONSULTA_ANTECIPADA: "Consulta antecipada",
  PRAZO_SEM_RESPOSTA: "Prazo sem resposta",
  SEM_CONTATO: "Sem contato",
  CONSULTA_LIBERADA: "Consulta liberada",
  PRESENCA_CONFIRMADA_NA_RECEPCAO: "Presença confirmada pela equipe",
};

export function ordinal(n: number): string {
  return `${n}º`;
}

export function percentual(valor: number | null): string {
  return valor === null ? "—" : `${Math.round(valor * 100)}%`;
}
