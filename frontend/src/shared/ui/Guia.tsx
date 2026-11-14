import { CircleHelp, type LucideIcon } from "lucide-react";
import type { ReactNode } from "react";
import { BotaoLink } from "./Botao";
import { Cartao } from "./Cartao";
import estilos from "./Guia.module.css";

export type TopicoDoGuia = {
  id: string;
  Icone: LucideIcon;
  titulo: string;
  resumo: string;
  passos: string[];
  dica?: string;
  link?: { href: string; texto: string };
  ilustracao?: ReactNode;
  extra?: ReactNode;
};

type Props = {
  cabecalho: ReactNode;
  topicos: TopicoDoGuia[];
  rotulo: string;
  children?: ReactNode;
  rodape?: ReactNode;
};

export function Guia({ cabecalho, topicos, rotulo, children, rodape }: Props) {
  return (
    <div className={estilos.pagina}>
      {cabecalho}
      {children}
      <Indice topicos={topicos} rotulo={rotulo} />
      <Topicos topicos={topicos} />
      {rodape}
    </div>
  );
}

function Indice({ topicos, rotulo }: { topicos: TopicoDoGuia[]; rotulo: string }) {
  return (
    <nav aria-label={rotulo}>
      <ul className={estilos.indice}>
        {topicos.map(({ id, Icone, titulo, resumo }) => (
          <li key={id}>
            <a href={`#${id}`} className={estilos.atalho}>
              <span className={estilos.icone} aria-hidden="true">
                <Icone size={20} />
              </span>
              <span className={estilos.textoAtalho}>
                <strong>{titulo}</strong>
                <span>{resumo}</span>
              </span>
            </a>
          </li>
        ))}
      </ul>
    </nav>
  );
}

function Topicos({ topicos }: { topicos: TopicoDoGuia[] }) {
  return topicos.map(({ id, Icone, titulo, resumo, passos, dica, link, ilustracao, extra }) => (
    <Cartao key={id} como="section" espacamento="amplo" id={id} className={estilos.topico} aria-labelledby={`${id}-titulo`}>
      <div className={ilustracao ? estilos.comIlustracao : undefined}>
        <div className={estilos.conteudo}>
          <header className={estilos.cabecalhoTopico}>
            <span className={estilos.icone} aria-hidden="true">
              <Icone size={20} />
            </span>
            <div>
              <h2 id={`${id}-titulo`} className={estilos.titulo}>
                {titulo}
              </h2>
              <p className={estilos.resumo}>{resumo}</p>
            </div>
          </header>
          <ol className={estilos.passos}>
            {passos.map((passo, indice) => (
              <li key={passo}>
                <span className={estilos.numero} aria-hidden="true">
                  {indice + 1}
                </span>
                <span>{passo}</span>
              </li>
            ))}
          </ol>
          {dica && (
            <p className={estilos.dica}>
              <CircleHelp size={16} aria-hidden="true" />
              <span>{dica}</span>
            </p>
          )}
          {link && (
            <div>
              <BotaoLink href={link.href} variante="contorno" tamanho="pequeno">
                {link.texto}
              </BotaoLink>
            </div>
          )}
        </div>
        {ilustracao && <div className={estilos.ilustracaoTopico}>{ilustracao}</div>}
      </div>
      {extra}
    </Cartao>
  ));
}
