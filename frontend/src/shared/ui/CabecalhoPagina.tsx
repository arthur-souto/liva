import type { ReactNode } from "react";
import estilos from "./CabecalhoPagina.module.css";

type Props = {
  titulo: string;
  subtitulo?: string;
  acoes?: ReactNode;
};

export function CabecalhoPagina({ titulo, subtitulo, acoes }: Props) {
  return (
    <header className={estilos.cabecalho}>
      <div className={estilos.textos}>
        <h1 className={estilos.titulo}>{titulo}</h1>
        {subtitulo && <p className={estilos.subtitulo}>{subtitulo}</p>}
      </div>
      {acoes && <div className={estilos.lateral}>{acoes}</div>}
    </header>
  );
}
