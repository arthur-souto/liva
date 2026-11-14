"use client";

import { meusAtendimentos } from "@/features/assistencial/api";
import { AtendimentosPaginados } from "@/features/assistencial/componentes/AtendimentosPaginados";
import { CabecalhoPagina } from "@/shared/ui/CabecalhoPagina";
import { Secao } from "@/shared/ui/Secao";
import { SinaisVitaisNoTempo } from "./SinaisVitaisNoTempo";
import { usePortal } from "./usePortal";
import estilos from "./Portal.module.css";

export function MeuHistorico() {
  const historico = usePortal();
  const comSinais = historico.dados?.ramos.some((r) => r.acessoClinico && r.triagem.sinaisVitais) ?? false;

  return (
    <div className={estilos.pagina}>
      <CabecalhoPagina
        titulo="Histórico"
        subtitulo="Cada triagem registrada na unidade, com os encaminhamentos, as consultas e o que a equipe anotou."
      />
      {historico.dados && comSinais && (
        <Secao
          titulo="Seus sinais vitais"
          descricao="Medidos pela equipe na triagem de cada atendimento. Passe o dedo ou o mouse sobre o gráfico para ver cada medida."
        >
          <SinaisVitaisNoTempo ramos={historico.dados.ramos} />
        </Secao>
      )}
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
