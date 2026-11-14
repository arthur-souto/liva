"use client";

import { CalendarPlus, ClipboardList, MapPin, XCircle } from "lucide-react";
import { formatarDataHora, formatarInstante } from "@/shared/lib/datas";
import { BotaoLink, Botao } from "@/shared/ui/Botao";
import { EstadoVazio } from "@/shared/ui/EstadoVazio";
import { Etiqueta } from "@/shared/ui/Etiqueta";
import { NATUREZAS, PRIORIDADES, STATUS_DA_CONSULTA, STATUS_DA_TRIAGEM, STATUS_DO_ENCAMINHAMENTO, nomeDaEspecialidade } from "../rotulos";
import type { Consulta, Historico, StatusConsulta } from "../tipos";
import estilos from "./HistoricoDeRamos.module.css";

const CANCELAVEIS: StatusConsulta[] = ["AGENDADO", "AGUARDANDO_RESPOSTA", "CONFIRMADO"];

type Props = {
  historico: Historico;
  linkParaAgendar?: (encaminhamentoId: string) => string;
  aoCancelar?: (consulta: Consulta) => void;
  cancelando?: string | null;
};

export function HistoricoDeRamos({ historico, linkParaAgendar, aoCancelar, cancelando }: Props) {
  if (historico.ramos.length === 0)
    return <EstadoVazio icone={<ClipboardList size={28} aria-hidden="true" />} titulo="Nenhuma triagem ainda" descricao="Os atendimentos aparecem aqui depois da triagem." />;

  return (
    <ol className={estilos.ramos}>
      {historico.ramos.map((ramo, indice) => (
        <li key={ramo.triagem.id} className={estilos.ramo}>
          <header className={estilos.cabecalho}>
            <span className={estilos.numero}>Triagem {historico.ramos.length - indice}</span>
            <span className={estilos.meta}>{formatarInstante(ramo.triagem.chegadaEm)}</span>
            <Etiqueta tom={STATUS_DA_TRIAGEM[ramo.triagem.status].tom}>{STATUS_DA_TRIAGEM[ramo.triagem.status].texto}</Etiqueta>
            {ramo.triagem.prioridade && (
              <Etiqueta tom={PRIORIDADES[ramo.triagem.prioridade].tom}>{PRIORIDADES[ramo.triagem.prioridade].texto}</Etiqueta>
            )}
          </header>
          {ramo.acessoClinico && ramo.triagem.queixaPrincipal && <p className={estilos.queixa}>{ramo.triagem.queixaPrincipal}</p>}

          {ramo.encaminhamentos.map((e) => (
            <div key={e.id} className={estilos.encaminhamento}>
              <div className={estilos.linha}>
                <strong>{nomeDaEspecialidade(e.especialidade)}</strong>
                <Etiqueta tom={STATUS_DO_ENCAMINHAMENTO[e.status].tom}>{STATUS_DO_ENCAMINHAMENTO[e.status].texto}</Etiqueta>
                {linkParaAgendar && e.status === "AGUARDANDO_AGENDAMENTO" && e.modalidade === "ELETIVO" && (
                  <BotaoLink href={linkParaAgendar(e.id)} tamanho="pequeno" icone={<CalendarPlus size={16} aria-hidden="true" />}>
                    Agendar
                  </BotaoLink>
                )}
              </div>
              {e.desfecho && <p className={estilos.meta}>Desfecho: {e.desfecho}</p>}
              <ul className={estilos.consultas}>
                {ramo.consultas
                  .filter((c) => c.encaminhamentoId === e.id)
                  .map((c) => (
                    <li key={c.id} className={estilos.consulta}>
                      <div className={estilos.linha}>
                        <strong>{formatarDataHora(c.inicio)}</strong>
                        <Etiqueta tom={STATUS_DA_CONSULTA[c.status].tom}>{STATUS_DA_CONSULTA[c.status].texto}</Etiqueta>
                        <span className={estilos.meta}>{NATUREZAS[c.natureza]}</span>
                      </div>
                      <p className={estilos.local}>
                        <MapPin size={14} aria-hidden="true" />
                        {c.unidade} · {c.endereco}
                      </p>
                      {aoCancelar && CANCELAVEIS.includes(c.status) && (
                        <Botao
                          variante="fantasma"
                          tamanho="pequeno"
                          icone={<XCircle size={16} aria-hidden="true" />}
                          carregando={cancelando === c.id}
                          onClick={() => aoCancelar(c)}
                        >
                          Cancelar consulta
                        </Botao>
                      )}
                    </li>
                  ))}
              </ul>
            </div>
          ))}
        </li>
      ))}
    </ol>
  );
}
