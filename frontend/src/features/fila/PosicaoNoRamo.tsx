import { CalendarClock, PauseCircle, Users } from "lucide-react";
import { NATUREZAS, PRIORIDADES } from "@/features/assistencial/rotulos";
import { classes } from "@/shared/lib/classes";
import { formatarDataHora, formatarInstante } from "@/shared/lib/datas";
import { Etiqueta } from "@/shared/ui/Etiqueta";
import { STATUS_DA_ENTRADA, ordinal } from "./rotulos";
import type { PosicaoNaFila } from "./tipos";
import estilos from "./PosicaoNoRamo.module.css";

function avanco(posicao: number, total: number): number {
  if (total <= 1) return 100;
  return Math.round(((total - posicao) / (total - 1)) * 100);
}

export function PosicaoNoRamo({ fila }: { fila: PosicaoNaFila }) {
  const status = STATUS_DA_ENTRADA[fila.status];
  const naFila = fila.status === "AGUARDANDO" && fila.posicao !== null && fila.total !== null;
  const progresso = naFila ? avanco(fila.posicao!, fila.total!) : 0;

  return (
    <article className={classes(estilos.ramo, !naFila && estilos.inativo)}>
      <header className={estilos.cabecalho}>
        <div>
          <h3 className={estilos.titulo}>
            {fila.nomeEspecialidade} <span>— Triagem #{String(fila.triagemNumero).padStart(3, "0")}</span>
          </h3>
          <p className={estilos.meta}>
            Triagem de {formatarInstante(fila.triagemEm)}
            {fila.natureza === "RETORNO" && ` · ${NATUREZAS.RETORNO}`}
            {fila.profissional && ` · ${fila.profissional}`}
          </p>
        </div>
        <div className={estilos.etiquetas}>
          <Etiqueta tom={PRIORIDADES[fila.prioridade].tom}>{PRIORIDADES[fila.prioridade].texto}</Etiqueta>
          <Etiqueta tom={status.tom}>{status.texto}</Etiqueta>
        </div>
      </header>

      {naFila && (
        <div className={estilos.posicao}>
          <p className={estilos.numero}>
            <strong>{ordinal(fila.posicao!)}</strong>
            <span>
              <Users size={14} aria-hidden="true" /> de {fila.total} {fila.total === 1 ? "pessoa" : "pessoas"} nesta fila
            </span>
          </p>
          <div
            className={estilos.trilha}
            role="progressbar"
            aria-label={`Posição ${fila.posicao} de ${fila.total}`}
            aria-valuemin={1}
            aria-valuemax={fila.total!}
            aria-valuenow={fila.total! - fila.posicao! + 1}
          >
            <span className={estilos.preenchimento} style={{ width: `${progresso}%` }} />
            <span className={estilos.marcador} style={{ left: `${progresso}%` }} aria-hidden="true" />
          </div>
          <div className={estilos.extremos} aria-hidden="true">
            <span>Fim da fila</span>
            <span>Próxima vaga</span>
          </div>
        </div>
      )}

      {fila.status === "PAUSADA" && (
        <p className={estilos.aviso}>
          <PauseCircle size={16} aria-hidden="true" />A equipe pausou esta fila{fila.motivo ? `: ${fila.motivo}` : "."} Quando retomar, você
          volta para a mesma posição.
        </p>
      )}

      <dl className={estilos.datas}>
        <div>
          <dt>Na fila desde</dt>
          <dd>{formatarInstante(fila.entrouEm)}</dd>
        </div>
        <div>
          <dt>
            <CalendarClock size={14} aria-hidden="true" /> Consulta marcada
          </dt>
          <dd>{fila.consultaAtual ? formatarDataHora(fila.consultaAtual) : "Ainda sem horário"}</dd>
        </div>
      </dl>
    </article>
  );
}
