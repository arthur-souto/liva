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
        subtitulo="Cada triagem registrada na unidade, com os encaminhamentos, as consultas e o que a equipe anotou."
      />
      <Secao titulo="Atendimentos">
        <AtendimentosPaginados
          chave="meus-atendimentos"
          carregar={meusAtendimentos}
          linkParaAgendar={(id) => `/portal/agendar/${id}`}
          comDetalhes
        />
      </Secao>
    </div>
  );
}
