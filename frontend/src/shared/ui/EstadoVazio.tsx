import type { ReactNode } from "react";
import { PessoaComCaixaVazia } from "./Ilustracoes";
import estilos from "./EstadoVazio.module.css";

type Props = {
  icone: ReactNode;
  titulo: string;
  descricao?: string;
  acao?: ReactNode;
  ilustracao?: ReactNode;
};

export function EstadoVazio({ icone, titulo, descricao, acao, ilustracao }: Props) {
  return (
    <div className={estilos.vazio}>
      <div className={estilos.figura}>
        {ilustracao ?? <PessoaComCaixaVazia />}
        <span className={estilos.icone}>{icone}</span>
      </div>
      <p className={estilos.titulo}>{titulo}</p>
      {descricao && <p className={estilos.descricao}>{descricao}</p>}
      {acao}
    </div>
  );
}
