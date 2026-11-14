import type { ReactNode } from "react";
import { classes } from "@/shared/lib/classes";
import estilos from "./Segmentado.module.css";

type Opcao<T extends string> = {
  valor: T;
  rotulo: string;
  icone?: ReactNode;
};

type Props<T extends string> = {
  rotulo: string;
  opcoes: Opcao<T>[];
  valor: T;
  aoMudar: (valor: T) => void;
};

export function Segmentado<T extends string>({ rotulo, opcoes, valor, aoMudar }: Props<T>) {
  return (
    <div className={estilos.grupo} role="radiogroup" aria-label={rotulo}>
      {opcoes.map((opcao) => (
        <button
          key={opcao.valor}
          type="button"
          role="radio"
          aria-checked={opcao.valor === valor}
          className={classes(estilos.opcao, opcao.valor === valor && estilos.ativa)}
          onClick={() => aoMudar(opcao.valor)}
        >
          {opcao.icone}
          {opcao.rotulo}
        </button>
      ))}
    </div>
  );
}
