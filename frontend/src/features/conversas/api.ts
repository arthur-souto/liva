import { requisitar } from "@/shared/api/cliente";
import type { StatusAgendamento } from "@/features/agendamentos/tipos";

export type ResultadoResposta = {
  aplicada: boolean;
  agendamentoId: string | null;
  status: StatusAgendamento | null;
  retornoEnviado: boolean;
  motivo: string | null;
};

export type ResultadoEnvio = {
  sucesso: boolean;
  idExterno: string | null;
  erro: string | null;
};

export function simularResposta(telefone: string, texto: string): Promise<ResultadoResposta> {
  return requisitar<ResultadoResposta>("/teste/atendimentos/respostas", { metodo: "POST", corpo: { telefone, texto } });
}

export function enviarMensagem(telefone: string, texto: string): Promise<ResultadoEnvio> {
  return requisitar<ResultadoEnvio>("/mensagens", { metodo: "POST", corpo: { telefone, texto } });
}
