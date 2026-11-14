import type { SemanaDeComparecimento } from "./tipos";

export function taxaDeFaltas(s: Pick<SemanaDeComparecimento, "compareceram" | "faltaram">): number | null {
  const realizadas = s.compareceram + s.faltaram;
  return realizadas === 0 ? null : s.faltaram / realizadas;
}

export function somar(semanas: SemanaDeComparecimento[]): SemanaDeComparecimento | null {
  if (semanas.length === 0) return null;
  return semanas.reduce((total, s) => ({
    inicio: total.inicio,
    compareceram: total.compareceram + s.compareceram,
    faltaram: total.faltaram + s.faltaram,
    cancelaramAntes: total.cancelaramAntes + s.cancelaramAntes,
    reaproveitadas: total.reaproveitadas + s.reaproveitadas,
  }));
}
