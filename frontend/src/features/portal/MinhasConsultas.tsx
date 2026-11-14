"use client";

import { CalendarPlus, CalendarX, History, SearchX } from "lucide-react";
import { useState } from "react";
import { meuHistorico, minhasConsultas } from "@/features/assistencial/api";
import { STATUS_DA_CONSULTA } from "@/features/assistencial/rotulos";
import type { FiltrosDeConsultas, StatusConsulta } from "@/features/assistencial/tipos";
import { mensagemDeErro } from "@/shared/api/erro";
import { useAtrasado } from "@/shared/hooks/useAtrasado";
import { useConsulta } from "@/shared/hooks/useConsulta";
import { usuarioAtual } from "@/shared/sessao/sessao";
import { Alerta } from "@/shared/ui/Alerta";
import { BarraDeFiltros } from "@/shared/ui/BarraDeFiltros";
import { BotaoLink } from "@/shared/ui/Botao";
import { CabecalhoPagina } from "@/shared/ui/CabecalhoPagina";
import { Campo, Selecao } from "@/shared/ui/Campo";
import { Carregando } from "@/shared/ui/Carregando";
import { EstadoVazio } from "@/shared/ui/EstadoVazio";
import { PessoaComLupa } from "@/shared/ui/Ilustracoes";
import { Paginacao } from "@/shared/ui/Paginacao";
import { Segmentado } from "@/shared/ui/Segmentado";
import { CartaoDeConsulta } from "./CartaoDeConsulta";
import { paraAgendar } from "./consultas";
import estilos from "./Portal.module.css";

type Periodo = FiltrosDeConsultas["periodo"];

const POR_PAGINA = 5;
const SEM_FILTROS = { busca: "", especialidade: "", situacao: "", ano: "" };

export function MinhasConsultas() {
  const [periodo, setPeriodo] = useState<Periodo>("PROXIMAS");
  const usuario = useConsulta(usuarioAtual, "usuario");
  const [filtros, setFiltros] = useState(SEM_FILTROS);
  const [pagina, setPagina] = useState(1);
  const busca = useAtrasado(filtros.busca.trim());
  const pedido: FiltrosDeConsultas = {
    periodo,
    busca: busca || undefined,
    especialidade: filtros.especialidade || undefined,
    situacao: (filtros.situacao || undefined) as StatusConsulta | undefined,
    ano: filtros.ano || undefined,
    pagina,
    tamanho: POR_PAGINA,
  };
  const consulta = useConsulta(() => minhasConsultas(pedido), `minhas-consultas|${JSON.stringify(pedido)}`);
  const historico = useConsulta(meuHistorico, "meu-historico");
  const dados = consulta.dados;
  const pendentes = historico.dados ? paraAgendar(historico.dados) : [];
  const filtrando = Object.values(filtros).some(Boolean);
  const doPeriodo = dados ? (periodo === "PROXIMAS" ? dados.proximas : dados.anteriores) : 0;

  function mudar(campo: keyof typeof SEM_FILTROS, valor: string) {
    setFiltros((atuais) => ({ ...atuais, [campo]: valor }));
    setPagina(1);
  }

  function limpar() {
    setFiltros(SEM_FILTROS);
    setPagina(1);
  }

  function trocarPeriodo(proximo: Periodo) {
    setPeriodo(proximo);
    limpar();
  }

  function recarregar() {
    consulta.recarregar();
    historico.recarregar();
  }

  return (
    <div className={estilos.pagina}>
      <CabecalhoPagina titulo="Minhas consultas" subtitulo="Você recebe a confirmação e os lembretes pelo WhatsApp." />
      {consulta.erro ? <Alerta tom="perigo">{mensagemDeErro(consulta.erro)}</Alerta> : null}
      {!dados && !consulta.erro && <Carregando linhas={5} />}

      {dados && (
        <>
          <Segmentado
            rotulo="Tipo de consulta"
            opcoes={[
              { valor: "PROXIMAS", rotulo: `Próximas (${dados.proximas})` },
              { valor: "ANTERIORES", rotulo: `Anteriores e canceladas (${dados.anteriores})` },
            ]}
            valor={periodo}
            aoMudar={trocarPeriodo}
          />

          {(filtrando || doPeriodo > POR_PAGINA) && (
            <BarraDeFiltros rotulo="Filtrar consultas" filtrando={filtrando} aoLimpar={limpar}>
              <Campo
                rotulo="Buscar"
                type="search"
                placeholder="Especialidade ou profissional"
                value={filtros.busca}
                onChange={(e) => mudar("busca", e.target.value)}
              />
              <Selecao rotulo="Especialidade" value={filtros.especialidade} onChange={(e) => mudar("especialidade", e.target.value)}>
                <option value="">Todas</option>
                {dados.especialidades.map((e) => (
                  <option key={e} value={e}>
                    {e}
                  </option>
                ))}
              </Selecao>
              <Selecao rotulo="Situação" value={filtros.situacao} onChange={(e) => mudar("situacao", e.target.value)}>
                <option value="">Todas</option>
                {dados.situacoes.map((s) => (
                  <option key={s} value={s}>
                    {STATUS_DA_CONSULTA[s].texto}
                  </option>
                ))}
              </Selecao>
              {dados.anos.length > 1 && (
                <Selecao rotulo="Ano" value={filtros.ano} onChange={(e) => mudar("ano", e.target.value)}>
                  <option value="">Todos</option>
                  {dados.anos.map((a) => (
                    <option key={a} value={a}>
                      {a}
                    </option>
                  ))}
                </Selecao>
              )}
            </BarraDeFiltros>
          )}

          {doPeriodo === 0 && periodo === "PROXIMAS" && (
            <EstadoVazio
              icone={<CalendarX size={28} aria-hidden="true" />}
              titulo="Nenhuma consulta marcada"
              acao={
                pendentes.length > 0 ? (
                  <BotaoLink
                    href={`/portal/agendar/${pendentes[0].id}`}
                    tamanho="pequeno"
                    icone={<CalendarPlus size={16} aria-hidden="true" />}
                  >
                    Escolher horário
                  </BotaoLink>
                ) : undefined
              }
            />
          )}
          {doPeriodo === 0 && periodo === "ANTERIORES" && (
            <EstadoVazio icone={<History size={28} aria-hidden="true" />} titulo="Nenhuma consulta anterior" />
          )}
          {doPeriodo > 0 && dados.total === 0 && (
            <EstadoVazio
              ilustracao={<PessoaComLupa />}
              icone={<SearchX size={28} aria-hidden="true" />}
              titulo="Nenhuma consulta com esses filtros"
              descricao="Tente outra busca ou limpe os filtros."
            />
          )}

          <ul className={estilos.lista}>
            {dados.itens.map((item) => (
              <li key={item.consulta.id}>
                <CartaoDeConsulta
                  item={item}
                  nomeProfissional={item.profissional ?? undefined}
                  aoAlterar={recarregar}
                  comAnotacoes={usuario.dados?.papel === "PACIENTE"}
                />
              </li>
            ))}
          </ul>
          <Paginacao {...dados} irPara={setPagina} carregando={consulta.carregando} item={["consulta", "consultas"]} />
        </>
      )}
    </div>
  );
}
