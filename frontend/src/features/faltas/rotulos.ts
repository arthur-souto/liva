import type { Tom } from "@/shared/ui/Etiqueta";
import type { MotivoDeAusencia, NivelDeRisco, ResultadoDaLigacao } from "./tipos";

export const MOTIVOS: Record<MotivoDeAusencia, { rotulo: string; acao: string }> = {
  ESQUECI: { rotulo: "Esqueceu", acao: "Reforçar lembretes e pedir confirmação mais perto da data." },
  TRANSPORTE: { rotulo: "Transporte ou distância", acao: "Oferecer horários e unidades mais próximas ou apoio de transporte." },
  TRABALHO: { rotulo: "Trabalho, estudo ou cuidado", acao: "Oferecer horários no início da manhã ou no fim da tarde." },
  SAUDE: { rotulo: "Doente no dia", acao: "Facilitar a remarcação rápida pelo WhatsApp." },
  NAO_PRECISA: { rotulo: "Não precisa mais", acao: "Liberar o encaminhamento e devolver a vaga para a fila." },
  OUTRO: { rotulo: "Outro motivo", acao: "Ligar para entender o caso." },
};

export const ORDEM_DOS_MOTIVOS: MotivoDeAusencia[] = ["ESQUECI", "TRANSPORTE", "TRABALHO", "SAUDE", "NAO_PRECISA", "OUTRO"];

export const NIVEIS: Record<NivelDeRisco, { rotulo: string; tom: Tom }> = {
  ALTO: { rotulo: "Risco alto", tom: "perigo" },
  MEDIO: { rotulo: "Risco médio", tom: "alerta" },
  BAIXO: { rotulo: "Risco baixo", tom: "sucesso" },
};

export const LIGACOES: Record<ResultadoDaLigacao, { rotulo: string; registrado: string; tom: Tom }> = {
  CONFIRMOU: { rotulo: "Confirmou", registrado: "Confirmou por telefone", tom: "sucesso" },
  VAI_REMARCAR: { rotulo: "Vai remarcar", registrado: "Vai remarcar", tom: "alerta" },
  NAO_ATENDEU: { rotulo: "Não atendeu", registrado: "Não atendeu a ligação", tom: "perigo" },
};

export const ORDEM_DAS_LIGACOES: ResultadoDaLigacao[] = ["CONFIRMOU", "VAI_REMARCAR", "NAO_ATENDEU"];
