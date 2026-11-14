"use client";

import { CalendarX2, Clock, Inbox, MessageCircle, Phone, RefreshCw } from "lucide-react";
import Link from "next/link";
import { useMemo, useState } from "react";
import { listarAgendamentos } from "@/features/agendamentos/api";
import { EtiquetaStatus } from "@/features/agendamentos/componentes/EtiquetaStatus";
import { ACAO_DA_EQUIPE, TIPOS_DE_ATENDIMENTO } from "@/features/agendamentos/rotulos";
import type { AgendamentoDaEquipe, StatusAgendamento } from "@/features/agendamentos/tipos";
import { mensagemDeErro } from "@/shared/api/erro";
import { useConsulta } from "@/shared/hooks/useConsulta";
import { classes } from "@/shared/lib/classes";
import { adicionarDias, dataDe, formatarDataHora, hoje, rotuloDoDia } from "@/shared/lib/datas";
import { formatarTelefone, linkWhatsApp } from "@/shared/lib/telefone";
import { Alerta } from "@/shared/ui/Alerta";
import { AtualizadoHa } from "@/shared/ui/AtualizadoHa";
import { CabecalhoPagina } from "@/shared/ui/CabecalhoPagina";
import { Carregando } from "@/shared/ui/Carregando";
import { Cartao } from "@/shared/ui/Cartao";
import { EstadoVazio } from "@/shared/ui/EstadoVazio";
import { Segmentado } from "@/shared/ui/Segmentado";
import estilos from "./PainelAvisos.module.css";

type Aba = "todos" | "acao" | "aguardando";

const STATUS_DE_AVISO: StatusAgendamento[] = ["CANCELADO", "REAGENDADO", "AGUARDANDO_RESPOSTA"];
const PEDEM_ACAO: StatusAgendamento[] = ["CANCELADO", "REAGENDADO"];
const DIAS = 30;
const INTERVALO = 10000;

const ICONES: Partial<Record<StatusAgendamento, typeof Clock>> = {
  CANCELADO: CalendarX2,
  REAGENDADO: RefreshCw,
  AGUARDANDO_RESPOSTA: Clock,
};

function filtrar(aba: Aba, agendamentos: AgendamentoDaEquipe[]): AgendamentoDaEquipe[] {
  if (aba === "acao") return agendamentos.filter((a) => PEDEM_ACAO.includes(a.status));
  if (aba === "aguardando") return agendamentos.filter((a) => a.status === "AGUARDANDO_RESPOSTA");
  return agendamentos;
}

function agrupar(agendamentos: AgendamentoDaEquipe[]): [string, AgendamentoDaEquipe[]][] {
  const grupos = new Map<string, AgendamentoDaEquipe[]>();
  agendamentos.forEach((a) => grupos.set(dataDe(a.dataHora), [...(grupos.get(dataDe(a.dataHora)) ?? []), a]));
  return [...grupos.entries()];
}

function Aviso({ agendamento }: { agendamento: AgendamentoDaEquipe }) {
  const Icone = ICONES[agendamento.status] ?? Clock;
  const acao = ACAO_DA_EQUIPE[agendamento.status];
  return (
    <Cartao como="li" espacamento="compacto" className={estilos.aviso}>
      <span className={classes(estilos.iconeAviso, estilos[agendamento.status])} aria-hidden="true">
        <Icone size={20} />
      </span>
      <div className={estilos.corpo}>
        <div className={estilos.linhaTopo}>
          <p className={estilos.nome}>{agendamento.nomePaciente ?? "Paciente"}</p>
          <EtiquetaStatus status={agendamento.status} />
        </div>
        <p className={estilos.meta}>
          {TIPOS_DE_ATENDIMENTO[agendamento.tipo]} · {formatarDataHora(agendamento.dataHora)} · {agendamento.unidade}
        </p>
        {acao && <p className={estilos.acao}>{acao}</p>}
        <div className={estilos.links}>
          <Link href={`/agendamentos/${agendamento.id}`} className={estilos.link}>
            Ver agendamento
          </Link>
          {agendamento.telefone && (
            <a href={linkWhatsApp(agendamento.telefone)} target="_blank" rel="noreferrer" className={estilos.link}>
              <Phone size={14} aria-hidden="true" />
              {formatarTelefone(agendamento.telefone)}
            </a>
          )}
          {agendamento.telefone && (
            <Link href={`/conversas?telefone=${agendamento.telefone}`} className={estilos.link}>
              <MessageCircle size={14} aria-hidden="true" />
              Simular resposta
            </Link>
          )}
        </div>
      </div>
    </Cartao>
  );
}

export function PainelAvisos() {
  const [aba, setAba] = useState<Aba>("todos");
  const inicio = hoje();
  const fim = adicionarDias(inicio, DIAS);
  const consulta = useConsulta(() => listarAgendamentos({ inicio, fim, status: STATUS_DE_AVISO }), inicio, INTERVALO);
  const avisos = useMemo(() => consulta.dados?.agendamentos ?? [], [consulta.dados]);
  const visiveis = filtrar(aba, avisos);
  const pendentes = filtrar("acao", avisos).length;

  const abas: { valor: Aba; rotulo: string }[] = [
    { valor: "todos", rotulo: `Todos (${avisos.length})` },
    { valor: "acao", rotulo: `Pedem ação (${pendentes})` },
    { valor: "aguardando", rotulo: `Sem resposta (${filtrar("aguardando", avisos).length})` },
  ];

  return (
    <div className={estilos.pagina}>
      <CabecalhoPagina titulo="Avisos" subtitulo={`Próximos ${DIAS} dias`} acoes={<AtualizadoHa atualizadoEm={consulta.atualizadoEm} carregando={consulta.carregando} aoAtualizar={consulta.recarregar} />} />

      <Cartao espacamento="amplo" className={estilos.hero}>
        <p className={estilos.heroTitulo}>{pendentes === 0 ? "Você está em dia 💜" : `${pendentes} ${pendentes === 1 ? "aviso pede" : "avisos pedem"} ação`}</p>
        <p className={estilos.heroTexto}>
          {pendentes === 0
            ? "Nenhum cancelamento ou pedido de remarcação pendente."
            : "Pacientes cancelaram ou pediram outro horário. Libere a vaga ou ligue para remarcar."}
        </p>
      </Cartao>

      <Segmentado rotulo="Filtrar avisos" opcoes={abas} valor={aba} aoMudar={setAba} />

      {consulta.erro ? (
        <Alerta tom="perigo" titulo="Não foi possível carregar os avisos">
          {mensagemDeErro(consulta.erro)}
        </Alerta>
      ) : consulta.carregando && !consulta.dados ? (
        <Carregando />
      ) : visiveis.length === 0 ? (
        <EstadoVazio icone={<Inbox size={28} />} titulo="Nada por aqui" descricao="Quando um paciente responder, o aviso aparece aqui." />
      ) : (
        <div className={estilos.grupos}>
          {agrupar(visiveis).map(([dia, lista]) => (
            <section key={dia} className={estilos.grupo} aria-label={rotuloDoDia(dia)}>
              <h2 className={estilos.dia}>{rotuloDoDia(dia)}</h2>
              <ul className={estilos.lista}>
                {lista.map((a) => (
                  <Aviso key={a.id} agendamento={a} />
                ))}
              </ul>
            </section>
          ))}
        </div>
      )}
    </div>
  );
}
