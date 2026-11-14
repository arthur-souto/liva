"use client";

import { useAgora } from "@/shared/hooks/useAgora";

export function useRelogioDoServidor(agoraNoServidor: string | undefined, recebidoEm: number | null): number {
  const agora = useAgora();
  if (!agoraNoServidor || !recebidoEm) return agora;
  return agora + (Date.parse(agoraNoServidor) - recebidoEm);
}

export function tempoRestante(ate: string, agora: number): number {
  return Math.max(0, Date.parse(ate) - agora);
}

export function descreverRestante(ms: number): string {
  const segundos = Math.floor(ms / 1000);
  const horas = Math.floor(segundos / 3600);
  const minutos = Math.floor((segundos % 3600) / 60);
  const resto = segundos % 60;
  if (horas > 0) return `${horas}h ${String(minutos).padStart(2, "0")}min`;
  return `${minutos}:${String(resto).padStart(2, "0")}`;
}
