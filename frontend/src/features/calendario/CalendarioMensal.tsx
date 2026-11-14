"use client";

import { CalendarPlus, CalendarSearch, ChevronLeft, ChevronRight } from "lucide-react";
import { useMemo, useState } from "react";
import { listarAgendamentos } from "@/features/agendamentos/api";
import { ListaAgendamentos } from "@/features/agendamentos/componentes/ListaAgendamentos";
import type { AgendamentoDaEquipe } from "@/features/agendamentos/tipos";
import { mensagemDeErro } from "@/shared/api/erro";
import { useConsulta } from "@/shared/hooks/useConsulta";
import { classes } from "@/shared/lib/classes";
import { dataDe, diaDoMes, fimDoMes, formatarDiaLongo, gradeDoMes, hoje, inicioDoMes, nomeMes } from "@/shared/lib/datas";
import { Alerta } from "@/shared/ui/Alerta";
import { BotaoLink } from "@/shared/ui/Botao";
import { CabecalhoPagina } from "@/shared/ui/CabecalhoPagina";
import { Carregando } from "@/shared/ui/Carregando";
import { Cartao } from "@/shared/ui/Cartao";
import { EstadoVazio } from "@/shared/ui/EstadoVazio";
import estilos from "./CalendarioMensal.module.css";

const SEMANA = ["seg", "ter", "qua", "qui", "sex", "sáb", "dom"];
const INTERVALO = 15000;

function mesDe(data: string) {
  const [ano, mes] = data.split("-").map(Number);
  return { ano, mes };
}

function porDia(agendamentos: AgendamentoDaEquipe[]): Map<string, AgendamentoDaEquipe[]> {
  const mapa = new Map<string, AgendamentoDaEquipe[]>();
  agendamentos.forEach((a) => mapa.set(dataDe(a.dataHora), [...(mapa.get(dataDe(a.dataHora)) ?? []), a]));
  return mapa;
}

export function CalendarioMensal() {
  const [selecionado, setSelecionado] = useState(hoje);
  const [{ ano, mes }, setMes] = useState(() => mesDe(hoje()));
  const inicio = inicioDoMes(ano, mes);
  const fim = fimDoMes(ano, mes);
  const consulta = useConsulta(() => listarAgendamentos({ inicio, fim }), inicio, INTERVALO);
  const dias = useMemo(() => porDia(consulta.dados?.agendamentos ?? []), [consulta.dados]);
  const doDia = dias.get(selecionado) ?? [];
  const referencia = hoje();

  function navegar(delta: number) {
    const indice = ano * 12 + (mes - 1) + delta;
    const novo = { ano: Math.floor(indice / 12), mes: (indice % 12) + 1 };
    setMes(novo);
    const primeiro = inicioDoMes(novo.ano, novo.mes);
    setSelecionado(referencia >= primeiro && referencia <= fimDoMes(novo.ano, novo.mes) ? referencia : primeiro);
  }

  return (
    <div className={estilos.pagina}>
      <CabecalhoPagina titulo="Calendário" subtitulo="Atendimentos do mês" />

      <div className={estilos.grade}>
        <Cartao className={estilos.calendario}>
          <div className={estilos.topo}>
            <h2 className={estilos.mes}>
              {nomeMes(mes)} {ano}
            </h2>
            <div className={estilos.navegacao}>
              <button type="button" className={estilos.seta} onClick={() => navegar(-1)} aria-label="Mês anterior">
                <ChevronLeft size={20} aria-hidden="true" />
              </button>
              <button type="button" className={estilos.seta} onClick={() => navegar(1)} aria-label="Próximo mês">
                <ChevronRight size={20} aria-hidden="true" />
              </button>
            </div>
          </div>

          <div className={estilos.semana} aria-hidden="true">
            {SEMANA.map((dia) => (
              <span key={dia}>{dia}</span>
            ))}
          </div>

          <div className={estilos.dias} role="group" aria-label={`${nomeMes(mes)} de ${ano}`}>
            {gradeDoMes(ano, mes).map((dia, i) =>
              dia ? (
                <button
                  key={dia}
                  type="button"
                  aria-pressed={dia === selecionado}
                  aria-label={`${formatarDiaLongo(dia)}${dias.has(dia) ? `, ${dias.get(dia)?.length} atendimentos` : ""}`}
                  className={classes(estilos.dia, dia === referencia && estilos.hoje, dia === selecionado && estilos.selecionado)}
                  onClick={() => setSelecionado(dia)}
                >
                  {diaDoMes(dia)}
                  {dias.has(dia) && <span className={estilos.ponto} />}
                </button>
              ) : (
                <span key={`vazio-${i}`} />
              ),
            )}
          </div>
        </Cartao>

        <section className={estilos.secao} aria-label="Atendimentos do dia">
          <div className={estilos.topo}>
            <h2 className={estilos.tituloDia}>{formatarDiaLongo(selecionado)}</h2>
          </div>
          {consulta.erro ? (
            <Alerta tom="perigo" titulo="Não foi possível carregar">
              {mensagemDeErro(consulta.erro)}
            </Alerta>
          ) : consulta.carregando && !consulta.dados ? (
            <Carregando />
          ) : doDia.length ? (
            <ListaAgendamentos agendamentos={doDia} />
          ) : (
            <EstadoVazio icone={<CalendarSearch size={28} />} titulo="Nenhum atendimento neste dia" />
          )}
          {selecionado >= referencia && (
            <BotaoLink href={`/agendar?data=${selecionado}`} larguraTotal icone={<CalendarPlus size={18} aria-hidden="true" />}>
              Agendar nova consulta
            </BotaoLink>
          )}
        </section>
      </div>
    </div>
  );
}
