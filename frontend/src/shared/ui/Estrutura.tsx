"use client";

import {
  Bell,
  BookOpen,
  CalendarCheck,
  CalendarDays,
  ClipboardList,
  History,
  HeartPulse,
  House,
  ListOrdered,
  Menu,
  MessageCircle,
  Plus,
  ShieldCheck,
  Stethoscope,
  UserRoundPlus,
  X,
} from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { type ReactNode, useEffect, useState } from "react";
import { useConsulta } from "@/shared/hooks/useConsulta";
import { classes } from "@/shared/lib/classes";
import { inicioDoPapel, rotaPermitida } from "@/shared/sessao/acesso";
import { rotaPublica, usuarioAtual } from "@/shared/sessao/sessao";
import { AlternarTema } from "@/shared/tema/AlternarTema";
import { Logo } from "./Logo";
import { UsuarioDaSessao } from "./UsuarioDaSessao";
import estilos from "./Estrutura.module.css";

type Item = { href: string; rotulo: string; Icone: typeof House; exato?: boolean };

const ITENS: Item[] = [
  { href: "/", rotulo: "Início", Icone: House },
  { href: "/recepcao", rotulo: "Recepção", Icone: UserRoundPlus },
  { href: "/triagem", rotulo: "Triagem", Icone: Stethoscope },
  { href: "/especialista", rotulo: "Minha agenda", Icone: CalendarCheck },
  { href: "/portal", rotulo: "Início", Icone: HeartPulse, exato: true },
  { href: "/portal/consultas", rotulo: "Minhas consultas", Icone: CalendarDays },
  { href: "/portal/fila", rotulo: "Minha fila", Icone: ListOrdered },
  { href: "/portal/historico", rotulo: "Histórico", Icone: History },
  { href: "/portal/guia", rotulo: "Como usar", Icone: BookOpen },
  { href: "/filas", rotulo: "Filas de espera", Icone: ListOrdered },
  { href: "/faltas", rotulo: "Prevenção de faltas", Icone: ShieldCheck },
  { href: "/agendamentos", rotulo: "Agendamentos", Icone: ClipboardList },
  { href: "/agendar", rotulo: "Agendar", Icone: Plus },
  { href: "/calendario", rotulo: "Calendário", Icone: CalendarDays },
  { href: "/conversas", rotulo: "Conversas", Icone: MessageCircle },
  { href: "/avisos", rotulo: "Avisos", Icone: Bell },
  { href: "/ajuda", rotulo: "Como usar", Icone: BookOpen },
];

const CHAVE_RECOLHIDO = "liva-menu-recolhido";
const MENU_ID = "menu-principal";

async function consultarSaude(): Promise<boolean> {
  const resposta = await fetch("/api/liva-health", { cache: "no-store" });
  const corpo = (await resposta.json()) as { status: string };
  return corpo.status === "UP";
}

function ativo(caminho: string, { href, exato }: Item): boolean {
  if (href === "/" || exato) return caminho === href;
  return caminho === href || caminho.startsWith(`${href}/`);
}

const TELA_LARGA = "(min-width: 960px)";

function lerRecolhido(): boolean {
  try {
    return window.localStorage.getItem(CHAVE_RECOLHIDO) === "1";
  } catch {
    return false;
  }
}

function salvarRecolhido(valor: boolean): void {
  try {
    window.localStorage.setItem(CHAVE_RECOLHIDO, valor ? "1" : "0");
  } catch {
    return;
  }
}

export function Estrutura({ children }: { children: ReactNode }) {
  const caminho = usePathname();
  if (rotaPublica(caminho))
    return (
      <main className={estilos.publica}>
        <div className={estilos.temaPublico}>
          <AlternarTema />
        </div>
        {children}
      </main>
    );
  return <EstruturaDaEquipe caminho={caminho}>{children}</EstruturaDaEquipe>;
}

