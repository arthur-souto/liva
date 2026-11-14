import { formatarDataHora, formatarInstante } from "@/shared/lib/datas";
import { Etiqueta } from "@/shared/ui/Etiqueta";
import { MODALIDADES, NATUREZAS, PRIORIDADES, STATUS_DA_CONSULTA, STATUS_DA_TRIAGEM, STATUS_DO_ENCAMINHAMENTO, nomeDaEspecialidade } from "../rotulos";
import type { Ramo, SinaisVitais } from "../tipos";
import estilos from "./ResumoDoRamo.module.css";

export const CAMPOS_DA_AVALIACAO = [
  ["queixaPrincipal", "Queixa principal"],
  ["sintomas", "Sintomas"],
  ["historicoClinico", "Histórico clínico"],
  ["antecedentes", "Antecedentes"],
  ["observacoes", "Observações"],
] as const;

export function sinais(s: SinaisVitais | null): [string, string][] {
  if (!s) return [];
  const itens: [string, string | null][] = [
    ["Pressão", s.pressaoSistolica && s.pressaoDiastolica ? `${s.pressaoSistolica}/${s.pressaoDiastolica} mmHg` : null],
    ["Freq. cardíaca", s.frequenciaCardiaca ? `${s.frequenciaCardiaca} bpm` : null],
    ["Freq. respiratória", s.frequenciaRespiratoria ? `${s.frequenciaRespiratoria} irpm` : null],
    ["Temperatura", s.temperatura ? `${String(s.temperatura).replace(".", ",")} °C` : null],
    ["Saturação", s.saturacao ? `${s.saturacao}%` : null],
  ];
  return itens.filter((i): i is [string, string] => i[1] !== null);
}

export function ResumoDoRamo({ ramo, destaque }: { ramo: Ramo; destaque?: string }) {
  const { triagem } = ramo;
  const vitais = sinais(triagem.sinaisVitais);

  return (
    <div className={estilos.ramo}>
      <div className={estilos.etiquetas}>
        <Etiqueta tom={STATUS_DA_TRIAGEM[triagem.status].tom}>{STATUS_DA_TRIAGEM[triagem.status].texto}</Etiqueta>
        {triagem.prioridade && <Etiqueta tom={PRIORIDADES[triagem.prioridade].tom}>{PRIORIDADES[triagem.prioridade].texto}</Etiqueta>}
        <span className={estilos.meta}>Chegada {formatarInstante(triagem.chegadaEm)}</span>
      </div>

      {triagem.justificativaPrioridade && <p className={estilos.texto}>Prioridade: {triagem.justificativaPrioridade}</p>}

      <dl className={estilos.campos}>
        {CAMPOS_DA_AVALIACAO.map(([campo, rotulo]) =>
          triagem.avaliacao?.[campo] ? (
            <div key={campo}>
              <dt>{rotulo}</dt>
              <dd>{triagem.avaliacao[campo]}</dd>
            </div>
          ) : null,
        )}
      </dl>

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

      {ramo.retificacoes.length > 0 && (
        <div className={estilos.bloco}>
          <h3>Retificações</h3>
          {ramo.retificacoes.map((r) => (
            <p key={r.registradaEm} className={estilos.texto}>
              <span className={estilos.meta}>{formatarInstante(r.registradaEm)} · </span>
              {r.texto} <em>({r.justificativa})</em>
            </p>
          ))}
        </div>
      )}

      {ramo.encaminhamentos.length > 0 && (
        <div className={estilos.bloco}>
          <h3>Encaminhamentos</h3>
          <ul className={estilos.lista}>
            {ramo.encaminhamentos.map((e) => (
              <li key={e.id}>
                <div className={estilos.etiquetas}>
                  <strong>{nomeDaEspecialidade(e.especialidade)}</strong>
                  <Etiqueta tom={STATUS_DO_ENCAMINHAMENTO[e.status].tom}>{STATUS_DO_ENCAMINHAMENTO[e.status].texto}</Etiqueta>
                  <span className={estilos.meta}>{MODALIDADES[e.modalidade]}</span>
                </div>
                <p className={estilos.texto}>{e.justificativa}</p>
                {e.desfecho && <p className={estilos.texto}>Desfecho: {e.desfecho}</p>}
              </li>
            ))}
          </ul>
        </div>
      )}

      {ramo.consultas.length > 0 && (
        <div className={estilos.bloco}>
          <h3>Consultas</h3>
          <ul className={estilos.lista}>
            {ramo.consultas.map((c) => (
              <li key={c.id} className={c.id === destaque ? estilos.destacada : undefined}>
                <div className={estilos.etiquetas}>
                  <strong>{formatarDataHora(c.inicio)}</strong>
                  <Etiqueta tom={STATUS_DA_CONSULTA[c.status].tom}>{STATUS_DA_CONSULTA[c.status].texto}</Etiqueta>
                  <span className={estilos.meta}>{NATUREZAS[c.natureza]}</span>
                </div>
                {ramo.evolucoes
                  .filter((ev) => ev.agendamentoId === c.id)
                  .map((ev) => (
                    <p key={ev.registradaEm} className={estilos.texto}>
                      <span className={estilos.meta}>{formatarInstante(ev.registradaEm)} · </span>
                      {ev.texto}
                    </p>
                  ))}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
