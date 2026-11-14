"use client";

import { useState } from "react";
import { mensagemDeErro } from "@/shared/api/erro";

export function useAcao() {
  const [executando, setExecutando] = useState<string | null>(null);
  const [falha, setFalha] = useState<string | null>(null);

  async function executar<T>(nome: string, acao: () => Promise<T>): Promise<T | undefined> {
    setExecutando(nome);
    setFalha(null);
    try {
      return await acao();
    } catch (erro) {
      setFalha(mensagemDeErro(erro));
      return undefined;
    } finally {
      setExecutando(null);
    }
  }

  return { executando, falha, executar, limparFalha: () => setFalha(null) };
}
