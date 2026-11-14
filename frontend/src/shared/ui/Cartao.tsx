import type { HTMLAttributes, ReactNode } from "react";
import { classes } from "@/shared/lib/classes";
import estilos from "./Cartao.module.css";

type Props = HTMLAttributes<HTMLElement> & {
  variante?: "padrao" | "suave" | "destaque";
  espacamento?: "compacto" | "normal" | "amplo";
  como?: "div" | "section" | "article" | "li";
  children: ReactNode;
};

export function Cartao({ variante = "padrao", espacamento = "normal", como: Elemento = "div", className, children, ...resto }: Props) {
  return (
    <Elemento {...resto} className={classes(estilos.cartao, estilos[variante], estilos[espacamento], className)}>
      {children}
    </Elemento>
  );
}
