"use client";

import { especialistas, meuHistorico } from "@/features/assistencial/api";
import type { Historico } from "@/features/assistencial/tipos";
import { useConsulta } from "@/shared/hooks/useConsulta";

export function usePortal() {
  return useConsulta(meuHistorico, "meu-historico", 60000);
}

function especialidadesDe(historico: Historico | null): string[] {
  if (!historico) return [];
  return [...new Set(historico.ramos.flatMap((r) => r.encaminhamentos.map((e) => e.especialidade)))].sort();
}

async function carregarNomes(especialidades: string[]): Promise<Map<string, string>> {
  const listas = await Promise.all(especialidades.map((e) => especialistas(e).catch(() => [])));
  return new Map(listas.flat().map((p) => [p.id, p.nome]));
}

export function useNomesDosProfissionais(historico: Historico | null): Map<string, string> {
  const especialidades = especialidadesDe(historico);
  const nomes = useConsulta(() => carregarNomes(especialidades), `profissionais-${especialidades.join(",")}`);
  return nomes.dados ?? new Map();
}
