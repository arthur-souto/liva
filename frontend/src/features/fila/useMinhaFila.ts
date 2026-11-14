"use client";

import { useConsulta } from "@/shared/hooks/useConsulta";
import { minhaFila } from "./api";
import { useRelogioDoServidor } from "./useRelogioDoServidor";

export function useMinhaFila() {
  const consulta = useConsulta(minhaFila, "minha-fila", 30000);
  const agora = useRelogioDoServidor(consulta.dados?.agora, consulta.atualizadoEm);
  const ofertas = (consulta.dados?.ofertas ?? []).filter((o) => Date.parse(o.expiraEm) > agora);
  return { ...consulta, agora, ofertas };
}
