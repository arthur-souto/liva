"use client";

import { useEffect, useId, useState } from "react";
import { Dica } from "./Dica";
import { type Estimativa, dataCurta, descreverDias, resumirDias } from "./estimativa";
import estilos from "./JornadaNaFila.module.css";

const RAIO = 52;
const CIRCUNFERENCIA = 2 * Math.PI * RAIO;

export function MedidorDeEspera({ estimativa }: { estimativa: Estimativa }) {
  const [montado, setMontado] = useState(false);
  const gradiente = useId();
  const progresso = estimativa.progresso ?? 0;
  const restante = estimativa.diasRestantes === null ? null : resumirDias(estimativa.diasRestantes);

  useEffect(() => {
    const id = window.requestAnimationFrame(() => setMontado(true));
    return () => window.cancelAnimationFrame(id);
  }, []);

  return (
    <Dica
      className={estilos.medidor}
      texto={
        estimativa.diasRestantes === null ? (
          <>
            <strong>Sem estimativa ainda</strong>Esta fila ainda não teve vagas nas últimas 4 semanas para calcularmos o ritmo
          </>
        ) : (
          <>
            <strong>Previsão: por volta de {dataCurta(estimativa.previsao!)}</strong>
            Na fila há {descreverDias(estimativa.diasNaFila)}. Faltam cerca de {descreverDias(estimativa.diasRestantes)}, no ritmo de{" "}
            {estimativa.vagasPorSemana!.toLocaleString("pt-BR", { maximumFractionDigits: 1 })} vagas por semana
          </>
        )
      }
    >
      <svg viewBox="0 0 120 120" className={estilos.anel} aria-hidden="true">
        <defs>
          <linearGradient id={gradiente} x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stopColor="var(--cor-roxo-300)" />
            <stop offset="100%" stopColor="var(--cor-destaque)" />
          </linearGradient>
        </defs>
        <circle cx="60" cy="60" r={RAIO} className={estilos.trilhaAnel} />
        <circle
          cx="60"
          cy="60"
          r={RAIO}
          className={estilos.progressoAnel}
          stroke={`url(#${gradiente})`}
          strokeDasharray={CIRCUNFERENCIA}
          strokeDashoffset={CIRCUNFERENCIA * (1 - (montado ? progresso : 0))}
        />
      </svg>
      <span className={estilos.centroAnel}>
        {restante ? (
          <>
            <span className={estilos.aprox}>cerca de</span>
            <strong>{restante.valor}</strong>
            <span>{restante.unidade}</span>
          </>
        ) : (
          <>
            <strong className={estilos.semValor}>—</strong>
            <span>sem estimativa</span>
          </>
        )}
      </span>
    </Dica>
  );
}
