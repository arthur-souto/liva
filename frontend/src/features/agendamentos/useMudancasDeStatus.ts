"use client";

import { useEffect, useRef, useState } from "react";
import type { AgendamentoDaEquipe, StatusAgendamento } from "./tipos";

const DURACAO_DO_DESTAQUE = 4000;

export function useMudancasDeStatus(agendamentos: AgendamentoDaEquipe[]): Set<string> {
  const anteriores = useRef<Map<string, StatusAgendamento> | null>(null);
  const temporizador = useRef<number | undefined>(undefined);
  const [destacados, setDestacados] = useState<Set<string>>(() => new Set());

  useEffect(() => {
    const atuais = new Map(agendamentos.map((a) => [a.id, a.status]));
    const antes = anteriores.current;
    anteriores.current = atuais;
    if (!antes) return;
    const mudaram = agendamentos.filter((a) => antes.has(a.id) && antes.get(a.id) !== a.status).map((a) => a.id);
    if (mudaram.length === 0) return;
    setDestacados(new Set(mudaram));
    window.clearTimeout(temporizador.current);
    temporizador.current = window.setTimeout(() => setDestacados(new Set()), DURACAO_DO_DESTAQUE);
  }, [agendamentos]);

  useEffect(() => () => window.clearTimeout(temporizador.current), []);

  return destacados;
}
