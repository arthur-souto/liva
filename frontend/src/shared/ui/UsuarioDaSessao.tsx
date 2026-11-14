"use client";

import { LogOut } from "lucide-react";
import { useState } from "react";
import { useConsulta } from "@/shared/hooks/useConsulta";
import { ROTULOS_DE_PAPEL, sair, usuarioAtual } from "@/shared/sessao/sessao";
import estilos from "./UsuarioDaSessao.module.css";

function iniciais(nome: string): string {
  const partes = nome.replace(/^(Dra?\.)\s+/i, "").split(/\s+/).filter(Boolean);
  return ((partes[0]?.[0] ?? "") + (partes.length > 1 ? partes[partes.length - 1][0] : "")).toUpperCase();
}

export function UsuarioDaSessao() {
  const sessao = useConsulta(usuarioAtual, "sessao");
  const [saindo, setSaindo] = useState(false);

  async function encerrar() {
    setSaindo(true);
    await sair().catch(() => undefined);
    window.location.assign("/entrar");
  }

  return (
    <div className={estilos.usuario}>
      {sessao.dados && (
        <>
          <span className={estilos.avatar} aria-hidden="true">
            {iniciais(sessao.dados.nome)}
          </span>
          <div className={estilos.identificacao}>
            <span className={estilos.nome}>{sessao.dados.nome}</span>
            <span className={estilos.papel}>{ROTULOS_DE_PAPEL[sessao.dados.papel]}</span>
          </div>
        </>
      )}
      <button type="button" className={estilos.sair} onClick={encerrar} disabled={saindo} aria-label="Sair">
        <LogOut size={18} aria-hidden="true" />
        <span className={estilos.textoSair}>Sair</span>
      </button>
    </div>
  );
}
