import type { ReactNode } from "react";
import { classes } from "@/shared/lib/classes";
import estilos from "./Etiqueta.module.css";

export type Tom = "sucesso" | "alerta" | "perigo" | "info" | "neutro" | "marca" | "destaque";

type Props = {
  tom?: Tom;
  icone?: ReactNode;
  children: ReactNode;
};

export function Etiqueta({ tom = "neutro", icone, children }: Props) {
  return (
    <span className={classes(estilos.etiqueta, estilos[tom])}>
      {icone}
      {children}
    </span>
  );
}
