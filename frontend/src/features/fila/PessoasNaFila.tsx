import { DoorOpen, UserRound } from "lucide-react";
import type { CSSProperties } from "react";
import { classes } from "@/shared/lib/classes";
import { Dica } from "./Dica";
import { ordinal } from "./rotulos";
import type { Estimativa } from "./estimativa";
import estilos from "./JornadaNaFila.module.css";

const MAX_A_FRENTE = 18;
const MAX_ATRAS = 5;

type Grupo = { de: number; ate: number; urgente: boolean };

function agrupar(primeira: number, ultima: number, maximo: number, maisUrgentes: number): Grupo[] {
  const quantidade = ultima - primeira + 1;
  if (quantidade <= 0) return [];
  const tamanho = Math.ceil(quantidade / maximo);
  const grupos: Grupo[] = [];
  for (let fim = ultima; fim >= primeira; fim -= tamanho) {
    const de = Math.max(primeira, fim - tamanho + 1);
    grupos.push({ de, ate: fim, urgente: de <= maisUrgentes });
  }
  return grupos;
}

function quem(g: Grupo, posicao: number) {
  const pessoas = g.ate - g.de + 1;
  const onde = pessoas === 1 ? `${ordinal(g.de)} lugar` : `${ordinal(g.de)} a ${ordinal(g.ate)} lugar · ${pessoas} pessoas`;
  if (g.de > posicao) return { onde, motivo: "Chegaram depois ou têm prioridade clínica menor" };
  return { onde, motivo: g.urgente ? "Prioridade clínica maior que a sua" : "Mesma prioridade, entraram antes de você" };
}

function atraso(indice: number): CSSProperties {
  return { animationDelay: `${indice * 45}ms` };
}

export function PessoasNaFila({ posicao, total, estimativa }: { posicao: number; total: number; estimativa: Estimativa }) {
  const aFrente = agrupar(1, posicao - 1, MAX_A_FRENTE, estimativa.maisUrgentes);
  const atras = agrupar(posicao + 1, total, MAX_ATRAS, 0);
  let indice = 0;

  return (
    <div className={estilos.pessoas} role="img" aria-label={`Você é a pessoa ${posicao} de ${total} nesta fila`}>
      <div className={estilos.faixa}>
        {atras.map((g) => {
          const d = quem(g, posicao);
          return (
            <Dica
              key={`atras-${g.de}`}
              texto={
                <>
                  <strong>{d.onde}</strong>
                  {d.motivo}
                </>
              }
            >
              <span className={classes(estilos.pessoa, estilos.atras)} style={atraso(indice++)}>
                <UserRound size={13} aria-hidden="true" />
                {g.ate > g.de && <span className={estilos.multiplo}>{g.ate - g.de + 1}</span>}
              </span>
            </Dica>
          );
        })}
        <Dica
          texto={
            <>
              <strong>
                Você · {ordinal(posicao)} de {total}
              </strong>
              {estimativa.aFrente === 0 ? "Você é a próxima pessoa a receber uma vaga" : `${estimativa.aFrente} na sua frente`}
            </>
          }
        >
          <span className={estilos.voce} style={atraso(indice++)}>
            <span className={estilos.pulso} aria-hidden="true" />
            <UserRound size={18} aria-hidden="true" />
            <span className={estilos.rotuloVoce}>Você</span>
          </span>
        </Dica>
        {aFrente.map((g) => {
          const d = quem(g, posicao);
          return (
            <Dica
              key={`frente-${g.de}`}
              texto={
                <>
                  <strong>{d.onde}</strong>
                  {d.motivo}
                </>
              }
            >
              <span className={classes(estilos.pessoa, g.urgente ? estilos.urgente : estilos.mesmoNivel)} style={atraso(indice++)}>
                <UserRound size={13} aria-hidden="true" />
                {g.ate > g.de && <span className={estilos.multiplo}>{g.ate - g.de + 1}</span>}
              </span>
            </Dica>
          );
        })}
        <Dica
          texto={
            <>
              <strong>Próxima vaga</strong>Quando alguém desmarca, o horário é oferecido aqui, da direita para a esquerda
            </>
          }
        >
          <span className={estilos.porta} style={atraso(indice++)}>
            <DoorOpen size={18} aria-hidden="true" />
          </span>
        </Dica>
      </div>
      <ul className={estilos.legenda}>
        {estimativa.maisUrgentes > 0 && (
          <li>
            <span className={classes(estilos.amostra, estilos.urgente)} /> Prioridade maior ({estimativa.maisUrgentes})
          </li>
        )}
        {estimativa.mesmoNivel > 0 && (
          <li>
            <span className={classes(estilos.amostra, estilos.mesmoNivel)} /> Mesma prioridade ({estimativa.mesmoNivel})
          </li>
        )}
        <li>
          <span className={classes(estilos.amostra, estilos.amostraVoce)} /> Você
        </li>
        {estimativa.atras > 0 && (
          <li>
            <span className={classes(estilos.amostra, estilos.atras)} /> Depois de você ({estimativa.atras})
          </li>
        )}
      </ul>
    </div>
  );
}
