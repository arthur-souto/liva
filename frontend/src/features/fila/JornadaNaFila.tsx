import { CalendarClock, Gauge, Users } from "lucide-react";
import { PRIORIDADES } from "@/features/assistencial/rotulos";
import { Etiqueta } from "@/shared/ui/Etiqueta";
import { Dica } from "./Dica";
import { estimar, relativo, dataCurta } from "./estimativa";
import { LinhaDoTempo } from "./LinhaDoTempo";
import { MedidorDeEspera } from "./MedidorDeEspera";
import { PessoasNaFila } from "./PessoasNaFila";
import { STATUS_DA_ENTRADA, ordinal } from "./rotulos";
import type { PosicaoNaFila } from "./tipos";
import estilos from "./JornadaNaFila.module.css";

export function JornadaNaFila({ fila, agora }: { fila: PosicaoNaFila; agora: number }) {
  const estimativa = estimar(fila, agora);
  const posicao = fila.posicao ?? 1;
  const total = fila.total ?? 1;
  const consultaAntes = estimativa.consulta !== null && (estimativa.previsao === null || estimativa.consulta <= estimativa.previsao);

  return (
    <article className={estilos.jornada}>
      <header className={estilos.cabecalho}>
        <div>
          <h3 className={estilos.titulo}>
            {fila.nomeEspecialidade} <span>— Triagem #{String(fila.triagemNumero).padStart(3, "0")}</span>
          </h3>
          {fila.profissional && <p className={estilos.meta}>{fila.profissional}</p>}
        </div>
        <div className={estilos.etiquetas}>
          <Etiqueta tom={PRIORIDADES[fila.prioridade].tom}>{PRIORIDADES[fila.prioridade].texto}</Etiqueta>
          <Etiqueta tom={STATUS_DA_ENTRADA[fila.status].tom}>{STATUS_DA_ENTRADA[fila.status].texto}</Etiqueta>
        </div>
      </header>

      <div className={estilos.destaque}>
        <MedidorDeEspera estimativa={estimativa} />
        <div className={estilos.numeros}>
          <p className={estilos.lugar}>
            <strong>{ordinal(posicao)}</strong> de {total} na fila
          </p>
          <ul className={estilos.fatos}>
            <li>
              <Users size={16} aria-hidden="true" />
              {estimativa.aFrente === 0 ? (
                "Você é a próxima pessoa a receber uma vaga"
              ) : (
                <span>
                  {estimativa.aFrente} {estimativa.aFrente === 1 ? "pessoa" : "pessoas"} na sua frente{" "}
                  {estimativa.maisUrgentes > 0 && (
                    <Dica texto="A triagem definiu uma prioridade clínica maior para elas, por isso passam na frente">
                      <span className={estilos.sublinhado}>({estimativa.maisUrgentes} com prioridade maior)</span>
                    </Dica>
                  )}
                </span>
              )}
            </li>
            <li>
              <Gauge size={16} aria-hidden="true" />
              {estimativa.vagasPorSemana === null
                ? "Ainda sem vagas recentes para estimar o ritmo"
                : `Cerca de ${estimativa.vagasPorSemana.toLocaleString("pt-BR", { maximumFractionDigits: 1 })} vagas por semana nesta fila`}
            </li>
            {estimativa.consulta !== null && (
              <li>
                <CalendarClock size={16} aria-hidden="true" />
                Consulta marcada para {dataCurta(estimativa.consulta)} ({relativo(estimativa.consulta, agora)})
              </li>
            )}
          </ul>
          {consultaAntes && (
            <p className={estilos.observacao}>Sua consulta já está marcada. A fila serve para antecipar se surgir um horário antes.</p>
          )}
        </div>
      </div>

      <PessoasNaFila posicao={posicao} total={total} estimativa={estimativa} />
      <LinhaDoTempo triagemEm={fila.triagemEm} entrouEm={fila.entrouEm} agora={agora} estimativa={estimativa} />
      <p className={estilos.aviso}>Passe o mouse ou toque nos itens para ver os detalhes. A previsão é uma estimativa e pode mudar.</p>
    </article>
  );
}
