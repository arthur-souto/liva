"use client";

import { useState } from "react";
import type { RamoDoHistorico, SinaisVitais } from "@/features/assistencial/tipos";
import graficos from "@/features/faltas/Graficos.module.css";
import { formatarInstante } from "@/shared/lib/datas";
import estilos from "./SinaisVitaisNoTempo.module.css";

const LARGURA = 320;
const ALTURA = 132;
const MARGEM = { topo: 12, direita: 40, base: 22, esquerda: 34 };

type Serie = { nome: string; cor: string; valores: (number | null)[] };
type Medida = { titulo: string; unidade: string; series: Serie[]; casas?: number };
type Medicao = { quando: string; sinais: SinaisVitais };

function numero(valor: number | null, casas = 0): string {
  return valor === null ? "—" : valor.toLocaleString("pt-BR", { minimumFractionDigits: casas, maximumFractionDigits: casas });
}

function dia(instante: string): string {
  return formatarInstante(instante).split(" ·")[0];
}

function medidas(lista: Medicao[]): Medida[] {
  const de = (campo: keyof SinaisVitais) => lista.map((m) => (m.sinais[campo] === null ? null : Number(m.sinais[campo])));
  return [
    {
      titulo: "Pressão arterial",
      unidade: "mmHg",
      series: [
        { nome: "Máxima", cor: "var(--grafico-sistolica)", valores: de("pressaoSistolica") },
        { nome: "Mínima", cor: "var(--grafico-diastolica)", valores: de("pressaoDiastolica") },
      ],
    },
    {
      titulo: "Batimentos do coração",
      unidade: "bpm",
      series: [{ nome: "Batimentos", cor: "var(--grafico-sistolica)", valores: de("frequenciaCardiaca") }],
    },
    {
      titulo: "Oxigenação do sangue",
      unidade: "%",
      series: [{ nome: "Saturação", cor: "var(--grafico-sistolica)", valores: de("saturacao") }],
    },
    {
      titulo: "Temperatura",
      unidade: "°C",
      casas: 1,
      series: [{ nome: "Temperatura", cor: "var(--grafico-sistolica)", valores: de("temperatura") }],
    },
    {
      titulo: "Respiração",
      unidade: "irpm",
      series: [{ nome: "Respirações por minuto", cor: "var(--grafico-sistolica)", valores: de("frequenciaRespiratoria") }],
    },
  ].filter((m) => m.series.some((s) => s.valores.some((v) => v !== null)));
}

function escala(valores: number[]): [number, number] {
  const menor = Math.min(...valores);
  const maior = Math.max(...valores);
  const folga = Math.max(1, (maior - menor) * 0.2);
  return [Math.floor(menor - folga), Math.ceil(maior + folga)];
}

