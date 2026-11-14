"use client";

import { ArrowRight, CalendarX, Inbox, Siren } from "lucide-react";
import { useRouter } from "next/navigation";
import { agendaDoEspecialista, encaminhamentosRecebidos, receberNoPlantao } from "@/features/assistencial/api";
import { MODALIDADES, NATUREZAS, PRIORIDADES, STATUS_DA_CONSULTA, STATUS_DO_ENCAMINHAMENTO, nomeDaEspecialidade } from "@/features/assistencial/rotulos";
import type { ConsultaDaAgenda, EncaminhamentoRecebido } from "@/features/assistencial/tipos";
import { mensagemDeErro } from "@/shared/api/erro";
import { useAcao } from "@/shared/hooks/useAcao";
import { useConsulta } from "@/shared/hooks/useConsulta";
import { adicionarDias, dataDe, formatarInstante, hoje, horaDe, rotuloDoDia } from "@/shared/lib/datas";
import { Alerta } from "@/shared/ui/Alerta";
import { AtualizadoHa } from "@/shared/ui/AtualizadoHa";
import { Botao, BotaoLink } from "@/shared/ui/Botao";
import { CabecalhoPagina } from "@/shared/ui/CabecalhoPagina";
import { Carregando } from "@/shared/ui/Carregando";
import { EstadoVazio } from "@/shared/ui/EstadoVazio";
import { ProfissionalComPrancheta } from "@/shared/ui/Ilustracoes";
import { Etiqueta } from "@/shared/ui/Etiqueta";
import { Secao } from "@/shared/ui/Secao";
import { LinkDeAjuda } from "@/shared/ui/LinkDeAjuda";
import estilos from "./PainelEspecialista.module.css";

const DIAS_DA_AGENDA = 7;

function semana(): Promise<ConsultaDaAgenda[]> {
  const inicio = hoje();
  return agendaDoEspecialista(inicio, adicionarDias(inicio, DIAS_DA_AGENDA - 1));
}

function porDia(itens: ConsultaDaAgenda[]): [string, ConsultaDaAgenda[]][] {
  const grupos = new Map<string, ConsultaDaAgenda[]>();
  for (const item of [...itens].sort((a, b) => a.consulta.inicio.localeCompare(b.consulta.inicio))) {
    const dia = dataDe(item.consulta.inicio);
    grupos.set(dia, [...(grupos.get(dia) ?? []), item]);
  }
  return [...grupos.entries()];
}

function linkDoAtendimento(triagemId: string, consultaId?: string): string {
  return `/especialista/atendimentos/${triagemId}${consultaId ? `?consulta=${consultaId}` : ""}`;
}

