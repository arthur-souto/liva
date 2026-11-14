import type { TipoAtendimento } from "@/features/agendamentos/tipos";
import type { Pagina } from "@/shared/api/pagina";

export type Sexo = "MASCULINO" | "FEMININO" | "OUTRO" | "NAO_INFORMADO";
export type StatusTriagem = "AGUARDANDO_ATENDIMENTO" | "EM_ANDAMENTO" | "FINALIZADA" | "CANCELADA" | "ENCERRADA";
export type Prioridade = "EMERGENCIA" | "MUITO_URGENTE" | "URGENTE" | "POUCO_URGENTE" | "NAO_URGENTE";
export type Modalidade = "ELETIVO" | "IMEDIATO";
export type StatusEncaminhamento = "PENDENTE" | "AGUARDANDO_AGENDAMENTO" | "AGENDADO" | "EM_ATENDIMENTO" | "CONCLUIDO" | "CANCELADO";
export type StatusConsulta =
  | "AGENDADO"
  | "AGUARDANDO_RESPOSTA"
  | "CONFIRMADO"
  | "CANCELADO"
  | "REAGENDADO"
  | "COMPARECEU"
  | "FALTOU"
  | "EM_ANDAMENTO";
export type NaturezaConsulta = "AVULSA" | "INICIAL" | "RETORNO";

export type Endereco = {
  cep: string | null;
  logradouro: string | null;
  numero: string | null;
  complemento: string | null;
  bairro: string | null;
  cidade: string | null;
  uf: string | null;
};

export type DadosDoPaciente = {
  nomeCompleto: string;
  nomeSocial: string | null;
  cpf: string | null;
  cartaoSus: string | null;
  dataNascimento: string | null;
  sexo: Sexo;
  email: string | null;
  telefone: string;
  endereco: Endereco | null;
  nomeCuidador: string | null;
  telefoneCuidador: string | null;
  contatoEmergenciaNome: string | null;
  contatoEmergenciaTelefone: string | null;
  observacoes: string | null;
  consentimento: boolean;
};

export type Paciente = DadosDoPaciente & {
  id: string;
  versao: number;
  criadoEm: string;
  atualizadoEm: string | null;
};

export type AvaliacaoClinica = {
  queixaPrincipal: string | null;
  sintomas: string | null;
  historicoClinico: string | null;
  antecedentes: string | null;
  observacoes: string | null;
};

export type SinaisVitais = {
  pressaoSistolica: number | null;
  pressaoDiastolica: number | null;
  frequenciaCardiaca: number | null;
  frequenciaRespiratoria: number | null;
  temperatura: number | null;
  saturacao: number | null;
};

export type Triagem = {
  id: string;
  pacienteId: string;
  status: StatusTriagem;
  chegadaEm: string;
  profissionalId: string | null;
  iniciadaEm: string | null;
  finalizadaEm: string | null;
  encerradaEm: string | null;
  canceladaEm: string | null;
  motivoCancelamento: string | null;
  avaliacao: AvaliacaoClinica | null;
  sinaisVitais: SinaisVitais | null;
  prioridade: Prioridade | null;
  justificativaPrioridade: string | null;
  versao: number;
};

export type Encaminhamento = {
  id: string;
  triagemId: string;
  pacienteId: string;
  especialidade: string;
  profissionalDestinoId: string | null;
  modalidade: Modalidade;
  prioridade: Prioridade;
  justificativa: string;
  status: StatusEncaminhamento;
  criadoEm: string;
  recebidoPor: string | null;
  recebidoEm: string | null;
  encerradoEm: string | null;
  desfecho: string | null;
  versao: number;
};

export type Consulta = {
  id: string;
  pacienteId: string;
  triagemId: string | null;
  encaminhamentoId: string | null;
  profissionalId: string | null;
  inicio: string;
  fim: string;
  duracaoMinutos: number;
  natureza: NaturezaConsulta;
  status: StatusConsulta;
  tipo: TipoAtendimento;
  unidade: string;
  endereco: string;
  versao: number;
};

