"use client";

import { ArrowDownRight, ArrowUpRight, CalendarCheck, MessageCircleQuestion, Phone, Recycle, ShieldAlert, UserX } from "lucide-react";
import Link from "next/link";
import { useMemo, useState } from "react";
import { STATUS } from "@/features/agendamentos/rotulos";
import { percentual } from "@/features/fila/rotulos";
import { mensagemDeErro } from "@/shared/api/erro";
import { useConsulta } from "@/shared/hooks/useConsulta";
import { classes } from "@/shared/lib/classes";
import { formatarDataHora } from "@/shared/lib/datas";
import { Alerta } from "@/shared/ui/Alerta";
import { AtualizadoHa } from "@/shared/ui/AtualizadoHa";
import { CabecalhoPagina } from "@/shared/ui/CabecalhoPagina";
import { Carregando } from "@/shared/ui/Carregando";
import { EstadoVazio } from "@/shared/ui/EstadoVazio";
import { Etiqueta } from "@/shared/ui/Etiqueta";
import { LinkDeAjuda } from "@/shared/ui/LinkDeAjuda";
import { Secao } from "@/shared/ui/Secao";
import { Segmentado } from "@/shared/ui/Segmentado";
import { painelDeFaltas } from "./api";
import { somar, taxaDeFaltas } from "./calculos";
import { GraficoDeTaxa } from "./GraficoDeTaxa";
import { GraficoSemanal } from "./GraficoSemanal";
import { MOTIVOS, NIVEIS } from "./rotulos";
import { RegistroDeLigacao } from "./RegistroDeLigacao";
import type { ConsultaEmRisco, PainelDeFaltas as Painel } from "./tipos";
import estilos from "./PainelDeFaltas.module.css";

type Periodo = "8" | "12" | "26";

const COMPARACAO = 4;

function variacao(atual: number | null, anterior: number | null): number | null {
  return atual === null || anterior === null ? null : Math.round((atual - anterior) * 100);
}

export function PainelDeFaltas() {
  const [periodo, setPeriodo] = useState<Periodo>("12");
  const [todos, setTodos] = useState(false);
  const consulta = useConsulta(() => painelDeFaltas(Number(periodo)), `faltas|${periodo}`, 60000);
  const p = consulta.dados;

  return (
    <div className={estilos.pagina}>
      <CabecalhoPagina
        titulo="Prevenção de faltas"
        subtitulo="Quem pode faltar nos próximos 7 dias, por que as pessoas faltam e como o comparecimento está evoluindo."
        acoes={
          <>
            <LinkDeAjuda topico="prevencao-de-faltas" />
            <AtualizadoHa atualizadoEm={consulta.atualizadoEm} carregando={consulta.carregando} aoAtualizar={consulta.recarregar} />
          </>
        }
      />

      {consulta.erro ? <Alerta tom="perigo">{mensagemDeErro(consulta.erro)}</Alerta> : null}
      {!p && !consulta.erro && <Carregando linhas={6} />}

      {p && (
        <>
          <Impacto painel={p} />
          <Indicadores painel={p} />

          <Secao
            titulo="Ligar primeiro"
            descricao="Consultas dos próximos 7 dias com maior chance de falta. Uma ligação ou mensagem agora evita o horário vazio."
          >
            <ListaDeRisco consultas={p.emRisco} todos={todos} aoAlternar={() => setTodos((v) => !v)} aoAlterar={consulta.recarregar} />
          </Secao>

          <Secao titulo="Comparecimento por semana" descricao="Passe o mouse em cada semana para ver os números.">
            <div className={estilos.periodo}>
              <Segmentado
                rotulo="Período"
                valor={periodo}
                aoMudar={setPeriodo}
                opcoes={[
                  { valor: "8", rotulo: "8 semanas" },
                  { valor: "12", rotulo: "12 semanas" },
                  { valor: "26", rotulo: "6 meses" },
                ]}
              />
            </div>
            <GraficoSemanal semanas={p.semanas} />
            <h3 className={estilos.subtitulo}>Percentual de faltas</h3>
            <GraficoDeTaxa semanas={p.semanas} />
          </Secao>

          <div className={estilos.duasColunas}>
            <Secao titulo="Por que as pessoas faltam" descricao="Respostas dos últimos 90 dias, pelo WhatsApp e pelo portal.">
              <Motivos painel={p} />
            </Secao>
            <Secao titulo="Faltas por especialidade" descricao="Consultas realizadas no período escolhido.">
              <Especialidades painel={p} />
            </Secao>
          </div>
        </>
      )}
    </div>
  );
}

