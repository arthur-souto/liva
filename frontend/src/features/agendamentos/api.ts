import { requisitar } from "@/shared/api/cliente";
import type {
  Agendamento,
  AgendamentoCriado,
  ListaDaEquipe,
  MensagemDoHistorico,
  NovoAgendamento,
  ResultadoDisparo,
  StatusAgendamento,
  TipoLembrete,
} from "./tipos";

type Filtro = {
  inicio: string;
  fim: string;
  status?: StatusAgendamento[];
  busca?: string;
  pagina?: number;
  tamanho?: number;
};

export function listarAgendamentos({ inicio, fim, status, busca, pagina, tamanho }: Filtro): Promise<ListaDaEquipe> {
  return requisitar<ListaDaEquipe>("/agendamentos", {
    parametros: {
      inicio,
      fim,
      status: status?.length ? status : undefined,
      busca: busca || undefined,
      pagina: pagina === undefined ? undefined : String(pagina),
      tamanho: tamanho === undefined ? undefined : String(tamanho),
    },
  });
}

export function buscarAgendamento(id: string): Promise<Agendamento> {
  return requisitar<Agendamento>(`/agendamentos/${encodeURIComponent(id)}`);
}

export function agendar(novo: NovoAgendamento): Promise<AgendamentoCriado> {
  return requisitar<AgendamentoCriado>("/agendamentos", { metodo: "POST", corpo: novo });
}

export function dispararLembrete(id: string, tipo: TipoLembrete, forcar: boolean): Promise<ResultadoDisparo> {
  return requisitar<ResultadoDisparo>(`/agendamentos/${encodeURIComponent(id)}/disparos`, {
    metodo: "POST",
    parametros: { tipo, forcar },
  });
}

export function executarRotinaDiaria(data?: string): Promise<{ mensagensEnviadas: number }> {
  return requisitar<{ mensagensEnviadas: number }>("/disparos/executar", { metodo: "POST", parametros: { data } });
}

export function historicoDeMensagens(id: string): Promise<MensagemDoHistorico[]> {
  return requisitar<MensagemDoHistorico[]>(`/agendamentos/${encodeURIComponent(id)}/mensagens`);
}
