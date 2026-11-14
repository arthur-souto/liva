import type { PosicaoNaFila } from "./tipos";

const DIA = 24 * 60 * 60 * 1000;
const FUSO_LOCAL = "-03:00";

export type Estimativa = {
  aFrente: number;
  maisUrgentes: number;
  mesmoNivel: number;
  atras: number;
  vagasPorSemana: number | null;
  diasNaFila: number;
  diasRestantes: number | null;
  previsao: number | null;
  consulta: number | null;
  progresso: number | null;
};

export function instanteLocal(dataHora: string): number {
  return Date.parse(dataHora.length > 16 ? `${dataHora.slice(0, 19)}${FUSO_LOCAL}` : `${dataHora}:00${FUSO_LOCAL}`);
}

export function estimar(fila: PosicaoNaFila, agora: number): Estimativa {
  const aFrente = Math.max(0, (fila.posicao ?? 1) - 1);
  const maisUrgentes = Math.min(aFrente, fila.aFrenteMaisUrgentes ?? 0);
  const atras = Math.max(0, (fila.total ?? 0) - (fila.posicao ?? 0));
  const ritmo = fila.vagasPorSemana && fila.vagasPorSemana > 0 ? fila.vagasPorSemana : null;
  const diasNaFila = Math.max(0, (agora - Date.parse(fila.entrouEm)) / DIA);
  const diasRestantes = ritmo === null ? null : ((aFrente + 1) / ritmo) * 7;
  return {
    aFrente,
    maisUrgentes,
    mesmoNivel: aFrente - maisUrgentes,
    atras,
    vagasPorSemana: ritmo,
    diasNaFila,
    diasRestantes,
    previsao: diasRestantes === null ? null : agora + diasRestantes * DIA,
    consulta: fila.consultaAtual ? instanteLocal(fila.consultaAtual) : null,
    progresso: diasRestantes === null ? null : diasNaFila / (diasNaFila + diasRestantes || 1),
  };
}

export function descreverDias(dias: number): string {
  if (dias < 1) return "menos de 1 dia";
  if (dias < 14) return `${Math.round(dias)} ${Math.round(dias) === 1 ? "dia" : "dias"}`;
  if (dias < 60) return `${Math.round(dias / 7)} semanas`;
  const meses = Math.round(dias / 30);
  return `${meses} ${meses === 1 ? "mês" : "meses"}`;
}

export function resumirDias(dias: number): { valor: string; unidade: string } {
  if (dias < 14) return { valor: String(Math.max(1, Math.round(dias))), unidade: Math.round(dias) <= 1 ? "dia" : "dias" };
  if (dias < 60) return { valor: String(Math.round(dias / 7)), unidade: "semanas" };
  const meses = Math.round(dias / 30);
  return { valor: String(meses), unidade: meses === 1 ? "mês" : "meses" };
}

export function relativo(instante: number, agora: number): string {
  const dias = (instante - agora) / DIA;
  if (Math.abs(dias) < 1) return "hoje";
  return dias > 0 ? `em ${descreverDias(dias)}` : `há ${descreverDias(-dias)}`;
}

export function dataCurta(instante: number): string {
  return new Intl.DateTimeFormat("pt-BR", { timeZone: "America/Sao_Paulo", day: "2-digit", month: "short" })
    .format(new Date(instante))
    .replace(".", "");
}
