"use client";

import { meusAtendimentos } from "@/features/assistencial/api";
import { AtendimentosPaginados } from "@/features/assistencial/componentes/AtendimentosPaginados";
import { CabecalhoPagina } from "@/shared/ui/CabecalhoPagina";
import { Secao } from "@/shared/ui/Secao";
import estilos from "./Portal.module.css";

export function MeuHistorico() {
  return (
    <div className={estilos.pagina}>
      <CabecalhoPagina
        titulo="Histórico"
        subtitulo="Cada triagem registrada na unidade, com os encaminhamentos e consultas que vieram dela."
      />
      <Secao titulo="Atendimentos">
        <AtendimentosPaginados chave="meus-atendimentos" carregar={meusAtendimentos} linkParaAgendar={(id) => `/portal/agendar/${id}`} />
      </Secao>
    </div>
  );
}
