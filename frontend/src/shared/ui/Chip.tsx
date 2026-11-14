import type { ButtonHTMLAttributes, ReactNode } from "react";
import { classes } from "@/shared/lib/classes";
import estilos from "./Chip.module.css";

type Props = Omit<ButtonHTMLAttributes<HTMLButtonElement>, "children"> & {
  selecionado?: boolean;
  variante?: "marca" | "realce";
  children: ReactNode;
};

export function Chip({ selecionado = false, variante = "marca", className, children, ...resto }: Props) {
  return (
    <button
      type="button"
      aria-pressed={selecionado}
      {...resto}
      className={classes(estilos.chip, selecionado && estilos[variante], className)}
    >
      {children}
    </button>
  );
}
