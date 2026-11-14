"use client";

import { CalendarCheck2, CalendarX2, ChevronLeft, ChevronRight, Clock, Moon, Sun, Sunrise, UserRound } from "lucide-react";
import { useState } from "react";
import { mensagemDeErro } from "@/shared/api/erro";
import { useAcao } from "@/shared/hooks/useAcao";
import { useConsulta } from "@/shared/hooks/useConsulta";
import { classes } from "@/shared/lib/classes";
import { adicionarDias, dataDe, diaDoMes, formatarDataCurta, horaDe, hoje, nomeDiaCurto, rotuloDoDia } from "@/shared/lib/datas";
import { Alerta } from "@/shared/ui/Alerta";
import { Botao } from "@/shared/ui/Botao";
import { Carregando } from "@/shared/ui/Carregando";
import { EstadoVazio } from "@/shared/ui/EstadoVazio";
import { agendaDoEncaminhamento, agendarConsulta, horariosDisponiveis, remarcarConsulta } from "../api";
import type { Consulta, HorarioDisponivel } from "../tipos";
import estilos from "./EscolhaDeHorario.module.css";

const DIAS_POR_PAGINA = 7;

const PERIODOS = [
  { rotulo: "Manhã", Icone: Sunrise, ate: 12 },
  { rotulo: "Tarde", Icone: Sun, ate: 18 },
  { rotulo: "Noite", Icone: Moon, ate: 24 },
];

type Escolha = { horario: HorarioDisponivel; chave: string };
type Remarcacao = { consultaId: string; versao: number };
type Agenda = { profissionalId: string; nome: string; horarios: HorarioDisponivel[] };

type Props = {
  encaminhamentoId: string;
  remarcacao?: Remarcacao | null;
  aoAgendar: (consulta: Consulta) => void;
};

function diasDaSemana(inicio: string): string[] {
  return Array.from({ length: DIAS_POR_PAGINA }, (_, i) => adicionarDias(inicio, i));
}

function porDia(horarios: HorarioDisponivel[]): Map<string, HorarioDisponivel[]> {
  const grupos = new Map<string, HorarioDisponivel[]>();
  horarios.forEach((h) => grupos.set(dataDe(h.inicio), [...(grupos.get(dataDe(h.inicio)) ?? []), h]));
  return grupos;
}

function porProfissional(horarios: HorarioDisponivel[]): Agenda[] {
  const agendas = new Map<string, Agenda>();
  horarios.forEach((h) => {
    const agenda = agendas.get(h.profissionalId) ?? { profissionalId: h.profissionalId, nome: h.nomeProfissional, horarios: [] };
    agenda.horarios.push(h);
    agendas.set(h.profissionalId, agenda);
  });
  return [...agendas.values()];
}

function porPeriodo(horarios: HorarioDisponivel[]) {
  return PERIODOS.map((periodo, i) => {
    const desde = i === 0 ? 0 : PERIODOS[i - 1].ate;
    return { ...periodo, horarios: horarios.filter((h) => Number(horaDe(h.inicio).slice(0, 2)) >= desde && Number(horaDe(h.inicio).slice(0, 2)) < periodo.ate) };
  }).filter((p) => p.horarios.length > 0);
}

function mesmoHorario(a: HorarioDisponivel, b: HorarioDisponivel): boolean {
  return a.inicio === b.inicio && a.profissionalId === b.profissionalId;
}

function quantidade(n: number): string {
  if (n === 0) return "Sem horários";
  return n === 1 ? "1 horário" : `${n} horários`;
}