export function PainelEspecialista() {
  const router = useRouter();
  const agenda = useConsulta(semana, "agenda-especialista", 30000);
  const recebidos = useConsulta(encaminhamentosRecebidos, "encaminhamentos-recebidos", 15000);
  const acao = useAcao();

  async function receber(item: EncaminhamentoRecebido) {
    const { id, triagemId } = item.encaminhamento;
    if (await acao.executar(id, () => receberNoPlantao(id))) router.push(linkDoAtendimento(triagemId));
    else recebidos.recarregar();
  }

  function atualizar() {
    agenda.recarregar();
    recebidos.recarregar();
  }

  const plantao = (recebidos.dados ?? []).filter((r) => r.encaminhamento.modalidade === "IMEDIATO" && r.encaminhamento.status === "PENDENTE");
  const emAcompanhamento = (recebidos.dados ?? []).filter((r) => !plantao.includes(r));

  return (
    <div className={estilos.pagina}>
      <CabecalhoPagina
        titulo="Minha agenda"
        subtitulo="Consultas dos próximos 7 dias e encaminhamentos da sua especialidade."
        acoes={<LinkDeAjuda topico="minha-agenda" />}
      />
      <div className={estilos.status}>
        <AtualizadoHa atualizadoEm={agenda.atualizadoEm} carregando={agenda.carregando || recebidos.carregando} aoAtualizar={atualizar} />
      </div>
      {acao.falha && <Alerta tom="perigo">{acao.falha}</Alerta>}

      {plantao.length > 0 && (
        <Secao titulo="Plantão" descricao="Encaminhamentos imediatos aguardando um especialista. Ao receber, o atendimento fica com você.">
          <ul className={estilos.lista}>
            {plantao.map((item) => (
              <li key={item.encaminhamento.id} className={`${estilos.item} ${estilos.urgente}`}>
                <DadosDoEncaminhamento item={item} />
                <Botao
                  tamanho="pequeno"
                  variante="perigo"
                  icone={<Siren size={16} aria-hidden="true" />}
                  carregando={acao.executando === item.encaminhamento.id}
                  onClick={() => receber(item)}
                >
                  Receber
                </Botao>
              </li>
            ))}
          </ul>
        </Secao>
      )}

      <Secao titulo="Consultas">
        {agenda.erro ? <Alerta tom="perigo">{mensagemDeErro(agenda.erro)}</Alerta> : null}
        {!agenda.dados && !agenda.erro && <Carregando linhas={3} />}
        {agenda.dados && agenda.dados.length === 0 && (
          <EstadoVazio
            ilustracao={<ProfissionalComPrancheta />}
            icone={<CalendarX size={28} aria-hidden="true" />}
            titulo="Nenhuma consulta nos próximos 7 dias"
          />
        )}
        {agenda.dados &&
          porDia(agenda.dados).map(([dia, itens]) => (
            <section key={dia} className={estilos.dia} aria-label={rotuloDoDia(dia)}>
              <h3 className={estilos.tituloDoDia}>{rotuloDoDia(dia)}</h3>
              <ul className={estilos.lista}>
                {itens.map(({ consulta, nomePaciente }) => (
                  <li key={consulta.id} className={estilos.item}>
                    <span className={estilos.hora}>{horaDe(consulta.inicio)}</span>
                    <div className={estilos.dados}>
                      <strong>{nomePaciente ?? "Paciente"}</strong>
                      <span className={estilos.meta}>{NATUREZAS[consulta.natureza]}</span>
                    </div>
                    <Etiqueta tom={STATUS_DA_CONSULTA[consulta.status].tom}>{STATUS_DA_CONSULTA[consulta.status].texto}</Etiqueta>
                    {consulta.triagemId && (
                      <BotaoLink
                        href={linkDoAtendimento(consulta.triagemId, consulta.id)}
                        tamanho="pequeno"
                        variante="contorno"
                        iconeDireita={<ArrowRight size={16} aria-hidden="true" />}
                      >
                        Abrir
                      </BotaoLink>
                    )}
                  </li>
                ))}
              </ul>
            </section>
          ))}
      </Secao>

      <Secao titulo="Encaminhamentos em acompanhamento">
        {recebidos.erro ? <Alerta tom="perigo">{mensagemDeErro(recebidos.erro)}</Alerta> : null}
        {!recebidos.dados && !recebidos.erro && <Carregando linhas={2} />}
        {recebidos.dados && emAcompanhamento.length === 0 && (
          <EstadoVazio icone={<Inbox size={28} aria-hidden="true" />} titulo="Nenhum encaminhamento aberto" />
        )}
        <ul className={estilos.lista}>
          {emAcompanhamento.map((item) => (
            <li key={item.encaminhamento.id} className={estilos.item}>
              <DadosDoEncaminhamento item={item} />
              <BotaoLink
                href={linkDoAtendimento(item.encaminhamento.triagemId)}
                tamanho="pequeno"
                variante="contorno"
                iconeDireita={<ArrowRight size={16} aria-hidden="true" />}
              >
                Abrir
              </BotaoLink>
            </li>
          ))}
        </ul>
      </Secao>
    </div>
  );
}

function DadosDoEncaminhamento({ item }: { item: EncaminhamentoRecebido }) {
  const e = item.encaminhamento;
  return (
    <div className={estilos.dados}>
      <strong>{item.nomePaciente ?? "Paciente"}</strong>
      <div className={estilos.etiquetas}>
        <Etiqueta tom={PRIORIDADES[e.prioridade].tom}>{PRIORIDADES[e.prioridade].texto}</Etiqueta>
        <Etiqueta tom={STATUS_DO_ENCAMINHAMENTO[e.status].tom}>{STATUS_DO_ENCAMINHAMENTO[e.status].texto}</Etiqueta>
      </div>
      <span className={estilos.meta}>
        {nomeDaEspecialidade(e.especialidade)} · {MODALIDADES[e.modalidade]} · {formatarInstante(e.criadoEm)}
      </span>
      <span className={estilos.justificativa}>{e.justificativa}</span>
    </div>
  );
}
