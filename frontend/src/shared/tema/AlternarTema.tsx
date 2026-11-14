"use client";

import { Moon, Sun } from "lucide-react";
import { useEffect, useState } from "react";
import { type Tema, aplicarTema, temaAtual, temaEscolhido } from "./tema";
import estilos from "./AlternarTema.module.css";

const SISTEMA_ESCURO = "(prefers-color-scheme: dark)";

export function AlternarTema() {
  const [tema, setTema] = useState<Tema | null>(null);

  useEffect(() => {
    setTema(temaAtual());
    const consulta = window.matchMedia(SISTEMA_ESCURO);
    const seguirSistema = () => {
      if (temaEscolhido()) return;
      const proximo: Tema = consulta.matches ? "escuro" : "claro";
      aplicarTema(proximo, false);
      setTema(proximo);
    };
    consulta.addEventListener("change", seguirSistema);
    return () => consulta.removeEventListener("change", seguirSistema);
  }, []);

  const escuro = tema === "escuro";

  function alternar() {
    const proximo: Tema = escuro ? "claro" : "escuro";
    aplicarTema(proximo, true);
    setTema(proximo);
  }

  return (
    <button
      type="button"
      className={estilos.botao}
      onClick={alternar}
      aria-pressed={tema === null ? undefined : escuro}
      aria-label="Modo escuro"
      title={escuro ? "Usar modo claro" : "Usar modo escuro"}
    >
      {escuro ? <Sun size={20} aria-hidden="true" /> : <Moon size={20} aria-hidden="true" />}
    </button>
  );
}
