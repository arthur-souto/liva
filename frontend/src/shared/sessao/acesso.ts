import type { Papel } from "./sessao";

const EQUIPE_ADMINISTRATIVA = ["/ajuda", "/recepcao", "/filas", "/agendamentos", "/agendar", "/calendario", "/conversas", "/avisos"];

const AREAS: Record<Papel, { inicio: string; prefixos: string[] }> = {
  ADMINISTRADOR: { inicio: "/", prefixos: EQUIPE_ADMINISTRATIVA },
  RECEPCAO: { inicio: "/", prefixos: EQUIPE_ADMINISTRATIVA },
  CLINICO_GERAL: { inicio: "/triagem", prefixos: ["/triagem", "/ajuda"] },
  ESPECIALISTA: { inicio: "/especialista", prefixos: ["/especialista", "/filas", "/ajuda"] },
  PACIENTE: { inicio: "/portal", prefixos: ["/portal"] },
  CUIDADOR: { inicio: "/portal", prefixos: ["/portal"] },
};

function dentro(caminho: string, prefixo: string): boolean {
  return caminho === prefixo || caminho.startsWith(`${prefixo}/`);
}

export function papelValido(valor: unknown): valor is Papel {
  return typeof valor === "string" && valor in AREAS;
}

export function inicioDoPapel(papel: Papel): string {
  return AREAS[papel].inicio;
}

export function rotaPermitida(papel: Papel, caminho: string): boolean {
  const area = AREAS[papel];
  return caminho === area.inicio || area.prefixos.some((prefixo) => dentro(caminho, prefixo));
}
