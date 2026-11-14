const FUSO = "America/Sao_Paulo";
const DIAS_CURTOS = ["dom", "seg", "ter", "qua", "qui", "sex", "sáb"];
const DIAS_LONGOS = ["Domingo", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado"];
const MESES = [
  "janeiro",
  "fevereiro",
  "março",
  "abril",
  "maio",
  "junho",
  "julho",
  "agosto",
  "setembro",
  "outubro",
  "novembro",
  "dezembro",
];

const formatoData = new Intl.DateTimeFormat("en-CA", { timeZone: FUSO, year: "numeric", month: "2-digit", day: "2-digit" });
const formatoHora = new Intl.DateTimeFormat("en-GB", { timeZone: FUSO, hour: "2-digit", minute: "2-digit", hourCycle: "h23" });
const formatoInstante = new Intl.DateTimeFormat("pt-BR", {
  timeZone: FUSO,
  day: "2-digit",
  month: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
});

function emUtc(data: string): Date {
  const [ano, mes, dia] = data.slice(0, 10).split("-").map(Number);
  return new Date(Date.UTC(ano, mes - 1, dia));
}

function paraIso(data: Date): string {
  return data.toISOString().slice(0, 10);
}

export function hoje(): string {
  return formatoData.format(new Date());
}

export function horaAtual(): string {
  return formatoHora.format(new Date());
}

export function adicionarDias(data: string, dias: number): string {
  const resultado = emUtc(data);
  resultado.setUTCDate(resultado.getUTCDate() + dias);
  return paraIso(resultado);
}

export function dataDe(dataHora: string): string {
  return dataHora.slice(0, 10);
}

export function horaDe(dataHora: string): string {
  return dataHora.slice(11, 16);
}

export function montarDataHora(data: string, hora: string): string {
  return `${data}T${hora}:00`;
}

export function diaDoMes(data: string): number {
  return emUtc(data).getUTCDate();
}

export function nomeDiaCurto(data: string): string {
  return DIAS_CURTOS[emUtc(data).getUTCDay()];
}

export function nomeMes(mes: number): string {
  return MESES[mes - 1];
}

export function formatarData(data: string): string {
  const [ano, mes, dia] = data.slice(0, 10).split("-");
  return `${dia}/${mes}/${ano}`;
}

export function formatarDataCurta(data: string): string {
  const d = emUtc(data);
  return `${d.getUTCDate()} ${MESES[d.getUTCMonth()].slice(0, 3)}`;
}

export function formatarDiaLongo(data: string): string {
  const d = emUtc(data);
  return `${DIAS_LONGOS[d.getUTCDay()]}, ${d.getUTCDate()} de ${MESES[d.getUTCMonth()]}`;
}

export function formatarDataHora(dataHora: string): string {
  return `${nomeDiaCurto(dataHora)}, ${formatarData(dataHora).slice(0, 5)} · ${horaDe(dataHora)}`;
}

export function formatarInstante(instante: string): string {
  return formatoInstante.format(new Date(instante)).replace(",", " ·");
}

export function rotuloDoDia(data: string): string {
  const referencia = hoje();
  if (data === referencia) return "Hoje";
  if (data === adicionarDias(referencia, 1)) return "Amanhã";
  if (data === adicionarDias(referencia, -1)) return "Ontem";
  return formatarDiaLongo(data);
}

export function inicioDoMes(ano: number, mes: number): string {
  return `${ano}-${String(mes).padStart(2, "0")}-01`;
}

export function fimDoMes(ano: number, mes: number): string {
  return paraIso(new Date(Date.UTC(ano, mes, 0)));
}

export function gradeDoMes(ano: number, mes: number): (string | null)[] {
  const primeiro = inicioDoMes(ano, mes);
  const vazios = (emUtc(primeiro).getUTCDay() + 6) % 7;
  const total = diaDoMes(fimDoMes(ano, mes));
  return [
    ...Array.from({ length: vazios }, () => null),
    ...Array.from({ length: total }, (_, i) => adicionarDias(primeiro, i)),
  ];
}
