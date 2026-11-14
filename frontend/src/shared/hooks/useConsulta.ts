"use client";

import { useCallback, useEffect, useRef, useState } from "react";

type Estado<T> = {
  dados: T | null;
  erro: unknown;
  carregando: boolean;
  atualizadoEm: number | null;
};

export function useConsulta<T>(carregar: () => Promise<T>, chave: string, intervaloMs?: number) {
  const [estado, setEstado] = useState<Estado<T>>({ dados: null, erro: null, carregando: true, atualizadoEm: null });
  const carregarAtual = useRef(carregar);
  const geracao = useRef(0);

  useEffect(() => {
    carregarAtual.current = carregar;
  });

  const executar = useCallback(async () => {
    const atual = ++geracao.current;
    try {
      const dados = await carregarAtual.current();
      if (atual === geracao.current) setEstado({ dados, erro: null, carregando: false, atualizadoEm: Date.now() });
    } catch (erro) {
      if (atual === geracao.current) setEstado((anterior) => ({ ...anterior, erro, carregando: false }));
    }
  }, []);

  useEffect(() => {
    setEstado((anterior) => ({ ...anterior, carregando: true }));
    executar();
    if (!intervaloMs) return;
    const id = window.setInterval(executar, intervaloMs);
    return () => window.clearInterval(id);
  }, [chave, intervaloMs, executar]);

  return { ...estado, recarregar: executar };
}
