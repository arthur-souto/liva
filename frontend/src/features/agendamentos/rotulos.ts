import type { Tom } from "@/shared/ui/Etiqueta";
import type { RespostaPaciente, StatusAgendamento, TipoAtendimento, TipoLembrete } from "./tipos";

export const STATUS: Record<StatusAgendamento, { rotulo: string; tom: Tom }> = {
  AGENDADO: { rotulo: "Agendado", tom: "neutro" },
  AGUARDANDO_RESPOSTA: { rotulo: "Aguardando resposta", tom: "alerta" },
  CONFIRMADO: { rotulo: "Confirmado", tom: "sucesso" },
  CANCELADO: { rotulo: "Cancelado", tom: "perigo" },
  REAGENDADO: { rotulo: "Quer outro horário", tom: "destaque" },
  COMPARECEU: { rotulo: "Compareceu", tom: "info" },
  FALTOU: { rotulo: "Faltou", tom: "perigo" },
  EM_ANDAMENTO: { rotulo: "Em atendimento", tom: "marca" },
};

export const TIPOS_DE_ATENDIMENTO: Record<TipoAtendimento, string> = {
  CONSULTA: "Consulta",
  TRIAGEM: "Triagem",
  EXAME: "Exame",
};

export const LEMBRETES: Record<TipoLembrete, { rotulo: string; descricao: string }> = {
  CONFIRMACAO_AGENDAMENTO: {
    rotulo: "Confirmação do agendamento",
    descricao: "Data, horário, endereço e documentos.",
  },
  PEDIDO_CONFIRMACAO: {
    rotulo: "Pedido de confirmação",
    descricao: "Pergunta \"Você consegue ir?\" com as opções 1, 2 e 3. Enviado dias antes, conforme a configuração da unidade.",
  },
  AVISO_FINAL_CONFIRMACAO: {
    rotulo: "Aviso final de confirmação",
    descricao: "Último aviso antes do prazo de confirmação, para quem ainda não respondeu.",
  },
  LEMBRETE_VESPERA: {
    rotulo: "Lembrete da véspera",
    descricao: "Data, horário, endereço e preparo. Enviado na véspera, conforme a configuração da unidade.",
  },
};

export const ACAO_DA_EQUIPE: Partial<Record<StatusAgendamento, string>> = {
  CANCELADO: "Vaga liberada",
  REAGENDADO: "Ligar para remarcar",
};

export const RESPOSTA_DO_PACIENTE: Record<StatusAgendamento, { texto: string; tom: Tom }> = {
  AGENDADO: { texto: "Pedido de confirmação ainda não enviado", tom: "neutro" },
  AGUARDANDO_RESPOSTA: { texto: "Ainda não respondeu", tom: "alerta" },
  CONFIRMADO: { texto: "Respondeu: vou comparecer", tom: "sucesso" },
  CANCELADO: { texto: "Respondeu: não vou", tom: "perigo" },
  REAGENDADO: { texto: "Respondeu: quero outro horário", tom: "destaque" },
  COMPARECEU: { texto: "Compareceu ao atendimento", tom: "info" },
  FALTOU: { texto: "Não compareceu", tom: "perigo" },
  EM_ANDAMENTO: { texto: "Em atendimento agora", tom: "info" },
};

export const INTERPRETACAO: Record<RespostaPaciente, string> = {
  CONFIRMAR: "Entendido como: vai comparecer",
  CANCELAR: "Entendido como: não vai",
  REAGENDAR: "Entendido como: quer outro horário",
};
