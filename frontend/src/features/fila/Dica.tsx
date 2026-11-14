import { type ReactNode, useId } from "react";
import { classes } from "@/shared/lib/classes";
import estilos from "./Dica.module.css";

type Props = {
  texto: ReactNode;
  children: ReactNode;
  className?: string;
  posicao?: "acima" | "abaixo";
};

export function Dica({ texto, children, className, posicao = "acima" }: Props) {
  const id = useId();
  return (
    <span className={classes(estilos.alvo, className)} tabIndex={0} aria-describedby={id}>
      {children}
      <span id={id} role="tooltip" className={classes(estilos.balao, estilos[posicao])}>
        {texto}
      </span>
    </span>
  );
}
