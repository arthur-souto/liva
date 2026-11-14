"use client";

import { useState } from "react";
import { percentual } from "@/features/fila/rotulos";
import { formatarDataCurta } from "@/shared/lib/datas";
import { taxaDeFaltas } from "./calculos";
import type { SemanaDeComparecimento } from "./tipos";
import estilos from "./Graficos.module.css";

const LARGURA = 720;
const ALTURA = 260;
const MARGEM = { topo: 16, direita: 12, base: 28, esquerda: 36 };
const VAO = 2;

const SERIES = [
  { chave: "compareceram", rotulo: "Compareceram", cor: "var(--grafico-compareceu)" },
  { chave: "faltaram", rotulo: "Faltaram", cor: "var(--grafico-faltou)" },
  { chave: "cancelaramAntes", rotulo: "Cancelaram antes", cor: "var(--grafico-cancelou)" },
] as const;

function topoArredondado(x: number, y: number, largura: number, altura: number, raio: number): string {
  const r = Math.min(raio, altura, largura / 2);
  return `M${x},${y + altura}V${y + r}Q${x},${y} ${x + r},${y}H${x + largura - r}Q${x + largura},${y} ${x + largura},${y + r}V${y + altura}Z`;
}

function escala(maximo: number): number[] {
  const passo = Math.max(5, Math.ceil(maximo / 4 / 5) * 5);
  return [0, passo, passo * 2, passo * 3, passo * 4];
}

export function GraficoSemanal({ semanas }: { semanas: SemanaDeComparecimento[] }) {
  const [foco, setFoco] = useState<number | null>(null);
  const totais = semanas.map((s) => s.compareceram + s.faltaram + s.cancelaramAntes);
  const marcas = escala(Math.max(1, ...totais));
  const topo = marcas[marcas.length - 1];
  const areaAltura = ALTURA - MARGEM.topo - MARGEM.base;
  const areaLargura = LARGURA - MARGEM.esquerda - MARGEM.direita;
  const coluna = areaLargura / Math.max(1, semanas.length);
  const largura = Math.min(36, coluna * 0.62);
  const y = (valor: number) => MARGEM.topo + areaAltura - (valor / topo) * areaAltura;
  const selecionada = foco === null ? null : semanas[foco];

  return (
    <figure className={estilos.figura}>
      <ul className={estilos.legenda}>
        {SERIES.map((s) => (
          <li key={s.chave}>
            <span className={estilos.amostra} style={{ background: s.cor }} aria-hidden="true" />
            {s.rotulo}
          </li>
        ))}
      </ul>
      <div className={estilos.area} onMouseLeave={() => setFoco(null)}>
        <svg viewBox={`0 0 ${LARGURA} ${ALTURA}`} className={estilos.svg} role="img" aria-label="Consultas por semana, por situação">
          {marcas.map((m) => (
            <g key={m}>
              <line x1={MARGEM.esquerda} x2={LARGURA - MARGEM.direita} y1={y(m)} y2={y(m)} className={estilos.grade} />
              <text x={MARGEM.esquerda - 8} y={y(m)} className={estilos.eixoY}>
                {m}
              </text>
            </g>
          ))}
          {semanas.map((s, i) => {
            const x = MARGEM.esquerda + coluna * i + (coluna - largura) / 2;
            let base = 0;
            const segmentos = SERIES.map((serie) => {
              const valor = s[serie.chave];
              const inicio = base;
              base += valor;
              return { serie, valor, inicio };
            }).filter((seg) => seg.valor > 0);
            return (
              <g key={s.inicio} className={estilos.coluna} style={{ animationDelay: `${i * 40}ms` }}>
                {segmentos.map(({ serie, valor, inicio }, indice) => {
                  const yTopo = y(inicio + valor);
                  const altura = Math.max(0, y(inicio) - yTopo - (indice > 0 ? VAO : 0));
                  const ultimo = indice === segmentos.length - 1;
                  return ultimo ? (
                    <path key={serie.chave} d={topoArredondado(x, yTopo, largura, altura, 4)} fill={serie.cor} />
                  ) : (
                    <rect key={serie.chave} x={x} y={yTopo} width={largura} height={altura} fill={serie.cor} />
                  );
                })}
                {(i % Math.ceil(semanas.length / 8) === 0 || i === semanas.length - 1) && (
                  <text x={x + largura / 2} y={ALTURA - 8} className={estilos.eixoX}>
                    {formatarDataCurta(s.inicio)}
                  </text>
                )}
                <rect
                  x={MARGEM.esquerda + coluna * i}
                  y={MARGEM.topo}
                  width={coluna}
                  height={areaAltura}
                  className={estilos.alvo}
                  tabIndex={0}
                  aria-label={`Semana de ${formatarDataCurta(s.inicio)}: ${s.compareceram} compareceram, ${s.faltaram} faltaram, ${s.cancelaramAntes} cancelaram antes`}
                  onMouseEnter={() => setFoco(i)}
                  onFocus={() => setFoco(i)}
                  onBlur={() => setFoco(null)}
                />
              </g>
            );
          })}
          {foco !== null && (
            <rect
              x={MARGEM.esquerda + coluna * foco}
              y={MARGEM.topo}
              width={coluna}
              height={areaAltura}
              className={estilos.realce}
              pointerEvents="none"
            />
          )}
        </svg>
        {selecionada && foco !== null && (
          <div
            className={estilos.dica}
            style={{ left: `${((MARGEM.esquerda + coluna * (foco + 0.5)) / LARGURA) * 100}%` }}
            data-lado={foco > semanas.length / 2 ? "esquerda" : "direita"}
            role="tooltip"
          >
            <strong>Semana de {formatarDataCurta(selecionada.inicio)}</strong>
            {SERIES.map((serie) => (
              <span key={serie.chave} className={estilos.linhaDica}>
                <span className={estilos.amostra} style={{ background: serie.cor }} aria-hidden="true" />
                {serie.rotulo}
                <b>{selecionada[serie.chave]}</b>
              </span>
            ))}
            <span className={estilos.rodapeDica}>
              Faltas: {percentual(taxaDeFaltas(selecionada))} · {selecionada.reaproveitadas} vagas reaproveitadas
            </span>
          </div>
        )}
      </div>
    </figure>
  );
}
