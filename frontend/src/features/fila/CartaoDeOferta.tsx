"use client";

import { ArrowRight, CalendarCheck, Clock, MapPin, Repeat, Sparkles, Stethoscope, X } from "lucide-react";
import { useState } from "react";
import type { Consulta } from "@/features/assistencial/tipos";
import { useAcao } from "@/shared/hooks/useAcao";
import { classes } from "@/shared/lib/classes";
import { dataDe, formatarDataCurta, formatarDiaLongo, horaDe } from "@/shared/lib/datas";
import { Alerta } from "@/shared/ui/Alerta";
import { Botao } from "@/shared/ui/Botao";
import { aceitarOferta, recusarOferta } from "./api";
import type { OfertaParaPaciente } from "./tipos";
import { descreverRestante, tempoRestante } from "./useRelogioDoServidor";
import estilos from "./CartaoDeOferta.module.css";

type Props = {
  oferta: OfertaParaPaciente;
  agora: number;
  aoAceitar: (consulta: Consulta) => void;
  aoRecusar: () => void;
};

const URGENTE_MS = 15 * 60 * 1000;
const DIA_MS = 24 * 60 * 60 * 1000;

function diasEntre(de: string, ate: string): number {
  return Math.round((Date.parse(`${dataDe(ate)}T00:00:00Z`) - Date.parse(`${dataDe(de)}T00:00:00Z`)) / DIA_MS);
}

function descreverAntecipacao(dias: number): string {
  if (dias <= 0) return "No mesmo dia, mais cedo";
  return dias === 1 ? "1 dia antes" : `${dias} dias antes`;
}

export function CartaoDeOferta({ oferta, agora, aoAceitar, aoRecusar }: Props) {
  const { executando, falha, executar } = useAcao();
  const [confirmandoRecusa, setConfirmandoRecusa] = useState(false);
  const restante = tempoRestante(oferta.expiraEm, agora);
  const expirou = restante === 0;

  async function aceitar() {
    const consulta = await executar("aceitar", () => aceitarOferta(oferta.id));
    if (consulta) aoAceitar(consulta);
  }

  async function recusar() {
    const feito = await executar("recusar", async () => {
      await recusarOferta(oferta.id);
      return true;
    });
    if (feito) aoRecusar();
  }

  return (
    <article className={estilos.oferta} aria-labelledby={`oferta-${oferta.id}`}>
      <header className={estilos.topo}>
        <span className={estilos.selo} aria-hidden="true">
          <Sparkles size={20} />
        </span>
        <div>
          <h2 id={`oferta-${oferta.id}`} className={estilos.titulo}>
            Uma vaga ficou disponível para você!
          </h2>
          <p className={estilos.subtitulo}>{oferta.nomeEspecialidade}</p>
        </div>
        <div
          className={classes(estilos.contagem, restante < URGENTE_MS && estilos.urgente)}
          role="timer"
          aria-live="off"
          aria-label={expirou ? "Prazo encerrado" : `Responda em ${descreverRestante(restante)}`}
        >
          <Clock size={16} aria-hidden="true" />
          {expirou ? "Prazo encerrado" : descreverRestante(restante)}
        </div>
      </header>

      <dl className={estilos.detalhes}>
        <div>
          <dt>
            <CalendarCheck size={16} aria-hidden="true" /> Novo horário
          </dt>
          <dd>
            {formatarDiaLongo(oferta.inicio)} · {horaDe(oferta.inicio)}
          </dd>
        </div>
        {oferta.profissional && (
          <div>
            <dt>
              <Stethoscope size={16} aria-hidden="true" /> Profissional
            </dt>
            <dd>{oferta.profissional}</dd>
          </div>
        )}
        <div>
          <dt>
            <MapPin size={16} aria-hidden="true" /> Local
          </dt>
          <dd>
            {oferta.unidade}
            <span>{oferta.endereco}</span>
          </dd>
        </div>
      </dl>

      {oferta.antecipaDe && (
        <section className={estilos.troca} aria-label="Troca de consulta">
          <p className={estilos.trocaTitulo}>
            <Repeat size={16} aria-hidden="true" />
            Ao aceitar, sua consulta de {formatarDataCurta(oferta.antecipaDe)} passa para este horário
          </p>
          <div className={estilos.comparacao}>
            <div className={estilos.horarioAntigo}>
              <span>Consulta atual</span>
              <strong>
                {formatarDataCurta(oferta.antecipaDe)} · {horaDe(oferta.antecipaDe)}
              </strong>
            </div>
            <ArrowRight size={20} aria-hidden="true" className={estilos.seta} />
            <div className={estilos.horarioNovo}>
              <span>{descreverAntecipacao(diasEntre(oferta.inicio, oferta.antecipaDe))}</span>
              <strong>
                {formatarDataCurta(oferta.inicio)} · {horaDe(oferta.inicio)}
              </strong>
            </div>
          </div>
          <p className={estilos.trocaNota}>Você continua com uma consulta só. O horário antigo fica livre para outra pessoa da fila.</p>
        </section>
      )}

      {falha && <Alerta tom="perigo">{falha}</Alerta>}

      {confirmandoRecusa ? (
        <div className={estilos.confirmacao}>
          <p>
            Tudo bem. {oferta.antecipaDe && "Sua consulta marcada continua igual. "}Você continua na fila na mesma posição e a vaga vai para
            a próxima pessoa.
          </p>
          <div className={estilos.acoes}>
            <Botao variante="contorno" onClick={() => setConfirmandoRecusa(false)} disabled={executando !== null}>
              Voltar
            </Botao>
            <Botao variante="perigo" onClick={recusar} carregando={executando === "recusar"}>
              Confirmar que não posso
            </Botao>
          </div>
        </div>
      ) : (
        <div className={estilos.acoes}>
          <Botao
            variante="contorno"
            icone={<X size={16} aria-hidden="true" />}
            onClick={() => setConfirmandoRecusa(true)}
            disabled={expirou || executando !== null}
          >
            Não posso neste horário
          </Botao>
          <Botao
            icone={<CalendarCheck size={16} aria-hidden="true" />}
            onClick={aceitar}
            carregando={executando === "aceitar"}
            disabled={expirou}
          >
            {oferta.antecipaDe ? "Trocar para este horário" : "Aceitar vaga"}
          </Botao>
        </div>
      )}
      <p className={estilos.nota}>Recusar não tira você da fila. Também dá para responder pelo WhatsApp.</p>
    </article>
  );
}
