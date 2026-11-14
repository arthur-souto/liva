"use client";

import { Bell, CalendarDays, ClipboardList, House, MessageCircle, Plus } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import type { ReactNode } from "react";
import { useConsulta } from "@/shared/hooks/useConsulta";
import { classes } from "@/shared/lib/classes";
import { Logo } from "./Logo";
import estilos from "./Estrutura.module.css";

const ITENS = [
  { href: "/", rotulo: "Início", curto: "Início", Icone: House },
  { href: "/agendamentos", rotulo: "Agendamentos", curto: "Lista", Icone: ClipboardList },
  { href: "/agendar", rotulo: "Agendar", curto: "Agendar", Icone: Plus },
  { href: "/calendario", rotulo: "Calendário", curto: "Agenda", Icone: CalendarDays },
  { href: "/conversas", rotulo: "Conversas", curto: "Chat", Icone: MessageCircle },
  { href: "/avisos", rotulo: "Avisos", curto: "Avisos", Icone: Bell },
];

async function consultarSaude(): Promise<boolean> {
  const resposta = await fetch("/api/liva-health", { cache: "no-store" });
  const corpo = (await resposta.json()) as { status: string };
  return corpo.status === "UP";
}

function ativo(caminho: string, href: string): boolean {
  if (href === "/") return caminho === "/";
  return caminho.startsWith(href);
}

export function Estrutura({ children }: { children: ReactNode }) {
  const caminho = usePathname();
  const saude = useConsulta(consultarSaude, "saude", 30000);
  const online = saude.dados === true;
  const offline = saude.dados === false || Boolean(saude.erro);

  const links = (campo: "rotulo" | "curto") =>
    ITENS.map(({ href, Icone, ...rotulos }) => (
      <Link
        key={href}
        href={href}
        className={classes(estilos.item, ativo(caminho, href) && estilos.ativo)}
        aria-current={ativo(caminho, href) ? "page" : undefined}
      >
        <Icone size={22} aria-hidden="true" />
        <span>{rotulos[campo]}</span>
      </Link>
    ));

  return (
    <div className={estilos.estrutura}>
      <aside className={estilos.lateral}>
        <div className={estilos.marca}>
          <Logo tamanho="grande" />
          <p className={estilos.slogan}>Seu cuidado, mais perto.</p>
        </div>
        <nav aria-label="Principal" className={estilos.navLateral}>
          {links("rotulo")}
        </nav>
        <p className={classes(estilos.saude, online && estilos.online, offline && estilos.offline)}>
          <span className={estilos.ponto} aria-hidden="true" />
          {online ? "API Liva online" : offline ? "API Liva offline" : "Verificando API"}
        </p>
      </aside>

      <main className={estilos.conteudo}>
        {offline && (
          <p className={estilos.faixaOffline} role="alert">
            API Liva offline. Confira se ela está rodando em LIVA_API_URL.
          </p>
        )}
        {children}
      </main>

      <nav aria-label="Principal" className={estilos.navInferior}>
        {links("curto")}
      </nav>
    </div>
  );
}
