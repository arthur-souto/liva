import type { StatusAgendamento } from "@/features/agendamentos/tipos";

export type MotivoDeAusencia = "ESQUECI" | "TRANSPORTE" | "TRABALHO" | "SAUDE" | "NAO_PRECISA" | "OUTRO";

export type NivelDeRisco = "BAIXO" | "MEDIO" | "ALTO";

export type ResultadoDaLigacao = "CONFIRMOU" | "VAI_REMARCAR" | "NAO_ATENDEU";

export type LigacaoRegistrada = {
  id: string;
  agendamentoId: string;
  resultado: ResultadoDaLigacao;
  usuarioId: string | null;
  registradaEm: string;
};

export type SemanaDeComparecimento = {
  inicio: string;
  compareceram: number;
  faltaram: number;
  cancelaramAntes: number;
  reaproveitadas: number;
};

export type MotivoContado = { motivo: MotivoDeAusencia; faltas: number; cancelamentos: number };

export type FaltasPorEspecialidade = { codigo: string; nome: string; compareceram: number; faltaram: number };

export type ConsultaEmRisco = {
  agendamentoId: string;
  pacienteId: string;
  nomePaciente: string;
  telefone: string;
  dataHora: string;
  especialidade: string | null;
  profissional: string | null;
  unidade: string;
  status: StatusAgendamento;
  risco: { pontos: number; nivel: NivelDeRisco; fatores: string[] };
  ultimaLigacao: LigacaoRegistrada | null;
};

export type PainelDeFaltas = {
  agora: string;
  semanas: SemanaDeComparecimento[];
  motivos: MotivoContado[];
  especialidades: FaltasPorEspecialidade[];
  emRisco: ConsultaEmRisco[];
  perguntasFeitas: number;
  perguntasRespondidas: number;
};
