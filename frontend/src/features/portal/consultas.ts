import type { Consulta, Historico, ResumoDoEncaminhamento, StatusConsulta } from "@/features/assistencial/tipos";
import { horaAtual, hoje } from "@/shared/lib/datas";

export type ConsultaDoPaciente = {
  consulta: Consulta;
  encaminhamento: ResumoDoEncaminhamento | null;
};

const ENCERRADAS: StatusConsulta[] = ["CANCELADO", "COMPARECEU", "FALTOU"];
const CANCELAVEIS: StatusConsulta[] = ["AGENDADO", "AGUARDANDO_RESPOSTA", "CONFIRMADO"];

function agora(): string {
  return `${hoje()}T${horaAtual()}`;
}

export function consultasDoHistorico(historico: Historico): ConsultaDoPaciente[] {
  const doEncaminhamento = historico.ramos.flatMap((ramo) =>
    ramo.consultas.map((consulta) => ({
      consulta,
      encaminhamento: ramo.encaminhamentos.find((e) => e.id === consulta.encaminhamentoId) ?? null,
    })),
  );
  const avulsas = historico.consultasAvulsas.map((consulta) => ({ consulta, encaminhamento: null }));
  return [...doEncaminhamento, ...avulsas];
}

export function proxima(item: ConsultaDoPaciente): boolean {
  if (item.consulta.status === "EM_ANDAMENTO") return true;
  return !ENCERRADAS.includes(item.consulta.status) && item.consulta.inicio.slice(0, 16) >= agora().slice(0, 16);
}

export function separar(itens: ConsultaDoPaciente[]): { proximas: ConsultaDoPaciente[]; anteriores: ConsultaDoPaciente[] } {
  const proximas = itens.filter(proxima).sort((a, b) => a.consulta.inicio.localeCompare(b.consulta.inicio));
  const anteriores = itens.filter((i) => !proxima(i)).sort((a, b) => b.consulta.inicio.localeCompare(a.consulta.inicio));
  return { proximas, anteriores };
}

export function podeCancelar(item: ConsultaDoPaciente): boolean {
  return CANCELAVEIS.includes(item.consulta.status) && proxima(item);
}

export function podeRemarcar(item: ConsultaDoPaciente): boolean {
  return podeCancelar(item) && item.encaminhamento?.modalidade === "ELETIVO" && item.encaminhamento.status === "AGENDADO";
}

export function paraAgendar(historico: Historico): ResumoDoEncaminhamento[] {
  return historico.ramos.flatMap((r) => r.encaminhamentos).filter((e) => e.modalidade === "ELETIVO" && e.status === "AGUARDANDO_AGENDAMENTO");
}

export function linkDeRemarcacao(item: ConsultaDoPaciente): string {
  return `/portal/agendar/${item.consulta.encaminhamentoId}?remarcar=${item.consulta.id}&versao=${item.consulta.versao}`;
}

function horarioDaAgenda(dataHora: string): string {
  return dataHora.slice(0, 19).replace(/[-:]/g, "");
}

export function linkParaAgenda(item: ConsultaDoPaciente, titulo: string): string {
  const { consulta } = item;
  const parametros = new URLSearchParams({
    action: "TEMPLATE",
    text: `${titulo} na ${consulta.unidade}`,
    dates: `${horarioDaAgenda(consulta.inicio)}/${horarioDaAgenda(consulta.fim)}`,
    location: `${consulta.endereco}, ${consulta.unidade}`,
    details: "Leve seu cartão do SUS e um documento com foto.",
  });
  return `https://calendar.google.com/calendar/render?${parametros}`;
}

export function linkComoChegar(item: ConsultaDoPaciente): string {
  return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(`${item.consulta.endereco}, ${item.consulta.unidade}`)}`;
}
