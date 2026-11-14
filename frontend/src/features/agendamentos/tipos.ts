export type StatusAgendamento =
  | "AGENDADO"
  | "AGUARDANDO_RESPOSTA"
  | "CONFIRMADO"
  | "CANCELADO"
  | "REAGENDADO"
  | "COMPARECEU"
  | "FALTOU";

export type TipoAtendimento = "CONSULTA" | "TRIAGEM" | "EXAME";

export type TipoLembrete = "CONFIRMACAO_AGENDAMENTO" | "PEDIDO_CONFIRMACAO" | "LEMBRETE_VESPERA";

export type Agendamento = {
  id: string;
  pacienteId: string;
  dataHora: string;
  tipo: TipoAtendimento;
  unidade: string;
  endereco: string;
  preparo: string | null;
  status: StatusAgendamento;
};

export type AgendamentoDaEquipe = {
  id: string;
  dataHora: string;
  tipo: TipoAtendimento;
  unidade: string;
  status: StatusAgendamento;
  pacienteId: string;
  nomePaciente: string | null;
  telefone: string | null;
  nomeCuidador: string | null;
  telefoneCuidador: string | null;
};

export type ListaDaEquipe = {
  inicio: string;
  fim: string;
  total: number;
  porStatus: Record<StatusAgendamento, number>;
  agendamentos: AgendamentoDaEquipe[];
};

export type RegistroMensagem = {
  agendamentoId: string | null;
  destino: { e164: string };
  tipo: TipoLembrete | null;
  texto: string;
  sucesso: boolean;
  idExterno: string | null;
  erro: string | null;
  enviadoEm: string;
};

export type ResultadoDisparo = {
  envios: RegistroMensagem[];
  motivoNaoEnvio: string | null;
};

export type NovoAgendamento = {
  nomePaciente: string;
  cpf?: string;
  telefone: string;
  nomeCuidador?: string;
  telefoneCuidador?: string;
  dataHora: string;
  tipo: TipoAtendimento;
  unidade: string;
  endereco: string;
  preparo?: string;
};

export type AgendamentoCriado = {
  pacienteId: string;
  pacienteNovo: boolean;
  agendamento: Agendamento;
  confirmacao: ResultadoDisparo;
  pedidoConfirmacao: ResultadoDisparo | null;
};

export type RespostaPaciente = "CONFIRMAR" | "CANCELAR" | "REAGENDAR";

export type MensagemDoHistorico = {
  direcao: "ENVIADA" | "RECEBIDA";
  telefone: string;
  texto: string;
  tipo: TipoLembrete | null;
  resposta: RespostaPaciente | null;
  sucesso: boolean;
  erro: string | null;
  em: string;
};
