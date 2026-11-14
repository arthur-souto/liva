import type { ReactNode } from "react";
import estilos from "./EstadoVazio.module.css";

type Props = {
  icone: ReactNode;
  titulo: string;
  descricao?: string;
  acao?: ReactNode;
};

export function EstadoVazio({ icone, titulo, descricao, acao }: Props) {
  return (
    <div className={estilos.vazio}>
      <span className={estilos.icone}>{icone}</span>
      <p className={estilos.titulo}>{titulo}</p>
      {descricao && <p className={estilos.descricao}>{descricao}</p>}
      {acao}
    </div>
  );
}
