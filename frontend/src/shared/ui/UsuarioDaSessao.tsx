"use client";

import { LogOut } from "lucide-react";
import { useState } from "react";
import { useConsulta } from "@/shared/hooks/useConsulta";
import { ROTULOS_DE_PAPEL, sair, usuarioAtual } from "@/shared/sessao/sessao";
import { classes } from "@/shared/lib/classes";
import estilos from "./UsuarioDaSessao.module.css";

export function UsuarioDaSessao({ compacto = false }: { compacto?: boolean }) {
  const sessao = useConsulta(usuarioAtual, "sessao");
  const [saindo, setSaindo] = useState(false);

  async function encerrar() {
    setSaindo(true);
    await sair().catch(() => undefined);
    window.location.assign("/entrar");
  }

  return (
    <div className={classes(estilos.usuario, compacto && estilos.compacto)}>
      {sessao.dados && (
        <div className={estilos.identificacao}>
          <span className={estilos.nome}>{sessao.dados.nome}</span>
          <span className={estilos.papel}>{ROTULOS_DE_PAPEL[sessao.dados.papel]}</span>
        </div>
      )}
      <button type="button" className={estilos.sair} onClick={encerrar} disabled={saindo}>
        <LogOut size={18} aria-hidden="true" />
        <span>Sair</span>
      </button>
    </div>
  );
}