function Impacto({ painel }: { painel: Painel }) {
  const inicio = somar(painel.semanas.slice(0, COMPARACAO));
  const recentes = somar(painel.semanas.slice(-COMPARACAO));
  const taxaInicial = inicio ? taxaDeFaltas(inicio) : null;
  if (!inicio || !recentes || taxaInicial === null || painel.semanas.length < COMPARACAO * 2) return null;
  const esperadas = taxaInicial * (recentes.compareceram + recentes.faltaram);
  const evitadas = Math.max(0, Math.round(esperadas - recentes.faltaram));
  const total = evitadas + recentes.reaproveitadas;
  if (total === 0) return null;

  return (
    <section className={estilos.impacto} aria-label="Impacto nas últimas 4 semanas">
      <span className={estilos.impactoNumero}>{total}</span>
      <div className={estilos.impactoTexto}>
        <strong>consultas que deixaram de ficar vazias nas últimas 4 semanas</strong>
        <span>
          Estimativa comparada ao ritmo de faltas do início do período ({percentual(taxaInicial)}), somada às vagas liberadas que foram
          ocupadas por quem estava na fila.
        </span>
      </div>
      <ul className={estilos.impactoPartes}>
        <li>
          <b>{evitadas}</b> faltas a menos
        </li>
        <li>
          <b>{recentes.reaproveitadas}</b> vagas reaproveitadas
        </li>
      </ul>
    </section>
  );
}

function Indicadores({ painel }: { painel: Painel }) {
  const recentes = somar(painel.semanas.slice(-COMPARACAO));
  const anteriores = somar(painel.semanas.slice(-COMPARACAO * 2, -COMPARACAO));
  const taxaAtual = recentes ? taxaDeFaltas(recentes) : null;
  const taxaAnterior = anteriores ? taxaDeFaltas(anteriores) : null;
  const delta = variacao(taxaAtual, taxaAnterior);
  const altos = painel.emRisco.filter((c) => c.risco.nivel === "ALTO").length;
  const resposta = painel.perguntasFeitas === 0 ? null : painel.perguntasRespondidas / painel.perguntasFeitas;

  return (
    <div className={estilos.indicadores}>
      <article className={classes(estilos.indicador, estilos.destaque)}>
        <span className={estilos.rotulo}>
          <UserX size={16} aria-hidden="true" /> Faltas nas últimas 4 semanas
        </span>
        <strong className={estilos.valor}>{percentual(taxaAtual)}</strong>
        {delta !== null && (
          <span className={classes(estilos.delta, delta <= 0 ? estilos.melhorou : estilos.piorou)}>
            {delta <= 0 ? <ArrowDownRight size={16} aria-hidden="true" /> : <ArrowUpRight size={16} aria-hidden="true" />}
            {delta <= 0 ? `${Math.abs(delta)} pontos a menos` : `${delta} pontos a mais`} que nas 4 semanas anteriores (
            {percentual(taxaAnterior)})
          </span>
        )}
      </article>
      <article className={estilos.indicador}>
        <span className={estilos.rotulo}>
          <CalendarCheck size={16} aria-hidden="true" /> Compareceram
        </span>
        <strong className={estilos.valor}>{recentes?.compareceram ?? 0}</strong>
        <span className={estilos.dica}>consultas nas últimas 4 semanas</span>
      </article>
      <article className={estilos.indicador}>
        <span className={estilos.rotulo}>
          <Recycle size={16} aria-hidden="true" /> Vagas reaproveitadas
        </span>
        <strong className={estilos.valor}>{recentes?.reaproveitadas ?? 0}</strong>
        <span className={estilos.dica}>de {recentes?.cancelaramAntes ?? 0} cancelamentos com antecedência</span>
      </article>
      <article className={estilos.indicador}>
        <span className={estilos.rotulo}>
          <ShieldAlert size={16} aria-hidden="true" /> Risco alto
        </span>
        <strong className={estilos.valor}>{altos}</strong>
        <span className={estilos.dica}>consultas nos próximos 7 dias</span>
      </article>
      <article className={estilos.indicador}>
        <span className={estilos.rotulo}>
          <MessageCircleQuestion size={16} aria-hidden="true" /> Motivos respondidos
        </span>
        <strong className={estilos.valor}>{percentual(resposta)}</strong>
        <span className={estilos.dica}>
          {painel.perguntasRespondidas} de {painel.perguntasFeitas} perguntas em 90 dias
        </span>
      </article>
    </div>
  );
}

type PropsDaLista = { consultas: ConsultaEmRisco[]; todos: boolean; aoAlternar: () => void; aoAlterar: () => void };

