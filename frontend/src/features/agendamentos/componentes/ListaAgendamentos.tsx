"use client";

import { dataDe, rotuloDoDia } from "@/shared/lib/datas";
import type { AgendamentoDaEquipe } from "../tipos";
import { useMudancasDeStatus } from "../useMudancasDeStatus";
import { ItemAgendamento } from "./ItemAgendamento";
import estilos from "./ListaAgendamentos.module.css";

type Props = {
  agendamentos: AgendamentoDaEquipe[];
  agruparPorDia?: boolean;
};

function agrupar(agendamentos: AgendamentoDaEquipe[]): [string, AgendamentoDaEquipe[]][] {
  const grupos = new Map<string, AgendamentoDaEquipe[]>();
  agendamentos.forEach((a) => grupos.set(dataDe(a.dataHora), [...(grupos.get(dataDe(a.dataHora)) ?? []), a]));
  return [...grupos.entries()];
}

export function ListaAgendamentos({ agendamentos, agruparPorDia = false }: Props) {
  const destacados = useMudancasDeStatus(agendamentos);

  const itens = (lista: AgendamentoDaEquipe[]) => (
    <ul className={estilos.lista}>
      {lista.map((a) => (
        <li key={a.id}>
          <ItemAgendamento agendamento={a} destacado={destacados.has(a.id)} />
        </li>
      ))}
    </ul>
  );

  if (!agruparPorDia) return itens(agendamentos);

  return (
    <div className={estilos.grupos}>
      {agrupar(agendamentos).map(([dia, lista]) => (
        <section key={dia} className={estilos.grupo} aria-label={rotuloDoDia(dia)}>
          <h3 className={estilos.dia}>{rotuloDoDia(dia)}</h3>
          {itens(lista)}
        </section>
      ))}
    </div>
  );
}