export function EscolhaDeHorario({ encaminhamentoId, remarcacao, aoAgendar }: Props) {
  const [inicio, setInicio] = useState(hoje);
  const [diaEscolhido, setDiaEscolhido] = useState<string | null>(null);
  const [escolha, setEscolha] = useState<Escolha | null>(null);
  const fim = adicionarDias(inicio, DIAS_POR_PAGINA - 1);
  const horarios = useConsulta(() => horariosDisponiveis(encaminhamentoId, inicio, fim), `${encaminhamentoId}-${inicio}`);
  const agendaAberta = useConsulta(() => agendaDoEncaminhamento(encaminhamentoId), `agenda-${encaminhamentoId}`);
  const acao = useAcao();

  const dias = diasDaSemana(inicio);
  const grupos = porDia(horarios.dados ?? []);
  const dia = diaEscolhido && grupos.has(diaEscolhido) ? diaEscolhido : (dias.find((d) => grupos.has(d)) ?? null);
  const agendas = dia ? porProfissional(grupos.get(dia) ?? []) : [];

  function mudarSemana(deslocamento: number) {
    setInicio((atual) => adicionarDias(atual, deslocamento));
    setDiaEscolhido(null);
    setEscolha(null);
  }

  function escolherDia(d: string) {
    setDiaEscolhido(d);
    setEscolha(null);
  }

  async function confirmar() {
    if (!escolha) return;
    const { horario, chave } = escolha;
    const consulta = await acao.executar("agendar", () =>
      remarcacao
        ? remarcarConsulta(remarcacao.consultaId, horario.profissionalId, horario.inicio, remarcacao.versao)
        : agendarConsulta(encaminhamentoId, horario.profissionalId, horario.inicio, chave),
    );
    if (consulta) aoAgendar(consulta);
    else {
      setEscolha(null);
      horarios.recarregar();
    }
  }

  if (agendaAberta.dados?.possuiAgenda === false)
    return (
      <EstadoVazio
        icone={<CalendarX2 size={28} aria-hidden="true" />}
        titulo="Ainda não há agenda para esta especialidade"
        descricao="O encaminhamento continua válido. Assim que um especialista abrir a agenda na unidade, os horários aparecem aqui."
      />
    );

  return (
    <div className={estilos.escolha}>
      <div className={estilos.navegacao}>
        <Botao variante="secundario" tamanho="pequeno" icone={<ChevronLeft size={18} aria-hidden="true" />} onClick={() => mudarSemana(-DIAS_POR_PAGINA)} disabled={inicio <= hoje()} aria-label="Semana anterior">
          <span className={estilos.textoNavegacao}>Anterior</span>
        </Botao>
        <span className={estilos.periodo}>
          {formatarDataCurta(inicio)} a {formatarDataCurta(fim)}
        </span>
        <Botao variante="secundario" tamanho="pequeno" iconeDireita={<ChevronRight size={18} aria-hidden="true" />} onClick={() => mudarSemana(DIAS_POR_PAGINA)} aria-label="Próxima semana">
          <span className={estilos.textoNavegacao}>Próxima</span>
        </Botao>
      </div>

      {acao.falha && <Alerta tom="perigo">{acao.falha}</Alerta>}
      {horarios.erro ? <Alerta tom="perigo">{mensagemDeErro(horarios.erro)}</Alerta> : null}

      <ol className={estilos.passos} aria-label="Como agendar">
        <li className={estilos.passo}>
          <span className={estilos.numero}>1</span> Escolha o dia
        </li>
        <li className={estilos.passo}>
          <span className={estilos.numero}>2</span> Escolha o horário
        </li>
        <li className={estilos.passo}>
          <span className={estilos.numero}>3</span> Confirme
        </li>
      </ol>

      <div className={estilos.semana} role="radiogroup" aria-label="Dias da semana">
        {dias.map((d) => {
          const total = grupos.get(d)?.length ?? 0;
          const selecionado = d === dia;
          return (
            <button
              key={d}
              type="button"
              role="radio"
              aria-checked={selecionado}
              aria-label={`${rotuloDoDia(d)}, ${quantidade(total)}`}
              className={classes(estilos.diaDaSemana, selecionado && estilos.diaSelecionado, d === hoje() && estilos.hoje)}
              disabled={total === 0}
              onClick={() => escolherDia(d)}
            >
              <span className={estilos.nomeDoDia}>{nomeDiaCurto(d)}</span>
              <span className={estilos.numeroDoDia}>{diaDoMes(d)}</span>
              <span className={estilos.vagas}>{horarios.dados ? quantidade(total) : " "}</span>
            </button>
          );
        })}
      </div>

      {horarios.carregando && !horarios.dados && <Carregando />}
      {horarios.dados && !dia && (
        <EstadoVazio icone={<Clock size={28} aria-hidden="true" />} titulo="Nenhum horário livre nesta semana" descricao="Toque em Próxima para ver a semana seguinte." />
      )}

      {dia && (
        <section className={estilos.agendaDoDia} aria-label={`Horários de ${rotuloDoDia(dia)}`}>
          <h3 className={estilos.tituloDoDia}>{rotuloDoDia(dia)}</h3>
          {agendas.map((agenda) => (
            <div key={agenda.profissionalId} className={estilos.agenda}>
              <p className={estilos.profissional}>
                <UserRound size={16} aria-hidden="true" />
                {agenda.nome}
              </p>
              {porPeriodo(agenda.horarios).map(({ rotulo, Icone, horarios: doPeriodo }) => (
                <div key={rotulo} className={estilos.periodoDoDia}>
                  <span className={estilos.rotuloPeriodo}>
                    <Icone size={16} aria-hidden="true" />
                    {rotulo}
                  </span>
                  <div className={estilos.horarios}>
                    {doPeriodo.map((h) => {
                      const selecionado = escolha !== null && mesmoHorario(escolha.horario, h);
                      return (
                        <button
                          key={h.inicio}
                          type="button"
                          aria-pressed={selecionado}
                          aria-label={`${horaDe(h.inicio)} com ${h.nomeProfissional}`}
                          className={classes(estilos.horario, selecionado && estilos.horarioSelecionado)}
                          onClick={() => setEscolha({ horario: h, chave: crypto.randomUUID() })}
                        >
                          {horaDe(h.inicio)}
                        </button>
                      );
                    })}
                  </div>
                </div>
              ))}
            </div>
          ))}
        </section>
      )}

      {escolha && (
        <div className={estilos.confirmacao} role="region" aria-label="Confirmar horário">
          <p>
            <strong>
              {rotuloDoDia(dataDe(escolha.horario.inicio))}, às {horaDe(escolha.horario.inicio)}
            </strong>
            <span>com {escolha.horario.nomeProfissional}</span>
          </p>
          <Botao icone={<CalendarCheck2 size={18} aria-hidden="true" />} carregando={acao.executando === "agendar"} onClick={confirmar}>
            {remarcacao ? "Confirmar novo horário" : "Confirmar agendamento"}
          </Botao>
        </div>
      )}
    </div>
  );
}