function ListaDeRisco({ consultas, todos, aoAlternar, aoAlterar }: PropsDaLista) {
  const visiveis = useMemo(() => (todos ? consultas : consultas.filter((c) => c.risco.nivel !== "BAIXO")), [consultas, todos]);
  const ocultas = consultas.length - visiveis.length;

  return (
    <div className={estilos.risco}>
      {visiveis.length === 0 ? (
        <EstadoVazio
          icone={<CalendarCheck size={28} aria-hidden="true" />}
          titulo="Nenhuma consulta com risco alto ou médio nos próximos 7 dias"
        />
      ) : (
        <ul className={estilos.listaRisco}>
          {visiveis.map((c, indice) => (
            <li
              key={c.agendamentoId}
              className={classes(estilos.itemRisco, estilos[`nivel${c.risco.nivel}`])}
              style={{ animationDelay: `${indice * 30}ms` }}
            >
              <div className={estilos.medidor} aria-hidden="true">
                <svg viewBox="0 0 36 36">
                  <circle cx="18" cy="18" r="15.5" className={estilos.trilho} />
                  <circle
                    cx="18"
                    cy="18"
                    r="15.5"
                    className={estilos.progresso}
                    pathLength={100}
                    strokeDasharray={`${c.risco.pontos} 100`}
                  />
                </svg>
                <span>{c.risco.pontos}</span>
              </div>
              <div className={estilos.dadosRisco}>
                <div className={estilos.topoRisco}>
                  <strong>{c.nomePaciente}</strong>
                  <Etiqueta tom={NIVEIS[c.risco.nivel].tom}>{NIVEIS[c.risco.nivel].rotulo}</Etiqueta>
                  <Etiqueta tom={STATUS[c.status].tom}>{STATUS[c.status].rotulo}</Etiqueta>
                </div>
                <span className={estilos.quando}>
                  {formatarDataHora(c.dataHora)}
                  {c.especialidade && ` · ${c.especialidade}`}
                  {c.profissional && ` · ${c.profissional}`}
                </span>
                <ul className={estilos.fatores}>
                  {c.risco.fatores.map((f) => (
                    <li key={f}>{f}</li>
                  ))}
                </ul>
                <RegistroDeLigacao consulta={c} aoRegistrar={aoAlterar} />
              </div>
              <div className={estilos.acoesRisco}>
                <a href={`tel:${c.telefone}`} className={estilos.ligar} aria-label={`Ligar para ${c.nomePaciente}`}>
                  <Phone size={16} aria-hidden="true" /> Ligar
                </a>
                <Link href={`/agendamentos/${c.agendamentoId}`} className={estilos.detalhe}>
                  Ver agendamento
                </Link>
              </div>
            </li>
          ))}
        </ul>
      )}
      {(ocultas > 0 || todos) && (
        <button type="button" className={estilos.alternar} onClick={aoAlternar}>
          {todos ? "Mostrar só risco alto e médio" : `Mostrar também ${ocultas} com risco baixo`}
        </button>
      )}
    </div>
  );
}

function Motivos({ painel }: { painel: Painel }) {
  const ordenados = [...painel.motivos].sort((a, b) => b.faltas + b.cancelamentos - (a.faltas + a.cancelamentos));
  const maximo = Math.max(1, ...ordenados.map((m) => m.faltas + m.cancelamentos));
  const total = ordenados.reduce((soma, m) => soma + m.faltas + m.cancelamentos, 0);
  if (total === 0)
    return <EstadoVazio icone={<MessageCircleQuestion size={28} aria-hidden="true" />} titulo="Nenhum motivo informado ainda" />;

  return (
    <div className={estilos.motivos}>
      <ul className={estilos.legendaMotivos}>
        <li>
          <span className={estilos.amostraFalta} aria-hidden="true" /> Faltas
        </li>
        <li>
          <span className={estilos.amostraCancelamento} aria-hidden="true" /> Cancelamentos
        </li>
      </ul>
      <ul className={estilos.listaMotivos}>
        {ordenados.map((m, indice) => (
          <li key={m.motivo} title={MOTIVOS[m.motivo].acao}>
            <div className={estilos.cabecalhoMotivo}>
              <span>{MOTIVOS[m.motivo].rotulo}</span>
              <b>
                {m.faltas + m.cancelamentos} <small>({percentual((m.faltas + m.cancelamentos) / total)})</small>
              </b>
            </div>
            <div className={estilos.barraMotivo} style={{ animationDelay: `${indice * 60}ms` }}>
              {m.faltas > 0 && <span className={estilos.parteFalta} style={{ width: `${(m.faltas / maximo) * 100}%` }} />}
              {m.cancelamentos > 0 && (
                <span className={estilos.parteCancelamento} style={{ width: `${(m.cancelamentos / maximo) * 100}%` }} />
              )}
            </div>
            {indice < 2 && <p className={estilos.sugestao}>{MOTIVOS[m.motivo].acao}</p>}
          </li>
        ))}
      </ul>
    </div>
  );
}

function Especialidades({ painel }: { painel: Painel }) {
  if (painel.especialidades.length === 0)
    return <EstadoVazio icone={<CalendarCheck size={28} aria-hidden="true" />} titulo="Sem consultas realizadas no período" />;
  const maior = Math.max(0.01, ...painel.especialidades.map((e) => taxaDeFaltas(e) ?? 0));

  return (
    <ul className={estilos.listaMotivos}>
      {painel.especialidades.map((e) => {
        const taxa = taxaDeFaltas(e);
        return (
          <li key={e.codigo}>
            <div className={estilos.cabecalhoMotivo}>
              <span>{e.nome}</span>
              <b>
                {percentual(taxa)} <small>de faltas</small>
              </b>
            </div>
            <div className={estilos.barraMotivo}>
              <span className={estilos.parteFalta} style={{ width: `${((taxa ?? 0) / maior) * 100}%` }} />
            </div>
            <p className={estilos.sugestao}>
              {e.faltaram} faltas em {e.compareceram + e.faltaram} consultas realizadas
            </p>
          </li>
        );
      })}
    </ul>
  );
}
