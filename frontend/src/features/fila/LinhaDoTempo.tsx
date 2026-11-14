"use client";

import { CalendarCheck, ClipboardCheck, Flag, ListOrdered, Sparkles } from "lucide-react";
import { type ReactNode, useEffect, useState } from "react";
import { classes } from "@/shared/lib/classes";
import { Dica } from "./Dica";
import { type Estimativa, dataCurta, relativo } from "./estimativa";
import estilos from "./JornadaNaFila.module.css";

type Marco = { chave: string; quando: number; rotulo: string; icone: ReactNode; tipo: "feito" | "hoje" | "futuro"; detalhe: string };

const ICONE = 14;
const INICIO = 6;
const FIM = 94;
const DISTANCIA_MINIMA = 14;

export function LinhaDoTempo({
  triagemEm,
  entrouEm,
  agora,
  estimativa,
}: {
  triagemEm: string;
  entrouEm: string;
  agora: number;
  estimativa: Estimativa;
}) {
  const [montado, setMontado] = useState(false);

  useEffect(() => {
    const id = window.requestAnimationFrame(() => setMontado(true));
    return () => window.cancelAnimationFrame(id);
  }, []);

  const marcos: Marco[] = [
    {
      chave: "triagem",
      quando: Date.parse(triagemEm),
      rotulo: "Triagem",
      icone: <ClipboardCheck size={ICONE} />,
      tipo: "feito",
      detalhe: "Avaliação na unidade e encaminhamento",
    },
    {
      chave: "fila",
      quando: Date.parse(entrouEm),
      rotulo: "Entrou na fila",
      icone: <ListOrdered size={ICONE} />,
      tipo: "feito",
      detalhe: "A data de entrada é mantida mesmo se você recusar uma vaga",
    },
    { chave: "hoje", quando: agora, rotulo: "Hoje", icone: <Flag size={ICONE} />, tipo: "hoje", detalhe: "Você está aqui" },
  ];
  if (estimativa.previsao !== null)
    marcos.push({
      chave: "previsao",
      quando: estimativa.previsao,
      rotulo: "Previsão de vaga",
      icone: <Sparkles size={ICONE} />,
      tipo: "futuro",
      detalhe: "Estimativa pelo ritmo das últimas 4 semanas. Pode chegar antes",
    });
  if (estimativa.consulta !== null)
    marcos.push({
      chave: "consulta",
      quando: estimativa.consulta,
      rotulo: "Consulta marcada",
      icone: <CalendarCheck size={ICONE} />,
      tipo: estimativa.consulta < agora ? "feito" : "futuro",
      detalhe: "Se surgir uma vaga antes, oferecemos a troca",
    });
  marcos.sort((a, b) => a.quando - b.quando);
  const grupos = agruparPorDia(marcos);
  const lugares = espalhar(grupos.map((g) => g.quando));
  const indiceDeHoje = grupos.findIndex((g) => g.marcos.some((m) => m.chave === "hoje"));
  const hoje = lugares[indiceDeHoje];

  return (
    <div className={estilos.linhaDoTempo}>
      <div className={estilos.eixo}>
        <span className={estilos.eixoPercorrido} style={{ width: montado ? `${hoje}%` : "0%" }} />
        <span className={estilos.eixoFuturo} style={{ left: `${hoje}%` }} />
        {grupos.map((g, i) => {
          const principal = g.marcos.find((m) => m.chave === "hoje") ?? g.marcos[g.marcos.length - 1];
          return (
            <span key={g.marcos.map((m) => m.chave).join("-")} className={estilos.marco} style={{ left: `${lugares[i]}%` }}>
              <Dica
                posicao={i % 2 === 0 ? "acima" : "abaixo"}
                texto={g.marcos.map((m) => (
                  <span key={m.chave} className={estilos.itemDaDica}>
                    <strong>
                      {m.rotulo} · {dataCurta(m.quando)}
                    </strong>
                    {m.chave === "hoje" ? m.detalhe : `${relativo(m.quando, agora)}. ${m.detalhe}`}
                  </span>
                ))}
              >
                <span
                  className={classes(estilos.ponto, estilos[principal.tipo])}
                  style={{ animationDelay: `${200 + i * 120}ms` }}
                  aria-hidden="true"
                >
                  {principal.icone}
                </span>
              </Dica>
              <span className={classes(estilos.rotuloMarco, i % 2 === 0 ? estilos.embaixo : estilos.emCima)}>
                {g.marcos.map((m) => m.rotulo).join(" · ")}
                <small>{dataCurta(g.quando)}</small>
              </span>
            </span>
          );
        })}
      </div>
    </div>
  );
}

function agruparPorDia(marcos: Marco[]): { quando: number; marcos: Marco[] }[] {
  const grupos: { quando: number; marcos: Marco[] }[] = [];
  marcos.forEach((m) => {
    const ultimo = grupos[grupos.length - 1];
    if (ultimo && dataCurta(ultimo.quando) === dataCurta(m.quando)) ultimo.marcos.push(m);
    else grupos.push({ quando: m.quando, marcos: [m] });
  });
  return grupos;
}

function espalhar(instantes: number[]): number[] {
  const inicio = instantes[0];
  const extensao = Math.max(1, instantes[instantes.length - 1] - inicio);
  const lugares = instantes.map((t) => INICIO + ((t - inicio) / extensao) * (FIM - INICIO));
  for (let i = 1; i < lugares.length; i++) lugares[i] = Math.max(lugares[i], lugares[i - 1] + DISTANCIA_MINIMA);
  lugares[lugares.length - 1] = Math.min(lugares[lugares.length - 1], FIM);
  for (let i = lugares.length - 2; i >= 0; i--) lugares[i] = Math.min(lugares[i], lugares[i + 1] - DISTANCIA_MINIMA);
  return lugares;
}
