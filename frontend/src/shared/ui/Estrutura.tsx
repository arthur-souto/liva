"use client";

import { Bell, CalendarCheck, CalendarDays, ClipboardList, HeartPulse, House, MessageCircle, Plus, Stethoscope, UserRoundPlus } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import type { CSSProperties, ReactNode } from "react";
import { useConsulta } from "@/shared/hooks/useConsulta";
import { classes } from "@/shared/lib/classes";
import { rotaPermitida } from "@/shared/sessao/acesso";
import { rotaPublica, usuarioAtual } from "@/shared/sessao/sessao";
import { Logo } from "./Logo";
import { UsuarioDaSessao } from "./UsuarioDaSessao";
import estilos from "./Estrutura.module.css";

const ITENS = [
  { href: "/", rotulo: "Início", curto: "Início", Icone: House },
  { href: "/recepcao", rotulo: "Recepção", curto: "Recepção", Icone: UserRoundPlus },
  { href: "/triagem", rotulo: "Triagem", curto: "Triagem", Icone: Stethoscope },
  { href: "/especialista", rotulo: "Minha agenda", curto: "Agenda", Icone: CalendarCheck },
  { href: "/portal", rotulo: "Meus atendimentos", curto: "Início", Icone: HeartPulse },
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
  if (rotaPublica(caminho)) return <main className={estilos.publica}>{children}</main>;
  return <EstruturaDaEquipe caminho={caminho}>{children}</EstruturaDaEquipe>;
}

function EstruturaDaEquipe({ caminho, children }: { caminho: string; children: ReactNode }) {
  const saude = useConsulta(consultarSaude, "saude", 30000);
  const usuario = useConsulta(usuarioAtual, "usuario");
  const papel = usuario.dados?.papel;
  const itens = papel ? ITENS.filter(({ href }) => rotaPermitida(papel, href)) : [];
  const online = saude.dados === true;
  const offline = saude.dados === false || Boolean(saude.erro);

  const links = (campo: "rotulo" | "curto") =>
    itens.map(({ href, Icone, ...rotulos }) => (
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
        <UsuarioDaSessao />
      </aside>

      <main className={estilos.conteudo}>
        <div className={estilos.sessaoMovel}>
          <UsuarioDaSessao compacto />
        </div>
        {offline && (
          <p className={estilos.faixaOffline} role="alert">
            API Liva offline. Confira se ela está rodando em LIVA_API_URL.
          </p>
        )}
        {children}
      </main>

      <nav aria-label="Principal" className={estilos.navInferior} style={{ "--colunas": Math.max(itens.length, 1) } as CSSProperties}>
        {links("curto")}
      </nav>
    </div>
  );
}
