import type { ReactNode } from "react";
import { Cartao } from "./Cartao";
import estilos from "./Secao.module.css";

type Props = {
  titulo: string;
  descricao?: string;
  acoes?: ReactNode;
  children: ReactNode;
};

export function Secao({ titulo, descricao, acoes, children }: Props) {
  return (
    <Cartao como="section" espacamento="amplo" className={estilos.secao}>
      <header className={estilos.cabecalho}>
        <div>
          <h2 className={estilos.titulo}>{titulo}</h2>
          {descricao && <p className={estilos.descricao}>{descricao}</p>}
        </div>
        {acoes && <div className={estilos.acoes}>{acoes}</div>}
      </header>
      {children}
    </Cartao>
  );
}
