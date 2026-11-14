import { classes } from "@/shared/lib/classes";
import estilos from "./Logo.module.css";

type Props = {
  tamanho?: "pequeno" | "medio" | "grande";
  invertido?: boolean;
};

export function Logo({ tamanho = "medio", invertido = false }: Props) {
  return (
    <span className={classes(estilos.logo, estilos[tamanho], invertido && estilos.invertido)} aria-label="Liva">
      <span aria-hidden="true">
        l<span className={estilos.i}>ı</span>va
      </span>
    </span>
  );
}
