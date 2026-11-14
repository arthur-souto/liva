import Link from "next/link";
import type { ButtonHTMLAttributes, ReactNode } from "react";
import { classes } from "@/shared/lib/classes";
import estilos from "./Botao.module.css";

type Aparencia = {
  variante?: "primario" | "secundario" | "contorno" | "fantasma" | "perigo";
  tamanho?: "pequeno" | "medio" | "grande";
  larguraTotal?: boolean;
  icone?: ReactNode;
  iconeDireita?: ReactNode;
  className?: string;
  children: ReactNode;
};

function nomesDoBotao({ variante = "primario", tamanho = "medio", larguraTotal, className }: Omit<Aparencia, "children">): string {
  return classes(estilos.botao, estilos[variante], estilos[tamanho], larguraTotal && estilos.larguraTotal, className);
}

type PropsBotao = Aparencia & Omit<ButtonHTMLAttributes<HTMLButtonElement>, "children" | "className"> & { carregando?: boolean };

export function Botao({ variante, tamanho, larguraTotal, icone, iconeDireita, carregando, className, children, ...resto }: PropsBotao) {
  return (
    <button
      type="button"
      {...resto}
      className={nomesDoBotao({ variante, tamanho, larguraTotal, className })}
      disabled={resto.disabled || carregando}
      aria-busy={carregando || undefined}
    >
      {carregando ? <span className={estilos.girando} aria-hidden="true" /> : icone}
      <span>{children}</span>
      {!carregando && iconeDireita}
    </button>
  );
}

export function BotaoLink({ href, icone, iconeDireita, children, ...aparencia }: Aparencia & { href: string }) {
  return (
    <Link href={href} className={nomesDoBotao(aparencia)}>
      {icone}
      <span>{children}</span>
      {iconeDireita}
    </Link>
  );
}