function EstruturaDaEquipe({ caminho, children }: { caminho: string; children: ReactNode }) {
  const saude = useConsulta(consultarSaude, "saude", 30000);
  const usuario = useConsulta(usuarioAtual, "usuario");
  const [gavetaAberta, setGavetaAberta] = useState(false);
  const [recolhida, setRecolhida] = useState(false);
  const [larga, setLarga] = useState(false);
  const papel = usuario.dados?.papel;
  const itens = papel ? ITENS.filter(({ href }) => rotaPermitida(papel, href)) : [];
  const online = saude.dados === true;
  const offline = saude.dados === false || Boolean(saude.erro);
  const textoSaude = online ? "API Liva online" : offline ? "API Liva offline" : "Verificando API";

  useEffect(() => {
    const consulta = window.matchMedia(TELA_LARGA);
    const atualizar = () => {
      setLarga(consulta.matches);
      if (consulta.matches) setGavetaAberta(false);
    };
    setRecolhida(lerRecolhido());
    atualizar();
    consulta.addEventListener("change", atualizar);
    return () => consulta.removeEventListener("change", atualizar);
  }, []);

  useEffect(() => {
    if (!gavetaAberta) return;
    const fecharComEsc = (evento: KeyboardEvent) => {
      if (evento.key === "Escape") setGavetaAberta(false);
    };
    document.addEventListener("keydown", fecharComEsc);
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", fecharComEsc);
      document.body.style.overflow = "";
    };
  }, [gavetaAberta]);

  function alternarMenu() {
    if (larga) {
      const proximo = !recolhida;
      setRecolhida(proximo);
      salvarRecolhido(proximo);
    } else {
      setGavetaAberta((aberta) => !aberta);
    }
  }

  return (
    <div className={classes(estilos.estrutura, recolhida && estilos.recolhida)}>
      <header className={estilos.cabecalho}>
        <button
          type="button"
          className={estilos.botaoIcone}
          onClick={alternarMenu}
          aria-controls={MENU_ID}
          aria-expanded={larga ? !recolhida : gavetaAberta}
          aria-label={larga ? (recolhida ? "Expandir menu" : "Recolher menu") : "Abrir menu"}
        >
          <Menu size={22} aria-hidden="true" />
        </button>
        <Link href={papel ? inicioDoPapel(papel) : "/"} className={estilos.marca} aria-label="Liva, ir para o início">
          <Logo />
        </Link>
        <div className={estilos.direita}>
          <AlternarTema />
          <UsuarioDaSessao />
        </div>
      </header>

      <div className={estilos.corpo}>
        {gavetaAberta && <div className={estilos.sobreposicao} onClick={() => setGavetaAberta(false)} aria-hidden="true" />}

        <aside id={MENU_ID} className={classes(estilos.lateral, gavetaAberta && estilos.aberta)} aria-label="Menu">
          <div className={estilos.topoDaGaveta}>
            <Logo />
            <button type="button" className={estilos.botaoIcone} onClick={() => setGavetaAberta(false)} aria-label="Fechar menu">
              <X size={22} aria-hidden="true" />
            </button>
          </div>

          <nav aria-label="Principal" className={estilos.navegacao}>
            {itens.map((item) => (
              <Link
                key={item.href}
                href={item.href}
                className={classes(estilos.item, ativo(caminho, item) && estilos.ativo)}
                aria-current={ativo(caminho, item) ? "page" : undefined}
                title={larga && recolhida ? item.rotulo : undefined}
                onClick={() => setGavetaAberta(false)}
              >
                <item.Icone size={20} aria-hidden="true" />
                <span className={estilos.rotulo}>{item.rotulo}</span>
              </Link>
            ))}
          </nav>

          <p className={classes(estilos.saude, online && estilos.online, offline && estilos.offline)} title={textoSaude}>
            <span className={estilos.ponto} aria-hidden="true" />
            <span className={estilos.rotulo}>{textoSaude}</span>
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
      </div>
    </div>
  );
}
