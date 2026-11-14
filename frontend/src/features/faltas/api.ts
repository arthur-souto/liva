import { requisitar } from "@/shared/api/cliente";
import type { MotivoDeAusencia, PainelDeFaltas, ResultadoDaLigacao } from "./tipos";

export function painelDeFaltas(semanas: number): Promise<PainelDeFaltas> {
  return requisitar<PainelDeFaltas>("/faltas", { parametros: { semanas: String(semanas) } });
}

export function registrarMotivo(consultaId: string, motivo: MotivoDeAusencia): Promise<void> {
  return requisitar<void>(`/consultas/${encodeURIComponent(consultaId)}/motivo-de-ausencia`, { metodo: "POST", corpo: { motivo } });
}

export function registrarLigacao(consultaId: string, resultado: ResultadoDaLigacao): Promise<void> {
  return requisitar<void>(`/consultas/${encodeURIComponent(consultaId)}/ligacao`, { metodo: "POST", corpo: { resultado } });
}
