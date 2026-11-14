import type { StatusAgendamento } from "@/features/agendamentos/tipos";
import type { NaturezaConsulta, Prioridade } from "@/features/assistencial/tipos";

export type StatusEntrada = "AGUARDANDO" | "PAUSADA" | "ATENDIDA" | "ENCERRADA";
export type StatusOferta = "PENDENTE" | "ENVIADA" | "ACEITA" | "RECUSADA" | "EXPIRADA" | "CANCELADA";
export type StatusVaga = "ABERTA" | "PREENCHIDA" | "ENCERRADA" | "INDISPONIVEL";
export type OrigemDaVaga = "CANCELAMENTO" | "SEM_CONFIRMACAO" | "ANTECIPACAO";
export type CanalDeResposta = "PORTAL" | "WHATSAPP";
export type DecisaoSemResposta = "MANTIDA_SEM_RESPOSTA" | "SEM_CONTATO" | "LIBERADA" | "CONFIRMADA_NA_RECEPCAO";
export type EventoDaFila =
  | "ENTROU_NA_FILA"
  | "SAIU_DA_FILA"
  | "PAUSADA"
  | "RETOMADA"
  | "VAGA_LIBERADA"
  | "VAGA_ENCERRADA"
  | "VAGA_INDISPONIVEL"
  | "OFERTA_CRIADA"
  | "OFERTA_ENVIADA"
  | "OFERTA_NAO_ENVIADA"
  | "OFERTA_ACEITA"
  | "OFERTA_RECUSADA"
  | "OFERTA_EXPIRADA"
  | "OFERTA_CANCELADA"
  | "CONSULTA_ANTECIPADA"
  | "PRAZO_SEM_RESPOSTA"
  | "SEM_CONTATO"
  | "CONSULTA_LIBERADA"
  | "PRESENCA_CONFIRMADA_NA_RECEPCAO";

export type PosicaoNaFila = {
  entradaId: string;
  encaminhamentoId: string;
  triagemId: string;
  triagemNumero: number;
  triagemEm: string;
  especialidade: string;
  nomeEspecialidade: string;
  profissional: string | null;
  prioridade: Prioridade;
  natureza: NaturezaConsulta;
  status: StatusEntrada;
  posicao: number | null;
  total: number | null;
  entrouEm: string;
  consultaAtual: string | null;
  motivo: string | null;
  atualizadaEm: string;
  aFrenteMaisUrgentes: number | null;
  vagasPorSemana: number | null;
};

export type OfertaParaPaciente = {
  id: string;
  entradaId: string;
  nomeEspecialidade: string;
  profissional: string | null;
  unidade: string;
  endereco: string;
  inicio: string;
  fim: string;
  expiraEm: string;
  antecipaDe: string | null;
  status: StatusOferta;
};

export type MinhaFila = { agora: string; filas: PosicaoNaFila[]; ofertas: OfertaParaPaciente[] };

export type FiltroDaFila = { especialidade: string | null; profissionalId: string | null; unidade: string | null };

export type Indicadores = {
  aguardando: number;
  pausadas: number;
  vagasAbertas: number;
  ofertasAtivas: number;
  aguardandoConfirmacao: number;
  semResposta: number;
  vagasReaproveitadas: number;
  ofertasRecusadas: number;
  ofertasExpiradas: number;
  taxaDeConfirmacao: number | null;
  taxaDeAbsenteismo: number | null;
  desde: string;
};

export type PacienteNaFila = {
  pacienteId: string;
  nomePaciente: string;
  posicao: PosicaoNaFila;
  ofertaAtiva: OfertaParaPaciente | null;
};

export type VagaNoPainel = {
  id: string;
  nomeEspecialidade: string;
  profissional: string | null;
  unidade: string;
  inicio: string;
  duracaoMinutos: number;
  origem: OrigemDaVaga;
  status: StatusVaga;
  motivo: string | null;
  ofertas: number;
  liberadaEm: string;
};

export type OfertaNoPainel = {
  id: string;
  entradaId: string;
  pacienteId: string;
  paciente: string;
  nomeEspecialidade: string;
  profissional: string | null;
  inicio: string;
  status: StatusOferta;
  criadaEm: string;
  expiraEm: string;
  enviadaEm: string | null;
  respondidaEm: string | null;
  canal: CanalDeResposta | null;
  motivo: string | null;
};

export type ConsultaSemConfirmacao = {
  consultaId: string;
  pacienteId: string;
  paciente: string;
  nomeEspecialidade: string;
  profissional: string | null;
  unidade: string;
  dataHora: string;
  confirmarAte: string;
  status: StatusAgendamento;
  decisao: DecisaoSemResposta | null;
  pedidoEnviado: boolean;
  pedidoEntregue: boolean;
};

export type MovimentacaoDaFila = {
  id: number;
  evento: EventoDaFila;
  detalhe: string | null;
  entradaId: string | null;
  ofertaId: string | null;
  vagaId: string | null;
  agendamentoId: string | null;
  pacienteId: string | null;
  paciente: string | null;
  papel: string | null;
  registradaEm: string;
};

export type PainelDaFila = {
  agora: string;
  filtro: FiltroDaFila;
  podeIntervir: boolean;
  indicadores: Indicadores;
  itens: PacienteNaFila[];
  pagina: number;
  tamanho: number;
  total: number;
  totalPaginas: number;
  vagas: VagaNoPainel[];
  ofertas: OfertaNoPainel[];
  semConfirmacao: ConsultaSemConfirmacao[];
  movimentacoes: MovimentacaoDaFila[];
};

export type DetalheDaEntrada = {
  pacienteId: string;
  nomePaciente: string;
  posicao: PosicaoNaFila;
  ofertas: OfertaNoPainel[];
  movimentacoes: MovimentacaoDaFila[];
};

export type ResumoDoCiclo = { entradas: number; vagas: number; ofertas: number; expiradas: number; avisos: number; prazos: number };

export type PedidoDoPainel = { especialidade?: string; profissional?: string; unidade?: string; pagina: number; tamanho: number };
