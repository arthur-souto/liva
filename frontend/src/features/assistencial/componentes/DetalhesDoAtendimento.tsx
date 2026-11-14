"use client";

import { mensagemDeErro } from "@/shared/api/erro";
import { useConsulta } from "@/shared/hooks/useConsulta";
import { formatarDataHora, formatarInstante } from "@/shared/lib/datas";
import { Alerta } from "@/shared/ui/Alerta";
import { Carregando } from "@/shared/ui/Carregando";
import { detalharTriagem } from "../api";
import { NATUREZAS, nomeDaEspecialidade } from "../rotulos";
import { CAMPOS_DA_AVALIACAO, sinais } from "./ResumoDoRamo";
import estilos from "./DetalhesDoAtendimento.module.css";

export function DetalhesDoAtendimento({ triagemId }: { triagemId: string }) {
  const ramo = useConsulta(() => detalharTriagem(triagemId), `detalhes-${triagemId}`);

  if (ramo.erro) return <Alerta tom="perigo">{mensagemDeErro(ramo.erro)}</Alerta>;
  if (!ramo.dados) return <Carregando linhas={3} />;

  const { triagem, encaminhamentos, consultas, retificacoes, evolucoes } = ramo.dados;
  const vitais = sinais(triagem.sinaisVitais);
  const campos = CAMPOS_DA_AVALIACAO.filter(([campo]) => triagem.avaliacao?.[campo]);
  const motivos = encaminhamentos.filter((e) => e.justificativa);
  const anotadas = consultas
    .map((c) => ({ consulta: c, anotacoes: evolucoes.filter((ev) => ev.agendamentoId === c.id) }))
    .filter((c) => c.anotacoes.length > 0)
    .sort((a, b) => b.consulta.inicio.localeCompare(a.consulta.inicio));
  const especialidadeDe = (encaminhamentoId: string | null) => {
    const e = encaminhamentos.find((x) => x.id === encaminhamentoId);
    return e ? nomeDaEspecialidade(e.especialidade) : null;
  };
  const vazio =
    campos.length === 0 &&
    vitais.length === 0 &&
    !triagem.justificativaPrioridade &&
    retificacoes.length === 0 &&
    motivos.length === 0 &&
    anotadas.length === 0;

  if (vazio) return <p className={estilos.vazio}>Ainda não há registros da equipe neste atendimento.</p>;

  return (
    <div className={estilos.detalhes}>
      {(campos.length > 0 || vitais.length > 0 || triagem.justificativaPrioridade) && (
        <section className={estilos.bloco}>
          <h3>Na triagem</h3>
          {campos.length > 0 && (
            <dl className={estilos.campos}>
              {campos.map(([campo, rotulo]) => (
                <div key={campo}>
                  <dt>{rotulo}</dt>
                  <dd>{triagem.avaliacao?.[campo]}</dd>
                </div>
              ))}
            </dl>
          )}
          {vitais.length > 0 && (
            <ul className={estilos.vitais} aria-label="Sinais vitais">
              {vitais.map(([rotulo, valor]) => (
                <li key={rotulo}>
                  <span>{rotulo}</span>
                  <strong>{valor}</strong>
                </li>
              ))}
            </ul>
          )}
          {triagem.justificativaPrioridade && (
            <p className={estilos.texto}>
              <span className={estilos.rotulo}>Por que essa prioridade: </span>
              {triagem.justificativaPrioridade}
            </p>
          )}
        </section>
      )}

      {retificacoes.length > 0 && (
        <section className={estilos.bloco}>
          <h3>Correções feitas pela equipe</h3>
          {retificacoes.map((r) => (
            <p key={r.registradaEm} className={estilos.texto}>
              <span className={estilos.meta}>{formatarInstante(r.registradaEm)} · </span>
              {r.texto}
            </p>
          ))}
        </section>
      )}

      {motivos.length > 0 && (
        <section className={estilos.bloco}>
          <h3>Por que você foi encaminhado</h3>
          {motivos.map((e) => (
            <p key={e.id} className={estilos.texto}>
              <span className={estilos.rotulo}>{nomeDaEspecialidade(e.especialidade)}: </span>
              {e.justificativa}
            </p>
          ))}
        </section>
      )}

      {anotadas.length > 0 && (
        <section className={estilos.bloco}>
          <h3>Anotações do profissional</h3>
          <ul className={estilos.anotacoes}>
            {anotadas.map(({ consulta, anotacoes }) => (
              <li key={consulta.id}>
                <span className={estilos.rotulo}>
                  {[especialidadeDe(consulta.encaminhamentoId), NATUREZAS[consulta.natureza], formatarDataHora(consulta.inicio)]
                    .filter(Boolean)
                    .join(" · ")}
                </span>
                {anotacoes.map((ev) => (
                  <p key={ev.registradaEm} className={estilos.texto}>
                    {ev.texto}
                  </p>
                ))}
              </li>
            ))}
          </ul>
        </section>
      )}
    </div>
  );
}