export type Retificacao = { triagemId: string; autorId: string; texto: string; justificativa: string; registradaEm: string };

export type Evolucao = { agendamentoId: string; autorId: string; texto: string; registradaEm: string };

export type Ramo = {
  triagem: Triagem;
  encaminhamentos: Encaminhamento[];
  consultas: Consulta[];
  retificacoes: Retificacao[];
  evolucoes: Evolucao[];
};

export type ItemDaFila = {
  triagemId: string;
  pacienteId: string;
  nomePaciente: string | null;
  chegadaEm: string;
  status: StatusTriagem;
  profissionalId: string | null;
  versao: number;
};

export type ResumoDaTriagem = {
  id: string;
  status: StatusTriagem;
  chegadaEm: string;
  finalizadaEm: string | null;
  prioridade: Prioridade | null;
  queixaPrincipal: string | null;
  profissionalId: string | null;
  sinaisVitais: SinaisVitais | null;
};

export type ResumoDoEncaminhamento = {
  id: string;
  especialidade: string;
  modalidade: Modalidade;
  prioridade: Prioridade;
  status: StatusEncaminhamento;
  criadoEm: string;
  profissionalDestinoId: string | null;
  justificativa: string | null;
  desfecho: string | null;
  versao: number;
};

export type RamoDoHistorico = {
  triagem: ResumoDaTriagem;
  encaminhamentos: ResumoDoEncaminhamento[];
  consultas: Consulta[];
  acessoClinico: boolean;
};

export type Historico = {
  paciente: { id: string; nomeCompleto: string; nomeSocial: string | null; dataNascimento: string | null; telefone: string | null };
  ramos: RamoDoHistorico[];
  consultasAvulsas: Consulta[];
  portalLiberado: boolean;
};

export type HorarioDisponivel = { profissionalId: string; nomeProfissional: string; inicio: string; fim: string };

export type Especialidade = { codigo: string; nome: string; plantao: boolean };

export type Profissional = { id: string; nome: string; especialidade: string | null; unidade: string };

export type EncaminhamentoRecebido = { encaminhamento: Encaminhamento; nomePaciente: string | null };

export type ConsultaDaAgenda = { consulta: Consulta; nomePaciente: string | null; encaminhamento: Encaminhamento | null };

export type NovoEncaminhamento = {
  especialidade: string;
  profissionalDestinoId: string | null;
  modalidade: Modalidade;
  prioridade: Prioridade;
  justificativa: string;
};

export type Parentesco =
  "FILHO" | "CONJUGE" | "PAI_OU_MAE" | "IRMAO" | "NETO" | "OUTRO_FAMILIAR" | "RESPONSAVEL_LEGAL" | "CUIDADOR_PROFISSIONAL" | "AMIGO";

export type PessoaDeConfianca = {
  id: string;
  nome: string;
  cpf: string;
  telefone: string;
  parentesco: Parentesco;
  autorizadoEm: string;
};

export type Atendimento = RamoDoHistorico & { numero: number };

export type Atendimentos = Pagina<Atendimento> & { especialidades: string[]; anos: number[] };

export type FiltrosDeAtendimentos = {
  especialidade?: string;
  andamento?: "ABERTO" | "ENCERRADO";
  ano?: string;
  pagina?: number;
  tamanho?: number;
};

export type ConsultaDoPortal = {
  consulta: Consulta;
  encaminhamento: ResumoDoEncaminhamento | null;
  especialidade: string;
  profissional: string | null;
};

export type ConsultasDoPortal = Pagina<ConsultaDoPortal> & {
  proximas: number;
  anteriores: number;
  especialidades: string[];
  situacoes: StatusConsulta[];
  anos: number[];
};

export type FiltrosDeConsultas = {
  periodo: "PROXIMAS" | "ANTERIORES";
  especialidade?: string;
  situacao?: StatusConsulta;
  ano?: string;
  busca?: string;
  pagina?: number;
  tamanho?: number;
};
