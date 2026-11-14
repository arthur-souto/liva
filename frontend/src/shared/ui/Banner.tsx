import type { ReactNode } from "react";
import estilos from "./Banner.module.css";

type Props = {
  titulo: string;
  children?: ReactNode;
  ilustracao: ReactNode;
  acoes?: ReactNode;
};

export function Banner({ titulo, children, ilustracao, acoes }: Props) {
  return (
    <header className={estilos.banner}>
      <div className={estilos.texto}>
        <h1 className={estilos.titulo}>{titulo}</h1>
        {children && <div className={estilos.descricao}>{children}</div>}
        {acoes && <div className={estilos.acoes}>{acoes}</div>}
      </div>
      <div className={estilos.ilustracao}>{ilustracao}</div>
    </header>
  );
}
