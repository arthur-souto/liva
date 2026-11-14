"use client";

import { ArrowRight, Printer } from "lucide-react";
import Link from "next/link";
import { useRef } from "react";
import { DetalhesDoAtendimento } from "@/features/assistencial/componentes/DetalhesDoAtendimento";
import { LinhaDoTempo } from "@/features/assistencial/componentes/LinhaDoTempo";
import type { RamoDoHistorico } from "@/features/assistencial/tipos";
import { formatarInstante } from "@/shared/lib/datas";
import { Botao } from "@/shared/ui/Botao";
import { Secao } from "@/shared/ui/Secao";
import estilos from "./Portal.module.css";

export function UltimoAtendimento({ ramo, nome }: { ramo: RamoDoHistorico; nome: string | undefined }) {
  const conteudo = useRef<HTMLDivElement>(null);

  function imprimir() {
    const alvo = conteudo.current;
    if (!alvo) return;
    alvo.setAttribute("data-imprimir", "");
    document.body.setAttribute("data-imprimindo", "");
    const limpar = () => {
      alvo.removeAttribute("data-imprimir");
      document.body.removeAttribute("data-imprimindo");
      window.removeEventListener("afterprint", limpar);
    };
    window.addEventListener("afterprint", limpar);
    window.print();
  }

  return (
    <Secao
      titulo="Seu último atendimento"
      descricao={`O que a equipe registrou a partir da triagem de ${formatarInstante(ramo.triagem.chegadaEm)}.`}
      acoes={
        <div className={estilos.acoesDaSecao}>
          <Botao variante="contorno" tamanho="pequeno" icone={<Printer size={16} aria-hidden="true" />} onClick={imprimir}>
            Imprimir
          </Botao>
          <Link href="/portal/historico" className={estilos.link}>
            Histórico completo <ArrowRight size={16} aria-hidden="true" />
          </Link>
        </div>
      }
    >
      <div ref={conteudo} className={estilos.ultimo}>
        <header data-so-impressao="" className={estilos.cabecalhoImpressao}>
          <strong>Liva · Resumo do atendimento</strong>
          <span>{nome}</span>
          <span>Triagem de {formatarInstante(ramo.triagem.chegadaEm)}</span>
          <span>Impresso em {formatarInstante(new Date().toISOString())}</span>
        </header>
        {ramo.encaminhamentos.map((e) => (
          <LinhaDoTempo key={e.id} chegadaEm={ramo.triagem.chegadaEm} encaminhamento={e} consultas={ramo.consultas} />
        ))}
        <DetalhesDoAtendimento triagemId={ramo.triagem.id} />
      </div>
    </Secao>
  );
}
