"use client";

import { useState } from "react";
import { percentual } from "@/features/fila/rotulos";
import { formatarDataCurta } from "@/shared/lib/datas";
import { taxaDeFaltas } from "./calculos";
import type { SemanaDeComparecimento } from "./tipos";
import estilos from "./Graficos.module.css";

const LARGURA = 720;
const ALTURA = 200;
const MARGEM = { topo: 16, direita: 16, base: 28, esquerda: 40 };

export function GraficoDeTaxa({ semanas }: { semanas: SemanaDeComparecimento[] }) {
  const [foco, setFoco] = useState<number | null>(null);
  const taxas = semanas.map(taxaDeFaltas);
  const maximo = Math.max(0.1, ...taxas.map((t) => t ?? 0));
  const topo = Math.ceil((maximo * 100) / 10) * 10;
  const marcas = [0, topo / 2, topo];
  const areaAltura = ALTURA - MARGEM.topo - MARGEM.base;
  const areaLargura = LARGURA - MARGEM.esquerda - MARGEM.direita;
  const passo = areaLargura / Math.max(1, semanas.length - 1);
  const x = (i: number) => MARGEM.esquerda + passo * i;
  const y = (valor: number) => MARGEM.topo + areaAltura - ((valor * 100) / topo) * areaAltura;
  const pontos = taxas.map((t, i) => (t === null ? null : { x: x(i), y: y(t) })).filter((p) => p !== null);
  const linha = pontos.map((p, i) => `${i === 0 ? "M" : "L"}${p.x},${p.y}`).join("");
  const area = pontos.length > 1 ? `${linha}L${pontos[pontos.length - 1].x},${y(0)}L${pontos[0].x},${y(0)}Z` : "";

  function aproximar(evento: React.MouseEvent<SVGRectElement>) {
    const caixa = evento.currentTarget.getBoundingClientRect();
    const relativo = ((evento.clientX - caixa.left) / caixa.width) * areaLargura;
    setFoco(Math.max(0, Math.min(semanas.length - 1, Math.round(relativo / passo))));
  }

  const taxaEmFoco = foco === null ? null : taxas[foco];

  return (
    <figure className={estilos.figura}>
      <div className={estilos.area} onMouseLeave={() => setFoco(null)}>
        <svg viewBox={`0 0 ${LARGURA} ${ALTURA}`} className={estilos.svg} role="img" aria-label="Percentual de faltas por semana">
          <defs>
            <linearGradient id="preenchimento-taxa" x1="0" x2="0" y1="0" y2="1">
              <stop offset="0%" stopColor="var(--grafico-faltou)" stopOpacity="0.22" />
              <stop offset="100%" stopColor="var(--grafico-faltou)" stopOpacity="0" />
            </linearGradient>
          </defs>
          {marcas.map((m) => (
            <g key={m}>
              <line x1={MARGEM.esquerda} x2={LARGURA - MARGEM.direita} y1={y(m / 100)} y2={y(m / 100)} className={estilos.grade} />
              <text x={MARGEM.esquerda - 8} y={y(m / 100)} className={estilos.eixoY}>
                {m}%
              </text>
            </g>
          ))}
          {area && <path d={area} fill="url(#preenchimento-taxa)" className={estilos.areaTaxa} />}
          <path d={linha} className={estilos.linhaTaxa} pathLength={1} />
          {semanas.map(
            (s, i) =>
              (i % Math.ceil(semanas.length / 8) === 0 || i === semanas.length - 1) && (
                <text key={s.inicio} x={x(i)} y={ALTURA - 8} className={estilos.eixoX}>
                  {formatarDataCurta(s.inicio)}
                </text>
              ),
          )}
          {foco !== null && taxaEmFoco !== null && (
            <g pointerEvents="none">
              <line x1={x(foco)} x2={x(foco)} y1={MARGEM.topo} y2={y(0)} className={estilos.mira} />
              <circle cx={x(foco)} cy={y(taxaEmFoco)} r={5} className={estilos.ponto} />
            </g>
          )}
          <rect
            x={MARGEM.esquerda}
            y={MARGEM.topo}
            width={areaLargura}
            height={areaAltura}
            className={estilos.alvo}
            onMouseMove={aproximar}
            aria-hidden="true"
          />
        </svg>
        {foco !== null && (
          <div
            className={estilos.dica}
            style={{ left: `${(x(foco) / LARGURA) * 100}%` }}
            data-lado={foco > semanas.length / 2 ? "esquerda" : "direita"}
            role="tooltip"
          >
            <strong>Semana de {formatarDataCurta(semanas[foco].inicio)}</strong>
            <span className={estilos.linhaDica}>
              Faltas
              <b>{percentual(taxaEmFoco)}</b>
            </span>
            <span className={estilos.rodapeDica}>
              {semanas[foco].faltaram} de {semanas[foco].compareceram + semanas[foco].faltaram} consultas realizadas
            </span>
          </div>
        )}
      </div>
      <table className={estilos.somenteLeitor}>
        <caption>Percentual de faltas por semana</caption>
        <thead>
          <tr>
            <th scope="col">Semana</th>
            <th scope="col">Faltas</th>
          </tr>
        </thead>
        <tbody>
          {semanas.map((s, i) => (
            <tr key={s.inicio}>
              <td>{formatarDataCurta(s.inicio)}</td>
              <td>{percentual(taxas[i])}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </figure>
  );
}
