"use client";

import { CalendarPlus, CalendarSearch } from "lucide-react";
import { useState } from "react";
import { mensagemDeErro } from "@/shared/api/erro";
import { useAtrasado } from "@/shared/hooks/useAtrasado";
import { useConsulta } from "@/shared/hooks/useConsulta";
import { adicionarDias, hoje } from "@/shared/lib/datas";
import { Alerta } from "@/shared/ui/Alerta";
import { AtualizadoHa } from "@/shared/ui/AtualizadoHa";
import { BotaoLink } from "@/shared/ui/Botao";
import { CabecalhoPagina } from "@/shared/ui/CabecalhoPagina";
import { Campo } from "@/shared/ui/Campo";
import { Carregando } from "@/shared/ui/Carregando";
import { Chip } from "@/shared/ui/Chip";
import { EstadoVazio } from "@/shared/ui/EstadoVazio";
import { Paginacao } from "@/shared/ui/Paginacao";
import { listarAgendamentos } from "./api";
import { ListaAgendamentos } from "./componentes/ListaAgendamentos";
import { STATUS } from "./rotulos";
import type { StatusAgendamento } from "./tipos";
import estilos from "./PainelAgendamentos.module.css";

const INTERVALO = 10000;
const POR_PAGINA = 20;
const MAXIMO_DE_DIAS = 30;
const ORDEM_DOS_STATUS: StatusAgendamento[] = [
  "AGUARDANDO_RESPOSTA",
  "CONFIRMADO",
  "REAGENDADO",
  "CANCELADO",
  "AGENDADO",
  "EM_ANDAMENTO",
  "COMPARECEU",
  "FALTOU",
];

const ATALHOS = [
  { rotulo: "Próximos 7 dias", inicio: 0, fim: 6 },
  { rotulo: "Próximos 30 dias", inicio: 0, fim: MAXIMO_DE_DIAS },
  { rotulo: "Últimos 7 dias", inicio: -7, fim: 0 },
];

export function PainelAgendamentos() {
  const [periodo, setPeriodo] = useState(() => ({ inicio: hoje(), fim: adicionarDias(hoje(), 6) }));
  const [status, setStatus] = useState<StatusAgendamento | null>(null);
  const [busca, setBusca] = useState("");
  const [pagina, setPagina] = useState(1);
  const buscaAtrasada = useAtrasado(busca.trim());
  const { inicio, fim } = periodo;
  const pedido = { inicio, fim, status: status ? [status] : undefined, busca: buscaAtrasada, pagina, tamanho: POR_PAGINA };
  const consulta = useConsulta(() => listarAgendamentos(pedido), JSON.stringify(pedido), INTERVALO);
  const lista = consulta.dados;
  const visiveis = lista?.agendamentos ?? [];

  function filtrarStatus(proximo: StatusAgendamento | null) {
    setStatus(proximo);
    setPagina(1);
  }

  function buscar(valor: string) {
    setBusca(valor);
    setPagina(1);
  }

  function aplicarAtalho(deslocamentoInicio: number, deslocamentoFim: number) {
    setPeriodo({ inicio: adicionarDias(hoje(), deslocamentoInicio), fim: adicionarDias(hoje(), deslocamentoFim) });
    setPagina(1);
  }

  function alterarInicio(valor: string) {
    if (!valor) return;
    setPagina(1);
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
            onChange={(e) => {
              if (!e.target.value) return;
              setPeriodo({ inicio, fim: e.target.value });
              setPagina(1);
            }}
          />
          <div className={estilos.busca}>
            <Campo
              rotulo="Buscar paciente"
              type="search"
              value={busca}
              onChange={(e) => buscar(e.target.value)}
              placeholder="Nome ou telefone"
            />
          </div>
        </div>
      </div>

      <div className={estilos.status} role="group" aria-label="Filtrar por status">
        <Chip selecionado={status === null} onClick={() => filtrarStatus(null)}>
          Todos · {lista?.total ?? "–"}
        </Chip>
        {ORDEM_DOS_STATUS.map((s) => (
          <Chip key={s} selecionado={status === s} onClick={() => filtrarStatus(status === s ? null : s)}>
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
        <>
          <ListaAgendamentos agendamentos={visiveis} agruparPorDia />
          <Paginacao
            pagina={lista?.pagina ?? 1}
            totalPaginas={lista?.totalPaginas ?? 1}
            tamanho={lista?.tamanho ?? POR_PAGINA}
            total={lista?.encontrados ?? 0}
            irPara={setPagina}
            carregando={consulta.carregando}
            item={["agendamento", "agendamentos"]}
          />
        </>
      )}
    </div>
  );
}
