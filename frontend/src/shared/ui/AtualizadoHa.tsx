"use client";

import { RefreshCw } from "lucide-react";
import { useAgora } from "@/shared/hooks/useAgora";
import { classes } from "@/shared/lib/classes";
import estilos from "./AtualizadoHa.module.css";

type Props = {
  atualizadoEm: number | null;
  carregando: boolean;
  aoAtualizar: () => void;
};

function descrever(segundos: number): string {
  if (segundos < 5) return "agora";
  if (segundos < 60) return `há ${segundos}s`;
  return `há ${Math.floor(segundos / 60)} min`;
}

export function AtualizadoHa({ atualizadoEm, carregando, aoAtualizar }: Props) {
  const agora = useAgora();
  return (
    <button type="button" className={estilos.botao} onClick={aoAtualizar} aria-label="Atualizar agora">
      <RefreshCw size={14} aria-hidden="true" className={classes(carregando && estilos.girando)} />
      {atualizadoEm ? `Atualizado ${descrever(Math.max(0, Math.round((agora - atualizadoEm) / 1000)))}` : "Atualizando"}
    </button>
  );
}
