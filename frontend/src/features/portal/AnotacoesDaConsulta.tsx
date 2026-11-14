"use client";

import { detalharTriagem } from "@/features/assistencial/api";
import { mensagemDeErro } from "@/shared/api/erro";
import { useConsulta } from "@/shared/hooks/useConsulta";
import { Alerta } from "@/shared/ui/Alerta";
import { Carregando } from "@/shared/ui/Carregando";
import estilos from "./CartaoDeConsulta.module.css";

export function AnotacoesDaConsulta({ triagemId, consultaId }: { triagemId: string; consultaId: string }) {
  const ramo = useConsulta(() => detalharTriagem(triagemId), `detalhes-${triagemId}`);

  if (ramo.erro) return <Alerta tom="perigo">{mensagemDeErro(ramo.erro)}</Alerta>;
  if (!ramo.dados) return <Carregando linhas={2} />;

  const anotacoes = ramo.dados.evolucoes.filter((ev) => ev.agendamentoId === consultaId);
  if (anotacoes.length === 0) return <p className={estilos.semAnotacoes}>O profissional não deixou anotações nesta consulta.</p>;

  return (
    <div className={estilos.anotacoes}>
      {anotacoes.map((ev) => (
        <p key={ev.registradaEm}>{ev.texto}</p>
      ))}
    </div>
  );
}
