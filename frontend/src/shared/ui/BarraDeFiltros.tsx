import { FilterX } from "lucide-react";
import type { ReactNode } from "react";
import { Botao } from "./Botao";
import estilos from "./BarraDeFiltros.module.css";

type Props = {
  rotulo: string;
  filtrando: boolean;
  aoLimpar: () => void;
  children: ReactNode;
};

export function BarraDeFiltros({ rotulo, filtrando, aoLimpar, children }: Props) {
  return (
    <div role="search" className={estilos.barra} aria-label={rotulo}>
      <div className={estilos.campos}>{children}</div>
      {filtrando && (
        <Botao variante="fantasma" tamanho="pequeno" icone={<FilterX size={16} aria-hidden="true" />} onClick={aoLimpar}>
          Limpar filtros
        </Botao>
      )}
    </div>
  );
}
