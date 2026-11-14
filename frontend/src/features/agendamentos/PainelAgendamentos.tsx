"use client";

import { CalendarPlus, CalendarSearch } from "lucide-react";
import { useMemo, useState } from "react";
import { mensagemDeErro } from "@/shared/api/erro";
import { useConsulta } from "@/shared/hooks/useConsulta";
import { adicionarDias, hoje } from "@/shared/lib/datas";
import { apenasDigitos } from "@/shared/lib/telefone";
import { Alerta } from "@/shared/ui/Alerta";
import { AtualizadoHa } from "@/shared/ui/AtualizadoHa";
import { BotaoLink } from "@/shared/ui/Botao";
import { CabecalhoPagina } from "@/shared/ui/CabecalhoPagina";
import { Campo } from "@/shared/ui/Campo";
import { Carregando } from "@/shared/ui/Carregando";
import { Chip } from "@/shared/ui/Chip";
import { EstadoVazio } from "@/shared/ui/EstadoVazio";
import { listarAgendamentos } from "./api";
import { ListaAgendamentos } from "./componentes/ListaAgendamentos";
import { STATUS } from "./rotulos";
import type { AgendamentoDaEquipe, StatusAgendamento } from "./tipos";
import estilos from "./PainelAgendamentos.module.css";

const INTERVALO = 10000;
const MAXIMO_DE_DIAS = 30;
const ORDEM_DOS_STATUS: StatusAgendamento[] = [
  "AGUARDANDO_RESPOSTA",
  "CONFIRMADO",
  "REAGENDADO",
  "CANCELADO",
  "AGENDADO",
  "COMPARECEU",
  "FALTOU",
];

const ATALHOS = [
  { rotulo: "Próximos 7 dias", inicio: 0, fim: 6 },
  { rotulo: "Próximos 30 dias", inicio: 0, fim: MAXIMO_DE_DIAS },
  { rotulo: "Últimos 7 dias", inicio: -7, fim: 0 },
];

function corresponde(agendamento: AgendamentoDaEquipe, busca: string): boolean {
  const termo = busca.trim().toLowerCase();
  if (!termo) return true;
  const digitos = apenasDigitos(termo);
  return (
    (agendamento.nomePaciente ?? "").toLowerCase().includes(termo) ||
    (agendamento.nomeCuidador ?? "").toLowerCase().includes(termo) ||
    (digitos.length > 0 && [agendamento.telefone, agendamento.telefoneCuidador].some((t) => t?.includes(digitos)))
  );
}

export function PainelAgendamentos() {
  const [periodo, setPeriodo] = useState(() => ({ inicio: hoje(), fim: adicionarDias(hoje(), 6) }));
  const [status, setStatus] = useState<StatusAgendamento | null>(null);
  const [busca, setBusca] = useState("");
  const { inicio, fim } = periodo;
  const consulta = useConsulta(() => listarAgendamentos({ inicio, fim }), `${inicio}:${fim}`, INTERVALO);
  const lista = consulta.dados;

  const visiveis = useMemo(
    () => (lista ? lista.agendamentos.filter((a) => (!status || a.status === status) && corresponde(a, busca)) : []),
    [lista, status, busca],
  );

  function aplicarAtalho(deslocamentoInicio: number, deslocamentoFim: number) {
    setPeriodo({ inicio: adicionarDias(hoje(), deslocamentoInicio), fim: adicionarDias(hoje(), deslocamentoFim) });
  }

  function alterarInicio(valor: string) {
    if (!valor) return;
    setPeriodo((atual) => ({
      inicio: valor,
      fim: atual.fim < valor || atual.fim > adicionarDias(valor, MAXIMO_DE_DIAS) ? adicionarDias(valor, 6) : atual.fim,
    }));
  }

  return (
    <div className={estilos.pagina}>
      <CabecalhoPagina
        titulo="Agendamentos"
        subtitulo="Status e resposta de cada paciente"
        acoes={<AtualizadoHa atualizadoEm={consulta.atualizadoEm} carregando={consulta.carregando} aoAtualizar={consulta.recarregar} />}
      />

      <div className={estilos.filtros}>
        <div className={estilos.atalhos} role="group" aria-label="Atalhos de período">
          {ATALHOS.map((a) => {
            const selecionado = inicio === adicionarDias(hoje(), a.inicio) && fim === adicionarDias(hoje(), a.fim);
            return (
              <Chip key={a.rotulo} selecionado={selecionado} onClick={() => aplicarAtalho(a.inicio, a.fim)}>
                {a.rotulo}
              </Chip>
            );
          })}
        </div>
        <div className={estilos.campos}>
          <Campo rotulo="De" type="date" value={inicio} onChange={(e) => alterarInicio(e.target.value)} />
          <Campo
            rotulo="Até"
            type="date"
            value={fim}
            min={inicio}
            max={adicionarDias(inicio, MAXIMO_DE_DIAS)}
            onChange={(e) => e.target.value && setPeriodo({ inicio, fim: e.target.value })}
          />
          <div className={estilos.busca}>
            <Campo rotulo="Buscar paciente" type="search" value={busca} onChange={(e) => setBusca(e.target.value)} placeholder="Nome ou telefone" />
          </div>
        </div>
      </div>

      <div className={estilos.status} role="group" aria-label="Filtrar por status">
        <Chip selecionado={status === null} onClick={() => setStatus(null)}>
          Todos · {lista?.total ?? "–"}
        </Chip>
        {ORDEM_DOS_STATUS.map((s) => (
          <Chip key={s} selecionado={status === s} onClick={() => setStatus(status === s ? null : s)}>
            {STATUS[s].rotulo} · {lista?.porStatus[s] ?? "–"}
          </Chip>
        ))}
      </div>

      {consulta.erro ? (
        <Alerta tom="perigo" titulo="Não foi possível carregar os agendamentos">
          {mensagemDeErro(consulta.erro)}
        </Alerta>
      ) : consulta.carregando && !lista ? (
        <Carregando linhas={4} />
      ) : visiveis.length === 0 ? (
        <EstadoVazio
          icone={<CalendarSearch size={28} />}
          titulo="Nenhum agendamento encontrado"
          descricao="Mude o período, o status ou a busca."
          acao={
            <BotaoLink href="/agendar" icone={<CalendarPlus size={18} aria-hidden="true" />}>
              Agendar consulta
            </BotaoLink>
          }
        />
      ) : (
        <ListaAgendamentos agendamentos={visiveis} agruparPorDia />
      )}
    </div>
  );
}
