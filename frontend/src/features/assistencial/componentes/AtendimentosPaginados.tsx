"use client";

import { SearchX } from "lucide-react";
import { useState } from "react";
import { mensagemDeErro } from "@/shared/api/erro";
import { useConsulta } from "@/shared/hooks/useConsulta";
import { Alerta } from "@/shared/ui/Alerta";
import { BarraDeFiltros } from "@/shared/ui/BarraDeFiltros";
import { Selecao } from "@/shared/ui/Campo";
import { Carregando } from "@/shared/ui/Carregando";
import { EstadoVazio } from "@/shared/ui/EstadoVazio";
import { PessoaComLupa } from "@/shared/ui/Ilustracoes";
import { Paginacao } from "@/shared/ui/Paginacao";
import { nomeDaEspecialidade } from "../rotulos";
import type { Atendimentos, Consulta, FiltrosDeAtendimentos } from "../tipos";
import { HistoricoDeRamos } from "./HistoricoDeRamos";
import estilos from "./AtendimentosPaginados.module.css";

const POR_PAGINA = 5;
const SEM_FILTROS = { especialidade: "", andamento: "", ano: "" };

type Props = {
  chave: string;
  carregar: (filtros: FiltrosDeAtendimentos) => Promise<Atendimentos>;
  linkParaAgendar?: (encaminhamentoId: string) => string;
  aoCancelar?: (consulta: Consulta) => void;
  cancelando?: string | null;
};

export function AtendimentosPaginados({ chave, carregar, linkParaAgendar, aoCancelar, cancelando }: Props) {
  const [filtros, setFiltros] = useState(SEM_FILTROS);
  const [pagina, setPagina] = useState(1);
  const pedido: FiltrosDeAtendimentos = {
    especialidade: filtros.especialidade || undefined,
    andamento: (filtros.andamento || undefined) as FiltrosDeAtendimentos["andamento"],
    ano: filtros.ano || undefined,
    pagina,
    tamanho: POR_PAGINA,
  };
  const consulta = useConsulta(() => carregar(pedido), `${chave}|${JSON.stringify(pedido)}`);
  const dados = consulta.dados;
  const filtrando = Object.values(filtros).some(Boolean);
  const mostrarFiltros = dados && (filtrando || dados.total > POR_PAGINA);

  function mudar(campo: keyof typeof SEM_FILTROS, valor: string) {
    setFiltros((atuais) => ({ ...atuais, [campo]: valor }));
    setPagina(1);
  }

  function limpar() {
    setFiltros(SEM_FILTROS);
    setPagina(1);
  }

  return (
    <div className={estilos.atendimentos}>
      {mostrarFiltros && (
        <BarraDeFiltros rotulo="Filtrar atendimentos" filtrando={filtrando} aoLimpar={limpar}>
          <Selecao rotulo="Especialidade" value={filtros.especialidade} onChange={(e) => mudar("especialidade", e.target.value)}>
            <option value="">Todas</option>
            {dados.especialidades.map((e) => (
              <option key={e} value={e}>
                {nomeDaEspecialidade(e)}
              </option>
            ))}
          </Selecao>
          <Selecao rotulo="Andamento" value={filtros.andamento} onChange={(e) => mudar("andamento", e.target.value)}>
            <option value="">Todos</option>
            <option value="ABERTO">Em andamento</option>
            <option value="ENCERRADO">Encerrados</option>
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

      {consulta.erro ? <Alerta tom="perigo">{mensagemDeErro(consulta.erro)}</Alerta> : null}
      {!dados && !consulta.erro && <Carregando linhas={4} />}
      {dados && filtrando && dados.total === 0 ? (
        <EstadoVazio
          ilustracao={<PessoaComLupa />}
          icone={<SearchX size={28} aria-hidden="true" />}
          titulo="Nenhum atendimento com esses filtros"
          descricao="Tente outra combinação ou limpe os filtros."
        />
      ) : (
        dados && <HistoricoDeRamos ramos={dados.itens} linkParaAgendar={linkParaAgendar} aoCancelar={aoCancelar} cancelando={cancelando} />
      )}
      {dados && <Paginacao {...dados} irPara={setPagina} carregando={consulta.carregando} item={["atendimento", "atendimentos"]} />}
    </div>
  );
}