function Grafico({ medida, datas }: { medida: Medida; datas: string[] }) {
  const [foco, setFoco] = useState<number | null>(null);
  const todos = medida.series.flatMap((s) => s.valores).filter((v): v is number => v !== null);
  const [minimo, maximo] = escala(todos);
  const areaLargura = LARGURA - MARGEM.esquerda - MARGEM.direita;
  const areaAltura = ALTURA - MARGEM.topo - MARGEM.base;
  const passo = areaLargura / Math.max(1, datas.length - 1);
  const x = (i: number) => MARGEM.esquerda + passo * i;
  const y = (v: number) => MARGEM.topo + areaAltura - ((v - minimo) / (maximo - minimo)) * areaAltura;
  const marcas = [minimo, Math.round((minimo + maximo) / 2), maximo];
  const varias = medida.series.length > 1;

  return (
    <div className={graficos.area} onMouseLeave={() => setFoco(null)}>
      <svg viewBox={`0 0 ${LARGURA} ${ALTURA}`} className={graficos.svg} role="img" aria-label={`${medida.titulo} em cada triagem`}>
        {marcas.map((m) => (
          <g key={m}>
            <line x1={MARGEM.esquerda} x2={LARGURA - MARGEM.direita} y1={y(m)} y2={y(m)} className={graficos.grade} />
            <text x={MARGEM.esquerda - 6} y={y(m)} className={graficos.eixoY}>
              {numero(m)}
            </text>
          </g>
        ))}
        {datas.map(
          (d, i) =>
            (i === 0 || i === datas.length - 1 || datas.length <= 4) && (
              <text key={`${d}-${i}`} x={x(i)} y={ALTURA - 6} className={graficos.eixoX}>
                {dia(d)}
              </text>
            ),
        )}
        {foco !== null && <line x1={x(foco)} x2={x(foco)} y1={MARGEM.topo} y2={MARGEM.topo + areaAltura} className={graficos.mira} />}
        {medida.series.map((s) => {
          const pontos = s.valores.map((v, i) => (v === null ? null : { x: x(i), y: y(v), v })).filter((p) => p !== null);
          const ultimo = pontos[pontos.length - 1];
          return (
            <g key={s.nome}>
              <path
                d={pontos.map((p, i) => `${i === 0 ? "M" : "L"}${p.x},${p.y}`).join("")}
                className={estilos.linha}
                style={{ stroke: s.cor }}
              />
              {pontos.map((p) => (
                <circle key={p.x} cx={p.x} cy={p.y} r={4} className={estilos.ponto} style={{ fill: s.cor }} />
              ))}
              {varias && ultimo && (
                <text x={ultimo.x + 8} y={ultimo.y} className={estilos.rotuloDireto}>
                  {numero(ultimo.v, medida.casas)}
                </text>
              )}
            </g>
          );
        })}
        {datas.map((d, i) => (
          <rect
            key={`alvo-${d}-${i}`}
            x={x(i) - passo / 2}
            y={MARGEM.topo}
            width={datas.length > 1 ? passo : areaLargura}
            height={areaAltura}
            className={graficos.alvo}
            tabIndex={0}
            aria-label={`${dia(d)}: ${medida.series.map((s) => `${s.nome} ${numero(s.valores[i], medida.casas)} ${medida.unidade}`).join(", ")}`}
            onMouseEnter={() => setFoco(i)}
            onFocus={() => setFoco(i)}
            onBlur={() => setFoco(null)}
          />
        ))}
      </svg>
      {foco !== null && (
        <div
          className={graficos.dica}
          style={{ left: `${(x(foco) / LARGURA) * 100}%` }}
          data-lado={foco > (datas.length - 1) / 2 ? "esquerda" : "direita"}
          role="tooltip"
        >
          <strong>Triagem de {formatarInstante(datas[foco])}</strong>
          {medida.series.map((s) => (
            <span key={s.nome} className={graficos.linhaDica}>
              <span className={estilos.amostra} style={{ background: s.cor }} aria-hidden="true" />
              {s.nome}
              <b>
                {numero(s.valores[foco], medida.casas)} {medida.unidade}
              </b>
            </span>
          ))}
        </div>
      )}
    </div>
  );
}

function ultimoValor(medida: Medida): string {
  const valores = medida.series.map((s) => [...s.valores].reverse().find((v) => v !== null) ?? null);
  return `${valores.map((v) => numero(v, medida.casas)).join("/")} ${medida.unidade}`;
}

export function SinaisVitaisNoTempo({ ramos }: { ramos: RamoDoHistorico[] }) {
  const lista: Medicao[] = ramos
    .filter((r) => r.acessoClinico && r.triagem.sinaisVitais)
    .map((r) => ({ quando: r.triagem.chegadaEm, sinais: r.triagem.sinaisVitais as SinaisVitais }))
    .sort((a, b) => a.quando.localeCompare(b.quando));
  const todas = medidas(lista);
  const datas = lista.map((m) => m.quando);

  if (todas.length === 0) return null;

  return (
    <div className={estilos.sinais}>
      <ul className={estilos.grade}>
        {todas.map((m) => (
          <li key={m.titulo} className={estilos.cartao}>
            <div className={estilos.topo}>
              <h3>{m.titulo}</h3>
              <span className={estilos.valor}>
                {ultimoValor(m)}
                <small>última medida</small>
              </span>
            </div>
            {m.series.length > 1 && (
              <ul className={graficos.legenda}>
                {m.series.map((s) => (
                  <li key={s.nome}>
                    <span className={graficos.amostra} style={{ background: s.cor }} aria-hidden="true" />
                    {s.nome}
                  </li>
                ))}
              </ul>
            )}
            {datas.length > 1 ? (
              <Grafico medida={m} datas={datas} />
            ) : (
              <p className={estilos.aviso}>O gráfico aparece a partir da segunda triagem.</p>
            )}
          </li>
        ))}
      </ul>
      <details className={estilos.tabela}>
        <summary>Ver em tabela</summary>
        <div className={estilos.rolagem}>
          <table>
            <thead>
              <tr>
                <th scope="col">Triagem</th>
                {todas.flatMap((m) =>
                  m.series.map((s) => (
                    <th key={`${m.titulo}-${s.nome}`} scope="col">
                      {m.series.length > 1 ? `${m.titulo} (${s.nome.toLowerCase()})` : m.titulo}
                    </th>
                  )),
                )}
              </tr>
            </thead>
            <tbody>
              {datas.map((d, i) => (
                <tr key={`${d}-${i}`}>
                  <th scope="row">{formatarInstante(d)}</th>
                  {todas.flatMap((m) =>
                    m.series.map((s) => (
                      <td key={`${m.titulo}-${s.nome}`}>{s.valores[i] === null ? "—" : `${numero(s.valores[i], m.casas)} ${m.unidade}`}</td>
                    )),
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </details>
    </div>
  );
}
